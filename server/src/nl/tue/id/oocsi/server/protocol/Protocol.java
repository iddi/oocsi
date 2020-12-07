package nl.tue.id.oocsi.server.protocol;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

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

	Server server;

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
				Map<String, Object> map = parseJSONMessage(message);
				if (c != null && c.accept(recipient)) {
					c.send(new Message(sender.getName(), recipient, new Date(), map));
				} else {
					// log if not private message
					if (!Channel.isPrivate(recipient)) {
						OOCSIServer.logEvent(sender.getName(), recipient, "?", map, new Date());
					}
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
						try {
							map = parseJavaMessage(message);
							if (c != null && c.accept(recipient)) {
								c.send(new Message(sender.getName(), recipient, new Date(), map));
							} else {
								// log if not private message
								if (!Channel.isPrivate(recipient)) {
									OOCSIServer.logEvent(sender.getName(), recipient, "?", map, new Date());
								}
							}
						} catch (IOException e) {
							OOCSIServer.log("[MsgParser] I/O problem: " + e.getMessage() + "\n\nSender:\n"
							        + sender.getName() + "\nRecipient:\n" + recipient + "\nData:\n" + message);
						} catch (IllegalArgumentException e) {
							OOCSIServer.log("[MsgParser] Base64 encoder problem: " + e.getMessage());
						} catch (ClassNotFoundException e) {
							OOCSIServer.log("[MsgParser] Unknown class: " + e.getMessage());
						} catch (Exception e) {
							// just in case
							OOCSIServer.log("[MsgParser] Unknown problem: " + e.getMessage());
						}
					}

					// only send if there is useful data
					if (map != null) {
						if (c != null && c.accept(recipient)) {
							c.send(new Message(sender.getName(), recipient, new Date(), map));
						} else {
							// log if not private message
							if (!Channel.isPrivate(recipient)) {
								OOCSIServer.logEvent(sender.getName(), recipient, "?", map, new Date());
							}
						}
					}
				}
			}
		}
		// respond to ping
		else if (inputLine.equals("ping")) {
			server.getChangeListener().refresh(sender, sender);
			return ".";
		}
		// record ping acknowledgement
		else if (inputLine.startsWith(".")) {
			sender.pong();
		}
		// no catch
		else {
			server.getChangeListener().refresh(sender, sender);
		}

		// default response
		return "";
	}

	/**
	 * parse a JSON message
	 * 
	 * @param message
	 * @return
	 */
	public static Map<String, Object> parseJSONMessage(String message) {
		final Map<String, Object> map = new HashMap<String, Object>();

		// try to parse input as JSON
		try {
			JsonElement je = JsonParser.parseString(message);
			if (je.isJsonObject()) {
				JsonObject jo = je.getAsJsonObject();
				for (Map.Entry<String, JsonElement> element : jo.entrySet()) {
					// translate primitives from json to java
					JsonElement value = element.getValue();
					// element is a single primitive
					if (value.isJsonPrimitive()) {
						map.put(element.getKey(), value);
					}
					// element is an array
					else if (value.isJsonArray()) {
						JsonArray asJsonArray = value.getAsJsonArray();

						// array is non-empty and contains primitives
						if (asJsonArray != null && asJsonArray.size() > 0 && asJsonArray.get(0).isJsonPrimitive()) {

							// booleans
							if (asJsonArray.get(0).getAsJsonPrimitive().isBoolean()) {
								boolean[] array = new boolean[asJsonArray.size()];
								for (int i = 0; i < array.length; i++) {
									JsonElement jsonElement = asJsonArray.get(i);
									if (jsonElement.isJsonPrimitive() && jsonElement.getAsJsonPrimitive().isBoolean()) {
										array[i] = jsonElement.getAsBoolean();
									}
								}
								map.put(element.getKey(), array);
							}
							// numbers
							else if (asJsonArray.get(0).getAsJsonPrimitive().isNumber()) {
								float[] array = new float[asJsonArray.size()];
								for (int i = 0; i < array.length; i++) {
									JsonElement jsonElement = asJsonArray.get(i);
									if (jsonElement.isJsonPrimitive() && jsonElement.getAsJsonPrimitive().isNumber()) {
										array[i] = jsonElement.getAsFloat();
									}
								}
								map.put(element.getKey(), array);
							}
							// string
							else if (asJsonArray.get(0).getAsJsonPrimitive().isString()) {
								String[] array = new String[asJsonArray.size()];
								for (int i = 0; i < array.length; i++) {
									JsonElement jsonElement = asJsonArray.get(i);
									if (jsonElement.isJsonPrimitive() && jsonElement.getAsJsonPrimitive().isString()) {
										array[i] = jsonElement.getAsString();
									}
								}
								map.put(element.getKey(), array);
							}
						}
						// no primitives included on first level, so we include the JSON array directly
						else if (!value.isJsonNull()) {
							map.put(element.getKey(), value/* serializer.toJson(value) */);
						}
					}
				}
			}
		} catch (JsonSyntaxException jse) {
			// in case of problems, add the full data as a raw string
			map.put("data", message);
		}
		return map;
	}

	/**
	 * parse a Java message
	 * 
	 * @param data
	 * @return
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	@SuppressWarnings("unchecked")
	private Map<String, Object> parseJavaMessage(String data) throws IOException, ClassNotFoundException {
		// serialized object parsing
		ByteArrayInputStream bais = new ByteArrayInputStream(Base64Coder.decode(data));
		ObjectInputStream ois = new ObjectInputStream(bais);
		Object outputObject = ois.readObject();
		return (Map<String, Object>) outputObject;
	}
}
