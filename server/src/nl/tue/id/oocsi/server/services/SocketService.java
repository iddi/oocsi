package nl.tue.id.oocsi.server.services;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;

import nl.tue.id.oocsi.server.OOCSIServer;
import nl.tue.id.oocsi.server.model.Server;

/**
 * socket service component
 * 
 * @author matsfunk
 * 
 */
public class SocketService extends AbstractService {

	private static final int MULTICAST_PORT = 4448;
	private static final String MULTICAST_GROUP = "224.0.0.144";

	private int port;
	private int maxClients;

	private ServerSocket serverSocket;
	private Thread multicastService;
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
			final String hostname = addr.getHostName();

			OOCSIServer.log("[TCP socket server]: Started TCP service @ local address '" + hostname + "' on port "
					+ port + " for TCP");

			multicastService = new Thread() {

				private DatagramSocket socket;
				private boolean running = true;

				public void run() {

					while (running) {
						try {
							if (socket == null) {
								socket = new DatagramSocket();
							}

							InetAddress group = InetAddress.getByName(MULTICAST_GROUP);
							String dString = "OOCSI@" + hostname + ":" + port;
							byte[] buf = dString.getBytes();

							DatagramPacket packet = new DatagramPacket(buf, buf.length, group, MULTICAST_PORT);
							socket.send(packet);

							try {
								sleep((long) Math.random() * 1000 + 5000);
							} catch (InterruptedException e) {
							}
						} catch (IOException e) {
							e.printStackTrace();
							running = false;
						}
					}

					socket.close();
					multicastService = null;
				};
			};
			multicastService.start();

			while (listening) {

				if (!serverSocket.isBound()) {
					continue;
				}

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
