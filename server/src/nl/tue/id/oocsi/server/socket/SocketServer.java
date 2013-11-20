package nl.tue.id.oocsi.server.socket;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;

import nl.tue.id.oocsi.server.OOCSIServer;
import nl.tue.id.oocsi.server.model.Server;

/**
 * socket implementation of OOCSI server
 * 
 * @author mfunk
 * 
 */
public class SocketServer extends Server {

	/**
	 * maximum of open connections
	 */
	private static final int MAX_CONNECTIONS = 25;

	/**
	 * port to listen for client connections
	 */
	private int port;

	/**
	 * server component for OOCSI implementing the socket protocol
	 * 
	 * @param port
	 */
	public SocketServer(int port) {
		this.port = port;
	}

	/**
	 * initialize the server and listen for client connects
	 * 
	 * @throws IOException
	 */
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
			OOCSIServer.log("Started OOCSI server v" + OOCSIServer.VERSION
					+ " with local address '" + hostname + "' on port " + port);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		// // add default management channel
		// os.getChannel("MANAGEMENT");
		//
		while (listening) {

			if (subChannels.size() < MAX_CONNECTIONS) {
				new SocketClient(protocol, serverSocket.accept()).start();
			} else {
				serverSocket.accept().close();
			}
		}

		serverSocket.close();
	}
}
