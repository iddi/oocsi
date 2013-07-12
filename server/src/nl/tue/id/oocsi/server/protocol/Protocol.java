package nl.tue.id.oocsi.server.protocol;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import nl.tue.id.oocsi.server.OOCSIServer;
import nl.tue.id.oocsi.server.model.Channel;
import nl.tue.id.oocsi.server.model.Client;
import nl.tue.id.oocsi.server.model.Server;
import nl.tue.id.oocsi.server.socket.Base64Coder;

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
	 * add a client to the client list in the server
	 * 
	 * @param client
	 */
	public void register(Client client) {
		server.addClient(client);
	}

	/**
	 * remove a client from the client list in the server
	 * 
	 * @param client
	 */
	public void unregister(Client client) {
		server.removeClient(client);
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
			return server.getChannels();
		}
		// get channel subscribers
		else if (inputLine.startsWith("channels")) {
			String channel = inputLine.split(" ")[1];
			Channel c = server.getChannel(channel);
			if (c != null) {
				return c.getChannels();
			}
		}
		// get clients info
		else if (inputLine.equals("clients")) {
			return server.getClients();
		}
		// client subscribes to channel
		else if (inputLine.startsWith("subscribe")) {

			String channel = inputLine.split(" ")[1];
			Channel c = server.getChannel(channel);
			if (c != null) {
				c.addChannel(sender);
			}
		}
		// client unsubscribes from channel
		else if (inputLine.startsWith("unsubscribe")) {
			OOCSIServer.log("unsubscribe");
			String channel = inputLine.split(" ")[1];
			Channel c = server.getChannel(channel);
			if (c != null) {
				c.removeChannel(sender);
			}
		}
		// create new message
		else if (inputLine.startsWith("sendraw")) {
			OOCSIServer.log("sendraw");
			String[] tokens = inputLine.split(" ", 3);
			if (tokens.length == 3) {
				String recipient = tokens[1];
				String message = tokens[2];

				Channel c = server.getChannel(recipient);
				if (c != null) {
					Map<String, Object> map = new HashMap<String, Object>();
					map.put("data", message);
					c.send(new Message(sender.getName(), recipient, new Date(),
							map));
				}
			}
		}
		// create new message
		else if (inputLine.startsWith("send")) {
			OOCSIServer.log("send");
			String[] tokens = inputLine.split(" ", 3);
			if (tokens.length == 3) {
				String recipient = tokens[1];
				String data = tokens[2];

				try {

					ByteArrayInputStream bais = new ByteArrayInputStream(
							Base64Coder.decode(data));
					ObjectInputStream ois = new ObjectInputStream(bais);
					Object outputObject = ois.readObject();

					String channel = inputLine.split(" ")[1];
					Channel c = server.getChannel(channel);
					if (c != null) {
						Map<String, Object> map = (Map<String, Object>) outputObject;
						c.send(new Message(sender.getName(), recipient,
								new Date(), map));
					}
				} catch (IOException e) {
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}

			}
		}

		// default response
		return "";
	}
}
