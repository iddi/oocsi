package nl.tue.id.oocsi.server.protocol;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import nl.tue.id.oocsi.server.OOCSIServer;
import nl.tue.id.oocsi.server.model.Channel;
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
	public String processInput(Channel sender, String inputLine) {

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
			String channel = inputLine.split(" ")[1];
			server.subscribe(sender, channel);
		}
		// client unsubscribes from channel
		else if (inputLine.startsWith("unsubscribe") && inputLine.contains(" ")) {
			String channel = inputLine.split(" ")[1];
			server.unsubscribe(sender, channel);
		}
		// create new message
		else if (inputLine.startsWith("sendraw")) {
			String[] tokens = inputLine.split(" ", 3);
			if (tokens.length == 3) {
				String recipient = tokens[1];
				String message = tokens[2];

				Channel c = server.getChannel(recipient);
				if (c != null) {
					Map<String, Object> map = new HashMap<String, Object>();
					map.put("data", message);
					c.send(new Message(sender.getName(), recipient, new Date(), map));
				}
			}
		}
		// create new message
		else if (inputLine.startsWith("send")) {
			String[] tokens = inputLine.split(" ", 3);
			if (tokens.length == 3) {
				String recipient = tokens[1];
				String data = tokens[2];

				try {
					// serialized object parsing
					ByteArrayInputStream bais = new ByteArrayInputStream(Base64Coder.decode(data));
					ObjectInputStream ois = new ObjectInputStream(bais);
					Object outputObject = ois.readObject();

					Channel c = server.getChannel(recipient);
					if (c != null) {
						@SuppressWarnings("unchecked")
						Map<String, Object> map = (Map<String, Object>) outputObject;
						c.send(new Message(sender.getName(), recipient, new Date(), map));
					}
				} catch (IOException e) {
					OOCSIServer.log("[Parser] IO problem: " + e.getMessage());
				} catch (ClassNotFoundException e) {
					OOCSIServer.log("[Parser] Unknown class: " + e.getMessage());
				}
			}
		}

		// default response
		return "";
	}
}
