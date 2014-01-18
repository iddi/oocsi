package nl.tue.id.oocsi.server.socket;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;

import nl.tue.id.oocsi.server.OOCSIServer;
import nl.tue.id.oocsi.server.discovery.OOCSIServiceProvider;
import nl.tue.id.oocsi.server.model.Server;
import nl.tue.id.oocsi.server.discovery.ServiceConstants;

/**
 * socket implementation of OOCSI server
 * 
 * @author mfunk
 * 
 */
public class SocketServer extends Server {
	
	private String serverName = ServiceConstants.SERVICE_NAME;

	/**
	 * port to listen for client connections
	 */
	private int port;

	/**
	 * maximum of open connections
	 */
	private int maxClients;

	/**
	 * server component for OOCSI implementing the socket protocol
	 * 
	 * @param port
	 */
	public SocketServer(int port, int maxClients) {
		this.port = port;
		this.maxClients = maxClients;
	}
	
	public SocketServer(String serverName, int port, int maxClients) {
		this.serverName = serverName;
		this.port = port;
		this.maxClients = maxClients;
	}
	
	/**
	 * initialize the server and listen for client connects
	 * 
	 * @throws IOException
	 */
	public void init() throws IOException {
		ServerSocket serverSocket = null;
		boolean listening = true;
		OOCSIServiceProvider provider = new OOCSIServiceProvider();

		try {
			/*
			// configure server socket
			serverSocket = new ServerSocket();
			serverSocket.setPerformancePreferences(0, 1, 0);
			serverSocket.setReuseAddress(true);

			// bind to localhost at port <port>
			SocketAddress sockaddr = new InetSocketAddress(port);
			serverSocket.bind(sockaddr);
			*/
			serverSocket = provider.getServerSocket(serverName, port);
		} catch (IOException e) {
			OOCSIServer.log("Could not listen on port: " + port);
			System.exit(-1);
		}

		InetAddress addr;
		try {
			addr = InetAddress.getLocalHost();
			String hostname = addr.getHostName();
			OOCSIServer.log("Started OOCSI server v" + OOCSIServer.VERSION
					+ " (max. " + maxClients + " parallel clients)"
					+ " @ local address '" + hostname + "' on port " + port);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		// // add default management channel
		// os.getChannel("MANAGEMENT");
		//
		while (listening) {

			if (subChannels.size() < maxClients) {
				new SocketClient(protocol, serverSocket.accept()).start();
			} else {
				serverSocket.accept().close();
			}
		}

		serverSocket.close();
	}
}
