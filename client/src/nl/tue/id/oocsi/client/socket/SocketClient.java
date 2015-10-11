package nl.tue.id.oocsi.client.socket;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import nl.tue.id.oocsi.client.protocol.Handler;

/**
 * OOCSI client interface for socket connections
 * 
 * @author matsfunk
 */
public class SocketClient {

	private static final int MULTICAST_PORT = 4448;
	private static final String MULTICAST_GROUP = "224.0.0.144";

	private String name;
	private Map<String, Handler> channels;
	private Queue<String> tempIncomingMessages = new LinkedBlockingQueue<String>(1);

	private Socket socket;
	private BufferedReader input;
	private PrintWriter output;
	private boolean disconnected = false;
	private boolean hasPrintedServerInfo = false;
	private boolean connectionEstablished = false;
	private boolean reconnect = false;
	private int reconnectCountDown = 100;

	/**
	 * create a new socket client with the given name
	 * 
	 * @param name
	 * @param channels
	 */
	public SocketClient(String name, Map<String, Handler> channels) {
		this.name = name;
		this.channels = channels;
	}

	public boolean startMulticastLookup() {

		MulticastSocket socket = null;
		try {
			socket = new MulticastSocket(MULTICAST_PORT);
			InetAddress group = InetAddress.getByName(MULTICAST_GROUP);
			socket.joinGroup(group);

			DatagramPacket packet;
			// check for multi-cast message from server for 10 * 5 seconds
			for (int i = 0; i < 10; i++) {
				byte[] buf = new byte[256];
				packet = new DatagramPacket(buf, buf.length);
				socket.receive(packet);

				// pack String and unpack hostname of server from String
				String received = new String(packet.getData(), 0, packet.getLength());
				if (received.startsWith("OOCSI@")) {
					received = received.replace("OOCSI@", "");
					received = received.replace("\\(.*\\)", "");
					String[] parts = received.split(":");
					if (parts.length == 2 && parts[0].length() > 0 && parts[1].length() > 0) {
						// try to connect
						int port = Integer.parseInt(parts[1]);
						if (connect(parts[0], port)) {
							socket.leaveGroup(group);
							return true;
						}
					}
				}

				try {
					// wait a bit
					Thread.sleep(5000);
				} catch (InterruptedException e) {
				}
			}

			// nothing found for 10 * 5 seconds
			socket.leaveGroup(group);
			return false;
		} catch (IOException ioe) {
			// problem occurred with connection
			return false;
		} finally {
			if (socket != null) {
				socket.close();
			}
		}
	}

	/**
	 * connect to OOCSI at address hostname:port
	 * 
	 * @param hostname
	 * @param port
	 * @return
	 */
	public boolean connect(final String hostname, final int port) {
		boolean result = false;
		while (!disconnected && (!reconnect || reconnectCountDown-- > 0)) {
			result = connectAttempt(hostname, port);

			if (result) {
				reconnectCountDown = 100;
				break;
			} else {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
			}
		}

		return result;
	}

	/**
	 * internal connect to OOCSI at address hostname:port
	 * 
	 * @param hostname
	 * @param port
	 * @return
	 */
	private boolean connectAttempt(final String hostname, final int port) {
		try {
			// configure socket
			socket = new Socket();
			socket.setTcpNoDelay(true);
			socket.setTrafficClass(0x10);
			socket.setPerformancePreferences(0, 1, 0);

			// connect
			SocketAddress sockaddr = new InetSocketAddress(hostname, port);
			socket.connect(sockaddr);

			output = new PrintWriter(socket.getOutputStream(), true);

			// send name
			output.println(name);

			// acquire input channel from server
			input = new BufferedReader(new InputStreamReader(socket.getInputStream()));

			// check if we are ok to connect
			String serverWelcomeMessage;
			if (!socket.isClosed() && (serverWelcomeMessage = input.readLine()) != null) {
				if (!serverWelcomeMessage.startsWith("welcome")) {
					disconnect();
					log(" - disconnected (client name not accepted)");
					return false;
				} else {
					log(" - connected successfully");
				}

				// first data has arrived = connection is ok
				connectionEstablished = true;

				// if ok, run the communication in a different thread
				new Thread(new Runnable() {
					public void run() {
						try {
							String fromServer;
							while (!socket.isClosed() && (fromServer = input.readLine()) != null) {
								if (fromServer.startsWith("send")) {
									// parse server output
									String[] tokens = fromServer.split(" ");
									if (tokens.length == 5) {
										String channel = tokens[1];
										String data = tokens[2];
										String timestamp = tokens[3];
										String sender = tokens[4];

										Handler c = channels.get(channel);
										if (c != null) {
											c.send(sender, data, timestamp, channel, name);
										} else if (channel.equals(name)) {
											c = channels.get("SELF");
											if (c != null) {
												c.send(sender, data, timestamp, channel, name);
											}
										}
									}
								} else {
									tempIncomingMessages.offer(fromServer);
								}

								output.println(".");
							}

						} catch (IOException e) {
							// e.printStackTrace();
						} finally {
							output.close();
							try {
								input.close();
								socket.close();
							} catch (IOException e) {
								// e.printStackTrace();
							}

							log(" - OOCSI disconnected "
									+ (!connectionEstablished ? "(client name not accepted)" : "(server unavailable)"));

							// try reconnect
							connect(hostname, port);
						}
					}
				}).start();
			}
		} catch (UnknownHostException e) {
			printServerInfo();
			log(" - OOCSI failed to connect (unknown host)");
			return false;
		} catch (ConnectException e) {
			printServerInfo();
			log(" - OOCSI failed to connect (connection refused)");
			return false;
		} catch (IOException e) {
			log(" - OOCSI connection error");
			return false;
		}

		reconnectCountDown = 0;
		return true;
	}

	/**
	 * print server hint once
	 */
	private void printServerInfo() {
		if (!hasPrintedServerInfo) {
			log(" --------------------------------------------------");
			log("   Problem finding an OOCSI server!");
			log("   Make sure there is an OOCSI server running");
			log("   at the IP address specified or on the local");
			log("   network when using the autoconf option.");
			log("   For more information how to run an OOCSI server");
			log("   refer to: https://iddi.github.io/oocsi/");
			log(" --------------------------------------------------");
			hasPrintedServerInfo = true;
		}
	}

	/**
	 * check if still connected to OOCSI
	 * 
	 * @return
	 */
	public boolean isConnected() {
		return socket != null && !socket.isClosed() && socket.isConnected();
	}

	/**
	 * disconnect from OOCSI
	 * 
	 */
	public void disconnect() {
		// disconnect from server with handshake
		disconnected = true;
		reconnect = false;
		reconnectCountDown = 0;

		try {
			output.println("quit");
			output.close();
			input.close();
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
	}

	/**
	 * kills this client connection from OOCSI
	 * 
	 */
	public void kill() {
		// disconnect from server without handshake
		disconnected = true;
		reconnect = false;
		reconnectCountDown = 0;

		try {
			output.close();
			input.close();
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
			e.printStackTrace();
		}

		log(" - disconnected (by kill)");
	}

	/**
	 * set whether or not a reconnection attempt should be made if a connection fails
	 * 
	 * @param reconnect
	 */
	public void setReconnect(boolean reconnect) {
		this.reconnect = reconnect;
	}

	/**
	 * subscribe to channel given by channelName
	 * 
	 * @param channelName
	 * @param handler
	 */
	public void subscribe(String channelName, Handler handler) {

		// register at server
		send("subscribe " + channelName);

		// add handler
		channels.put(channelName, handler);
	}

	/**
	 * subscribe to channel my own channel
	 * 
	 * @param handler
	 */
	public void subscribe(Handler handler) {

		// register at server
		send("subscribe " + name);

		// add handler
		channels.put("SELF", handler);
	}

	/**
	 * unsubscribe from channel given by channelName
	 * 
	 * @param channelName
	 */
	public void unsubscribe(String channelName) {

		// unregister at server
		send("unsubscribe " + channelName);

		// remove handler
		channels.remove(channelName);
	}

	/**
	 * unsubscribe from my channel
	 * 
	 */
	public void unsubscribe() {

		// unregister at server
		send("unsubscribe " + name);

		// remove handler
		channels.remove("SELF");
	}

	/**
	 * send raw message (no serialization)
	 * 
	 * @param channelName
	 * @param message
	 */
	public void send(String channelName, String message) {
		// send message
		send("sendraw " + channelName + " " + message);
	}

	/**
	 * send message with data payload (map of key value pairs which will be serialized before sending)
	 * 
	 * @param channelName
	 * @param data
	 */
	public void send(String channelName, Map<String, Object> data) {
		// send message with raw data
		send("send " + channelName + " " + serialize(data));
	}

	/**
	 * retrieve the current channels on server
	 * 
	 * @return
	 */
	public String clients() {
		synchronized (tempIncomingMessages) {
			tempIncomingMessages.clear();
			send("clients");
			return syncPoll();
		}
	}

	/**
	 * retrieve the current channels on server
	 * 
	 * @return
	 */
	public String channels() {
		synchronized (tempIncomingMessages) {
			tempIncomingMessages.clear();
			send("channels");
			return syncPoll();
		}
	}

	/**
	 * retrieve the current sub-channels of the given channel on server
	 * 
	 * @param channelName
	 * @return
	 */
	public String channels(String channelName) {
		synchronized (tempIncomingMessages) {
			tempIncomingMessages.clear();
			send("channels " + channelName);
			return syncPoll();
		}
	}

	/**
	 * send the message to the socket
	 * 
	 * @param rawMessage
	 */
	private void send(String rawMessage) {
		if (isConnected() && output != null) {
			output.println(rawMessage);
		}
	}

	/**
	 * poll incoming message in a hard-synchronized way with timeout 1000ms
	 * 
	 * @return
	 */
	private String syncPoll() {
		return syncPoll(1000);
	}

	/**
	 * poll incoming message in a hard-synchronized way with variable timeout in ms
	 * 
	 * @param timeout
	 * @return
	 */
	private String syncPoll(int timeout) {
		long start = System.currentTimeMillis();

		try {
			while (tempIncomingMessages.size() == 0 || start + timeout > System.currentTimeMillis()) {
				Thread.sleep(50);
			}
			return tempIncomingMessages.size() > 0 ? tempIncomingMessages.poll() : null;
		} catch (InterruptedException e) {
		}

		return null;
	}

	/**
	 * serialize a map of key value pairs
	 * 
	 * @param data
	 * @return
	 */
	private String serialize(Map<String, Object> data) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
		try {
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(data);
			byte[] rawData = baos.toByteArray();
			return new String(Base64Coder.encode(rawData));
		} catch (IOException e) {
			return "";
		}
	}

	/**
	 * logging of message on console (can be overridden by subclass)
	 */
	public void log(String message) {
		// no logging by default
	}
}
