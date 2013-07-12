package nl.tue.id.oocsi.server.socket;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;

import nl.tue.id.oocsi.server.OOCSIServer;
import nl.tue.id.oocsi.server.model.Server;

public class SocketServer extends Server {

	private int port;

	public SocketServer(int port) {
		this.port = port;
	}

	public void init() throws IOException {
		ServerSocket serverSocket = null;
		boolean listening = true;

		try {
			serverSocket = new ServerSocket(port);
		} catch (IOException e) {
			OOCSIServer.log("Could not listen on port: " + port);
			System.exit(-1);
		}

		InetAddress addr;
		try {
			addr = InetAddress.getLocalHost();
			String hostname = addr.getHostName();
			OOCSIServer.log("Local IP address is " + hostname);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		// // add default management channel
		// os.getChannel("MANAGEMENT");
		//

		while (listening) {
			new SocketClient(protocol, serverSocket.accept()).start();
		}

		serverSocket.close();
	}
}
