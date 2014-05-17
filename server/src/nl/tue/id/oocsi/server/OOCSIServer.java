package nl.tue.id.oocsi.server;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

import nl.tue.id.oocsi.server.model.Channel;
import nl.tue.id.oocsi.server.protocol.Message;
import nl.tue.id.oocsi.server.socket.SocketServer;

import com.google.gson.Gson;

/**
 * main server component for running OOCSI
 * 
 * @author matsfunk
 * 
 */
public class OOCSIServer {

	public static final String VERSION = "0.8";

	// defaults
	public static int port = 4444;
	public static int maxClients = 30;
	public static boolean isLogging = false;

	// default channels
	public static final String OOCSI_EVENTS = "OOCSI_events";
	public static final String OOCSI_CONNECTIONS = "OOCSI_connections";

	public static SocketServer server;

	public static void main(String[] args) {

		// check dependencies
		new Gson();

		// get port from arguments
		parseCommandlineArgs(args);

		// start socket server
		try {
			server = new SocketServer(port, maxClients);
			server.init();
		} catch (IOException e) {
			// e.printStackTrace();
		} finally {
			// done
		}
	}

	/**
	 * logging of a general server event (can be switched off with startup parameter '-logging')
	 * 
	 * @param message
	 */
	public static void log(String message) {
		if (isLogging) {
			System.out.println(message);
		}
	}

	/**
	 * logging of event (can be switched off with startup parameter '-logging')
	 * 
	 * @param sender
	 * @param recipient
	 * @param data
	 * @param timestamp
	 */
	public static void logEvent(String sender, String recipient, Map<String, Object> data, Date timestamp) {
		if (isLogging && recipient != OOCSIServer.OOCSI_EVENTS && recipient != OOCSIServer.OOCSI_CONNECTIONS) {
			System.out.println(OOCSI_EVENTS + " " + sender + "->" + recipient);

			Message message = new Message(sender, OOCSI_EVENTS, timestamp, data);
			message.addData("sender", sender);
			message.addData("recipient", recipient);
			Channel logChannel = server.getChannel(OOCSI_EVENTS);
			if (logChannel != null) {
				logChannel.send(message);
			}
		}
	}

	/**
	 * logging of connection/channel update (can be switched off with startup parameter '-logging')
	 * 
	 * @param message
	 */
	public static void logConnection(String client, String channel, String operation, Date timestamp) {
		if (isLogging && channel != OOCSIServer.OOCSI_EVENTS && channel != OOCSIServer.OOCSI_CONNECTIONS) {
			System.out.println(OOCSI_CONNECTIONS + " " + client + "->" + channel + " (" + operation + ")");

			Message message = new Message(client, OOCSI_CONNECTIONS, timestamp);
			message.addData("client", client);
			message.addData("channel", channel);
			message.addData("operation", operation);
			Channel logChannel = server.getChannel(OOCSI_CONNECTIONS);
			if (logChannel != null) {
				logChannel.send(message);
			}
		}
	}

	/**
	 * parses the command line arguments
	 */
	public static void parseCommandlineArgs(String[] args) {
		for (int i = 0; i < args.length; i++) {
			String argument = args[i];

			if (argument.equals("-port") && args.length >= i + 2) {
				port = Integer.parseInt(args[i + 1]);
			} else if (argument.equals("-clients") && args.length >= i + 2) {
				maxClients = Integer.parseInt(args[i + 1]);
			} else if (argument.equals("-logging")) {
				isLogging = true;
			}
		}
	}
}
