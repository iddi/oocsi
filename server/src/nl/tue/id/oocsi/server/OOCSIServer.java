package nl.tue.id.oocsi.server;

import java.io.IOException;

import nl.tue.id.oocsi.server.socket.SocketServer;

/**
 * main server component for running OOCSI
 * 
 * @author mfunk
 * 
 */
public class OOCSIServer {

	public static final String VERSION = "0.3";

	// defaults
	public static int port = 4444;
	public static int maxClients = 25;
	public static boolean isLogging = false;

	public static void main(String[] args) {

		// get port from arguments
		parseCommandlineArgs(args);

		// start socket server
		try {
			new SocketServer(port, maxClients).init();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			// done
		}
	}

	/**
	 * logging of message on console (can be switched off)
	 * 
	 * @param message
	 */
	public static void log(String message) {
		if (isLogging) {
			System.out.println(message);
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
