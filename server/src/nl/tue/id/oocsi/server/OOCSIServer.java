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

	private static boolean isLogging = true;

	public static void main(String[] args) {

		// get port from arguments
		String port = "4444";
		OOCSIServer.log("Starting OOCSI server on port " + port);

		// start socket server
		try {
			new SocketServer(4444).init();
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
