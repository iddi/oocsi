package nl.tue.id.oocsi.server.services;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import nl.tue.id.oocsi.server.OOCSIServer;
import nl.tue.id.oocsi.server.model.Client;
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

	private final int port;
	private final String[] users;

	private ServerSocket serverSocket;
	private boolean listening = true;

	/**
	 * create a TCP socket service for OOCSI
	 * 
	 * @param server
	 * @param port
	 */
	public SocketService(Server server, int port, String[] users) {
		super(server);

		this.port = port;
		this.users = users;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.tue.id.oocsi.server.services.AbstractService#register(nl.tue.id.oocsi.server.model.Client)
	 */
	@Override
	public boolean register(Client client) {
		// for private clients, first check whether it needs to comply to existing users
		final String name = client.getName();
		if (users != null) {
			for (String user : users) {
				if (user != null && user.replaceFirst(":.*", "").equals(name)) {
					if (client.validate(user)) {
						return super.register(client);
					} else {
						return false;
					}

				}
			}
		}

		// for non-private clients
		return !client.isPrivate() && super.register(client);
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

			// configure periodic services
			Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(new Runnable() {
				private DatagramSocket socket;

				@Override
				public void run() {

					// multicast beacon
					try {
						if (socket == null) {
							socket = new DatagramSocket();
						}

						InetAddress group = InetAddress.getByName(MULTICAST_GROUP);
						String dString = "OOCSI@" + hostname + ":" + port;
						byte[] buf = dString.getBytes();

						DatagramPacket packet = new DatagramPacket(buf, buf.length, group, MULTICAST_PORT);
						socket.send(packet);
					} catch (Exception e) {
						// e.printStackTrace();
					}
				};
			}, 1000, (long) (5 * 1000 + Math.random() * 1000), TimeUnit.MILLISECONDS);

			// socket service operations
			while (listening) {
				if (!serverSocket.isBound()) {
					Thread.yield();
					continue;
				}

				// first see if there is a new connection coming in
				Socket acceptedSocket = serverSocket.accept();

				// then check if we can accept a new client
				if (server.canAcceptClient(null)) {
					new SocketClient(this, acceptedSocket).start();
				} else {
					acceptedSocket.close();
				}
			}

			serverSocket.close();
		} catch (SocketException e) {
			// only report if still listening
			if (listening) {
				e.printStackTrace();
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// only report if still listening
			if (listening) {
				e.printStackTrace();
			}
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
