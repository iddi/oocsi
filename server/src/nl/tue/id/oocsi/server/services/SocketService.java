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
import java.util.Timer;
import java.util.TimerTask;

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

	private int port;
	private int maxClients;
	private String[] users;

	private ServerSocket serverSocket;
	private Timer periodicMaintenanceService;
	private boolean listening = true;

	/**
	 * create a TCP socket service for OOCSI
	 * 
	 * @param server
	 * @param port
	 * @param maxClients
	 */
	public SocketService(Server server, int port, int maxClients, String[] users) {
		super(server);

		this.port = port;
		this.maxClients = maxClients;
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
		String name = client.getName();
		for (String user : users) {
			if (user != null && user.replaceFirst(":.*", "").equals(name)) {
				if (client.validate(user)) {
					return super.register(client);
				} else {
					return false;
				}

			}
		}
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

			TimerTask task = new TimerTask() {

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
					} catch (IOException e) {
						// e.printStackTrace();
					}

					// keep-alive ping-pong with socket clients
					for (Client client : server.getClients()) {
						long timeout = System.currentTimeMillis() - client.lastAction();
						if (timeout > 120000) {
							OOCSIServer.log("Client " + client.getName()
									+ " has not responded for 120 secs and will be disconnected");
							server.removeClient(client);
							break;
						} else {
							client.ping();
						}
					}
				};
			};
			periodicMaintenanceService = new Timer();
			periodicMaintenanceService.schedule(task, 0, (long) (5 * 1000 + Math.random() * 1000));

			while (listening) {

				if (!serverSocket.isBound()) {
					continue;
				}

				// first see if there is a new connection coming in
				Socket acceptedSocket = serverSocket.accept();

				// then check if we can accept a new client
				if (server.getClients().size() < maxClients) {
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
