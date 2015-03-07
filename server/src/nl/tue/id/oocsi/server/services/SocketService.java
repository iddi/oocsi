package nl.tue.id.oocsi.server.services;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;

import nl.tue.id.oocsi.server.OOCSIServer;
import nl.tue.id.oocsi.server.model.Server;

public class SocketService extends AbstractService {

	private int port;
	private int maxClients;

	private ServerSocket serverSocket;
	private boolean listening = true;

	/**
	 * create a TCP socket service for OOCSI
	 * 
	 * @param server
	 * @param port
	 * @param maxClients
	 */
	public SocketService(Server server, int port, int maxClients) {
		super(server);

		this.port = port;
		this.maxClients = maxClients;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.tue.id.oocsi.server.services.AbstractService#start()
	 */
	public void start() {
		try {
			// configure server socket
			serverSocket = new ServerSocket();
			serverSocket.setPerformancePreferences(0, 1, 0);
			serverSocket.setReuseAddress(true);

			// bind to localhost at port <port>
			SocketAddress sockaddr = new InetSocketAddress(port);
			serverSocket.bind(sockaddr);
		} catch (IOException e) {
			OOCSIServer.log("[TCP socket server]: Could not listen on port: " + port);
		}

		InetAddress addr;
		try {
			addr = InetAddress.getLocalHost();
			String hostname = addr.getHostName();

			OOCSIServer.log(" - started TCP service @ local address '" + hostname + "' on port " + port + "for TCP");

			while (listening) {

				// first see if there is a new connection coming in
				Socket acceptedSocket = serverSocket.accept();

				// then check if we can accept
				if (server.getChannels().size() < maxClients) {
					new SocketClient(this, acceptedSocket).start();
				} else {
					acceptedSocket.close();
				}
			}

			serverSocket.close();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.tue.id.oocsi.server.services.AbstractService#stop()
	 */
	public void stop() {
		// switch off listening loop
		listening = false;

		// close server socket
		try {
			serverSocket.close();
		} catch (IOException e) {
		}
	}
}
