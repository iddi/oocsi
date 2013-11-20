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

	public static final String VERSION = "0.2";

	private static boolean isLogging = true;

	public static void main(String[] args) {

		// get port from arguments
		// TODO

		// for now, set the port fixed
		int port = 4444;

		// start socket server
		try {
			new SocketServer(port).init();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			// done
		}
	}

	public static void log(String message) {
		if (isLogging) {
			System.out.println(message);
		}
	}
}
