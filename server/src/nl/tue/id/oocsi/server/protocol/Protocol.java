package nl.tue.id.oocsi.server.protocol;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import nl.tue.id.oocsi.server.OOCSIServer;
import nl.tue.id.oocsi.server.model.Channel;
import nl.tue.id.oocsi.server.model.Client;
import nl.tue.id.oocsi.server.model.Server;

/**
 * implements the OOCSI communication protocol, registers and unregisters clients, and parses and dispatches input
 * received from clients
 * 
 * @author matsfunk
 * 
 */
public class Protocol {

	private final Server server;

	/**
	 * create new protocol
	 * 
	 * @param server
	 */
	public Protocol(Server server) {
		this.server = server;
	}

	/**
	 * process a line of input send via the socket
	 * 
	 * @param sender
	 * @param inputLine
	 * @return
	 */
	public String processInput(Client sender, String inputLine) {

		if (inputLine == null) {
			return "";
		}

		// close connection request
		if (inputLine.equals("quit")) {
			return null;
		}
		// get channel info
		else if (inputLine.equals("channels")) {
			return server.getChannelList();
		}
		// get channel subscribers
		else if (inputLine.startsWith("channels") && inputLine.contains(" ")) {
			String channel = inputLine.split(" ")[1];
			Channel c = server.getChannel(channel);
			if (c != null) {
				return c.getChannelList();
			}
		}
		// get clients info
		else if (inputLine.equals("clients")) {
			return server.getClientList();
		}
		// client subscribes to channel
		else if (inputLine.startsWith("subscribe") && inputLine.contains(" ")) {
			String channel = inputLine.split(" ", 2)[1];
			server.subscribe(sender, channel);
		}
		// client unsubscribes from channel
		else if (inputLine.startsWith("unsubscribe") && inputLine.contains(" ")) {
			String channel = inputLine.split(" ", 2)[1];
			server.unsubscribe(sender, channel);
		}
		// create new message from raw text input
		else if (inputLine.startsWith("sendraw")) {
			String[] tokens = inputLine.split(" ", 3);
			if (tokens.length == 3) {
				String recipient = tokens[1];
				String message = tokens[2];

				Channel c = server.getChannel(recipient);
				if (message.startsWith("{")) {
					dispatchMessage(sender, recipient, c, parseJSONMessage(message));
				} else {
					final Map<String, Object> map = new ConcurrentHashMap<String, Object>();
					map.put("data", message);
					dispatchMessage(sender, recipient, c, map);
				}
			}
		}
		// create new message from Java serialized input
		else if (inputLine.startsWith("send")) {
			String[] tokens = inputLine.split(" ", 3);
			if (tokens.length == 3) {
				String recipient = tokens[1];
				String message = tokens[2];

				Channel c = server.getChannel(recipient);
				if (message.length() > 0) {
					Map<String, Object> map = null;
					if (message.startsWith("{")) {
						map = parseJSONMessage(message);
					} else {
						// log legacy messages
						OOCSIServer.log("[MsgParser] Received legacy Java message from " + sender.getName()
						        + "\nRecipient:\n" + recipient + "\n");

						// no function message
						map = new ConcurrentHashMap<>();
					}

					// only send if there is useful data
					if (map != null) {
						dispatchMessage(sender, recipient, c, map);
					}
				}
			}
		}
		// respond to ping
		else if (inputLine.equals("ping")) {
			server.refreshChannelPresence(sender);
			sender.pong();
			return ".";
		}
		// record ping acknowledgement
		else if (inputLine.startsWith(".")) {
			server.refreshChannelPresence(sender);
			sender.pong();
		}
		// no catch
		else {
			server.refreshChannelPresence(sender);
		}

		// default response
		return "";
	}

	/**
	 * dispatch a message in the system; check for delayed messages and log messages that are sent to empty channels
	 * 
	 * @param sender
	 * @param recipient
	 * @param c         outgoing channel
	 * @param map       message data as map
	 */
	private void dispatchMessage(Client sender, String recipient, Channel c, Map<String, Object> map) {
		// check for delayed message by requesting the _DELAY attribute that provides the requested delay in seconds
		if (map.containsKey(Message.DELAY_MESSAGE)) {
			long delayTime = 0;
			// check for valid delay time
			Object delay = map.get(Message.DELAY_MESSAGE);
			// extract delay time from Number
			if (delay instanceof Number) {
				Number delayMS = (Number) delay;
				delayTime = delayMS.longValue();
			}
			// extract delay time from String
			else if (delay instanceof String) {
				String delayMS = (String) delay;
				try {
					delayTime = Long.parseLong(delayMS);
				} catch (Exception e) {
					delayTime = 0;
				}
			}

			// send with specified delay in seconds
			if (delayTime > 0) {
				server.sendDelayedMessage(recipient, new Message(sender.getName(), recipient,
				        new Date(System.currentTimeMillis() + delayTime * 1000), map));
			}
			// normal dispatch for broken _DELAY
			else if (c != null && c.accept(recipient)) {
				c.send(new Message(sender.getName(), recipient, new Date(), map));
			}
		}
		// no delay --> normal dispatch
		else if (c != null && c.accept(recipient)) {
			c.send(new Message(sender.getName(), recipient, new Date(), map));
		}
		// only log if no messages will go out to the channel
		else {
			// log if not private message
			if (!Channel.isPrivate(recipient)) {
				OOCSIServer.logEvent(sender.getName(), recipient, "-", map, new Date());
			}
		}
	}

	/**
	 * parse a JSON message --> convert JsonNode (ObjectNode) to Map<String, Object> that can later be serialized as
	 * Json again
	 * 
	 * @param message
	 * @return
	 */
	public static Map<String, Object> parseJSONMessage(String message) {
		Map<String, Object> map = new HashMap<String, Object>();
		try {
			ObjectMapper om = new ObjectMapper();
			JsonNode jn = om.readTree(message);
			if (jn.isObject()) {
				ObjectNode on = (ObjectNode) jn;
				for (Iterator<Entry<String, JsonNode>> iterator = on.fields(); iterator.hasNext();) {
					Entry<String, JsonNode> entry = iterator.next();
					JsonNode val = entry.getValue();
					if (val.isBoolean()) {
						map.put(entry.getKey(), val.booleanValue());
					} else if (val.isInt()) {
						map.put(entry.getKey(), val.intValue());
					} else if (val.isFloat()) {
						map.put(entry.getKey(), val.floatValue());
					} else if (val.isDouble()) {
						map.put(entry.getKey(), val.doubleValue());
					} else if (val.isLong()) {
						map.put(entry.getKey(), val.longValue());
					} else if (val.isTextual()) {
						map.put(entry.getKey(), val.textValue());
					} else if (val.isObject()) {
						ObjectNode object = (ObjectNode) val;
						map.put(entry.getKey(), object);
					} else if (val.isArray()) {
						ArrayNode array = (ArrayNode) val;
						map.put(entry.getKey(), array);
					}
				}
			}
		} catch (JsonMappingException e) {
		} catch (JsonProcessingException e) {
		}

		return map;
	}
}
