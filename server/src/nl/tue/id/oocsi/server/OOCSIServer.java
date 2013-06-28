package nl.tue.id.oocsi.server;

import java.io.IOException;

import nl.tue.id.oocsi.server.socket.SocketServer;

public class OOCSIServer {

	public static void main(String[] args) {

		// get port from arguments
		String port = "4444";
		System.out.println("Starting OOCSI server on port " + port);

		// start socket server
		try {
			new SocketServer(4444).init();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			// done
		}
	}
}
