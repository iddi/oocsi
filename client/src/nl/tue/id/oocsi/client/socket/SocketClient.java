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
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import nl.tue.id.oocsi.client.protocol.Handler;
import nl.tue.id.oocsi.client.protocol.MultiHandler;
import nl.tue.id.oocsi.client.services.OOCSICall;
import nl.tue.id.oocsi.client.services.Responder;

/**
 * OOCSI client interface for socket connections
 * 
 * @author matsfunk
 */
public class SocketClient {

	private static final String SELF = "SELF";
	private static final int MULTICAST_PORT = 4448;
	private static final String MULTICAST_GROUP = "224.0.0.144";

	private String hostname;
	private int port;
	private String name;

	private Map<String, Handler> channels;
	private Map<String, Responder> services;
	private Queue<String> tempIncomingMessages = new LinkedBlockingQueue<String>(1);
	private List<OOCSICall> openCalls = new LinkedList<OOCSICall>();

	// i/o
	private Socket socket;
	private BufferedReader input;
	private PrintWriter output;

	// connection flags
	private boolean disconnected = false;
	private boolean hasPrintedServerInfo = false;
	private boolean connectionEstablished = false;
	private boolean reconnect = false;
	private int reconnectCountDown = 100;
	private boolean relinquished = false;

	// thread pool
	private ExecutorService executor;

	/**
	 * create a new socket client with the given name
	 * 
	 * @param name
	 * @param channels
	 */
	public SocketClient(String name, Map<String, Handler> channels, Map<String, Responder> services) {
		this.name = name;
		this.channels = channels;
		this.services = services;

		this.executor = Executors.newCachedThreadPool();
	}

	/**
	 * start pinging for a multi-cast lookup
	 * 
	 * @return
	 */
	public boolean startMulticastLookup() {
		try (MulticastSocket socket = new MulticastSocket(MULTICAST_PORT)) {
			socket.setSoTimeout(10000);
			InetAddress group = InetAddress.getByName(MULTICAST_GROUP);
			socket.joinGroup(group);

			// check for multi-cast message from server for 5 * 1 second
			for (int i = 0; !isConnected() && i < 5; i++) {

				// connect to multi-cast server host name
				connectFromMulticast(socket);

				// no proper signal
				sleep(1000);
			}

			// nothing found for 10 * 5 seconds
			socket.leaveGroup(group);
			return isConnected();
		} catch (IOException ioe) {
			// problem occurred with connection
			return false;
		}
	}

	/**
	 * connection to a multi-cast server
	 * 
	 * @param socket
	 * @throws IOException
	 */
	private void connectFromMulticast(MulticastSocket socket) throws IOException {
		try {
			final byte[] buf = new byte[256];
			final DatagramPacket packet = new DatagramPacket(buf, buf.length);
			socket.receive(packet);

			// pack String and unpack host name of server from String
			String received = new String(packet.getData(), 0, packet.getLength());
			if (received.startsWith("OOCSI@")) {
				String[] parts = received.replace("OOCSI@", "").replace("\\(.*\\)", "").split(":");
				if (parts.length == 2 && parts[0].length() > 0 && parts[1].length() > 0) {
					// try to connect with given parts as server address
					connect(parts[0], Integer.parseInt(parts[1]));
				}
			}
		} catch (NumberFormatException nfe) {
			// do nothing
		} catch (SocketTimeoutException e) {
			// likely timeout occurred
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

		// store the connection details
		this.hostname = hostname;
		this.port = port;

		// start connection thread
		executor.submit(new SocketClientRunnable());

		// check back on connection progress
		while (!disconnected && !connectionEstablished && reconnectCountDown > 0) {
			sleep(100);
		}

		// return connection status
		return connectionEstablished;
	}

	/**
	 * internal connect to OOCSI at address hostname:port
	 * 
	 * @param hostname
	 * @param port
	 * @return
	 * @throws OOCSIAuthenticationException
	 */
	private boolean connectAttempt(final String hostname, final int port) throws OOCSIAuthenticationException {
		try {
			// configure and connect socket
			connectSocket(hostname, port);

			output = new PrintWriter(socket.getOutputStream(), true);

			// send name
			output.println(name);

			// acquire input channel from server
			input = new BufferedReader(new InputStreamReader(socket.getInputStream()));

			// do the handshake
			return connectionHandshake(hostname, port);

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

	}

	/**
	 * connection handshake after the socket connection has been established
	 * 
	 * @param hostname
	 * @param port
	 * @return
	 * @throws IOException
	 * @throws OOCSIAuthenticationException
	 */
	private boolean connectionHandshake(final String hostname, final int port)
			throws IOException, OOCSIAuthenticationException {
		// short timeout when connecting
		socket.setSoTimeout(5000);

		// check if we are ok to connect
		String serverWelcomeMessage;
		if (!socket.isClosed() && (serverWelcomeMessage = input.readLine()) != null) {
			// name is not ok
			if (!serverWelcomeMessage.startsWith("welcome")) {
				disconnect();
				log(" - disconnected (client name not accepted)");
				throw new OOCSIAuthenticationException();
			}

			// name is ok
			log(" - connected successfully");

			// longer timeout after successful connection
			socket.setSoTimeout(20000);

			// first data has arrived = connection is ok
			connectionEstablished = true;

			// subscribe to all open channels
			for (String channelName : channels.keySet()) {
				this.internalSubscribe(channelName);
			}

			reconnectCountDown = 0;
			return true;
		}

		reconnectCountDown = 0;
		return false;
	}

	/**
	 * configure and connect a socket
	 * 
	 * @param hostname
	 * @param port
	 * @throws SocketException
	 * @throws IOException
	 */
	private void connectSocket(final String hostname, final int port) throws SocketException, IOException {

		// open and configure socket
		socket = new Socket();
		socket.setTcpNoDelay(true);
		socket.setTrafficClass(0x10);
		socket.setPerformancePreferences(0, 1, 0);

		// connect
		SocketAddress sockaddr = new InetSocketAddress(hostname, port);
		socket.connect(sockaddr);
	}

	/**
	 * print server hint once
	 * 
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
	 * return client name
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * disconnect from OOCSI
	 * 
	 */
	public void disconnect() {
		// disconnect from server with handshake
		disconnected = true;

		// and no reconnect
		reconnect = false;
		reconnectCountDown = 0;
		relinquished = true;

		output.println("quit");
		internalDisconnect();
		shutDown();
	}

	/**
	 * kills this client connection from OOCSI
	 * 
	 */
	public void kill() {
		// disconnect from server without handshake, allows for reconnection testing
		disconnected = true;

		internalDisconnect();
		log(" - disconnected (by kill)");
		shutDown();
	}

	/**
	 * reconnects this client connection to OOCSI
	 * 
	 */
	public void reconnect() {
		output.println("quit");
		internalDisconnect();
		log(" - disconnected (by reconnect)");
	}

	/**
	 * close all i/o resources safely
	 * 
	 */
	private void internalDisconnect() {

		// shutdown I/O
		try {
			if (output != null) {
				output.close();
			}
			if (input != null) {
				input.close();
			}
			if (socket != null) {
				socket.close();
			}
		} catch (IOException e) {
			// do nothing
		} catch (NullPointerException e) {
			// do nothing
		}
	}

	/**
	 * shutdown executor service
	 * 
	 */
	private void shutDown() {
		try {
			executor.shutdown();
			executor.awaitTermination(200, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
		} finally {
			if (!executor.isTerminated()) {
			}
			executor.shutdownNow();
		}
	}

	/**
	 * retrieve whether we are still trying to reconnect, or whether we have given up on this connection (server,
	 * handle, etc.)
	 * 
	 * @return
	 */
	public boolean isReconnect() {
		return this.reconnect && !this.relinquished;
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

		// subscribe to channel if not done yet
		if (!internalIsSubscribed(channelName)) {
			internalSubscribe(channelName);
		}

		// add handler to internal multi-handler
		internalAddHandler(channelName, handler);
	}

	/**
	 * @param channelName
	 */
	private void internalSubscribe(String channelName) {
		// register at server
		send("subscribe " + channelName);

		// check for replacement
		if (channels.get(channelName) != null) {
			log(" - reconnected subscription for " + channelName);
		}
	}

	/**
	 * manage internal multi-handler for this channel: will add the given handler to an existing multi-handler's
	 * internal list, or create a new multi-handler with the given handler as the first sub-handler
	 * 
	 * @param channelName
	 * @param handler
	 */
	private void internalAddHandler(String channelName, Handler handler) {
		if (channels.containsKey(channelName)) {
			Handler h = channels.get(channelName);
			if (h instanceof MultiHandler) {
				MultiHandler mh = (MultiHandler) h;
				mh.add(handler);
			}
		} else {
			channels.put(channelName, new MultiHandler(handler));
		}
	}

	/**
	 * returns whether this client has already subscribed to the given channel
	 * 
	 * @param channelName
	 * @return
	 */
	private boolean internalIsSubscribed(String channelName) {
		return channels.containsKey(channelName);
	}

	/**
	 * subscribe to channel my own channel
	 * 
	 * @param handler
	 */
	public void subscribe(Handler handler) {

		// register at server
		send("subscribe " + name);

		// check for replacement
		if (channels.get(SELF) != null) {
			log(" - reconnected subscription for " + name);
		}

		// add handler
		channels.put(SELF, handler);
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
		internalRemoveHandler(channelName, null);
	}

	/**
	 * unsubscribe from my channel
	 * 
	 */
	public void unsubscribe() {

		// unregister at server
		send("unsubscribe " + name);

		// remove handler
		internalRemoveHandler(SELF, null);
	}

	/**
	 * manage internal multi-handler for this channel: will remove the given handler from an existing multi-handler's
	 * internal list, or just remove the channel directly
	 * 
	 * @param channelName
	 * @param handler
	 */
	private void internalRemoveHandler(String channelName, Handler handler) {
		if (channels.containsKey(channelName) && handler != null) {
			Handler h = channels.get(channelName);
			if (h instanceof MultiHandler) {
				MultiHandler mh = (MultiHandler) h;
				mh.remove(handler);

				if (mh.isEmpty()) {
					channels.remove(channelName);
				}
			}
		} else {
			channels.remove(channelName);
		}
	}

	/**
	 * register a call in the list of open calls
	 * 
	 * @param call
	 */
	public void register(OOCSICall call) {
		openCalls.add(call);
	}

	/**
	 * register a responder with a handle "callName"
	 * 
	 * @param callName
	 * @param responder
	 */
	public void register(String callName, Responder responder) {
		services.put(callName, responder);
	}

	/**
	 * unregister a responder with a handle "callName"
	 * 
	 * @param callName
	 */
	public void unregister(String callName) {
		services.remove(callName);
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
		tempIncomingMessages.clear();
		send("clients");
		return syncPoll();
	}

	/**
	 * retrieve the current channels on server
	 * 
	 * @return
	 */
	public String channels() {
		tempIncomingMessages.clear();
		send("channels");
		return syncPoll();
	}

	/**
	 * retrieve the current sub-channels of the given channel on server
	 * 
	 * @param channelName
	 * @return
	 */
	public String channels(String channelName) {
		tempIncomingMessages.clear();
		send("channels " + channelName);
		return syncPoll();
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
				Thread.yield();
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
	 * let thread sleep for ms milliseconds
	 * 
	 */
	private void sleep(int ms) {
		try {
			// so, wait a bit before next trial
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			// do nothing
		}
	}

	/**
	 * logging of message on console (can be overridden by subclass)
	 */
	public void log(String message) {
		// no logging by default
	}

	/**
	 * communication handler (to be run in a separate thread)
	 * 
	 */
	class SocketClientRunnable implements Runnable {

		public SocketClientRunnable() {
			// TODO Auto-generated constructor stub
		}

		@Override
		public void run() {
			try {
				int connections = 0;

				// SESSIONS
				// only do if we either always reconnect, or it's the first connection
				while (reconnect || connections == 0) {
					connections++;

					// ATTEMPTS
					// try 100 times per (re-)connection attempt
					reconnectCountDown = 100;
					while (reconnectCountDown-- > 0) {
						// try to connect
						if (connectAttempt(hostname, port)) {
							// connection is fine, stop trials
							break;
						}

						// another trial, sleep before
						sleep(1000);
					}

					if (isConnected()) {
						// connection rest forever
						runCommunication();
					} else {
						// take a rest before trying again
						sleep(5000);
					}
				}
			} catch (OOCSIAuthenticationException oae) {
				disconnect();
				// quit this thread...
			} finally {
				relinquished = true;
			}
		}

		/**
		 * all communications after the initial handshake happen here
		 * 
		 */
		private void runCommunication() {
			try {
				String fromServer;
				int cyclesSinceRead = 0;
				while (!socket.isClosed()) {

					// main messaging loop
					while (input.ready() && (fromServer = input.readLine()) != null) {
						handleMessage(fromServer);
						cyclesSinceRead = 0;
					}

					// sleep if there is nothing to read
					sleep(1);

					// if no data came in for 20 secs, kill connection and reconnect
					if (cyclesSinceRead++ > 20000) {
						internalDisconnect();
						log(" - OOCSI disconnected (application level timeout)");
						break;
					}
				}
			} catch (Exception e) {
				internalDisconnect();
				if (connectionEstablished) {
					log(" - OOCSI disconnected (server unavailable)");
				}
			}
		}

		/**
		 * handle a whole message
		 * 
		 * @param fromServer
		 * @throws IOException
		 */
		public void handleMessage(String fromServer) throws IOException {

			// check message type
			if (!fromServer.startsWith("send")) {
				tempIncomingMessages.offer(fromServer);
				send(".");
				return;
			}

			// parse server output
			String[] tokens = fromServer.split(" ");
			if (tokens.length != 5) {
				return;
			}

			// get channel
			final String channel = tokens[1];
			Handler c = channels.get(channel);
			if (c == null && channel.equals(name.replaceFirst(":.*", ""))) {
				c = channels.get(SELF);
			}

			// parse the data
			handleData(channel, tokens[2], tokens[3], tokens[4], c);
		}

		/**
		 * handle the data pay-load within the message
		 * 
		 * @param channel
		 * @param data
		 * @param timestamp
		 * @param sender
		 * @param c
		 */
		private void handleData(final String channel, final String data, final String timestamp, final String sender,
				final Handler c) {

			// try to parse the data
			Map<String, Object> dataMap = parseData(data);
			if (dataMap != null) {
				handleMappedData(channel, data, timestamp, sender, c, dataMap);
			}

			// if dataMap not parseable and channel ready
			else if (c != null) {
				executor.submit(new Runnable() {
					public void run() {
						c.send(sender, data, timestamp, channel, name);
					}
				});
			}
		}

		/**
		 * handle the data pay-load map within the message
		 * 
		 * @param channel
		 * @param data
		 * @param timestamp
		 * @param sender
		 * @param c
		 * @param dataMap
		 */
		private void handleMappedData(final String channel, final String data, final String timestamp,
				final String sender, final Handler c, final Map<String, Object> dataMap) {
			// try to find a responder
			if (dataMap.containsKey(OOCSICall.MESSAGE_HANDLE)) {
				final Responder r = services.get((String) dataMap.get(OOCSICall.MESSAGE_HANDLE));
				if (r != null) {
					executor.submit(new Runnable() {
						public void run() {
							try {
								r.receive(sender, Handler.parseData(data), Handler.parseTimestamp(timestamp), channel,
										name);
							} catch (ClassNotFoundException e) {
								// nothing
							} catch (Exception e) {
								// nothing
							}
						}
					});
				}
				return;
			}

			// try to find an open call
			if (!openCalls.isEmpty() && dataMap.containsKey(OOCSICall.MESSAGE_ID)) {
				String id = (String) dataMap.get(OOCSICall.MESSAGE_ID);

				// walk from back to allow for removal
				for (int i = openCalls.size() - 1; i >= 0; i--) {
					final OOCSICall call = openCalls.get(i);
					if (!call.isValid()) {
						openCalls.remove(i);
					} else if (call.getId().equals(id)) {
						executor.submit(new Runnable() {
							public void run() {
								call.respond(dataMap);
							}
						});
						break;
					}
				}
				return;
			}

			// if no responder or call and channel ready waiting
			if (c != null) {
				executor.submit(new Runnable() {
					public void run() {
						c.send(sender, data, timestamp, channel, name);
					}
				});
			}

		}

		/**
		 * parse the message data
		 * 
		 * @param data
		 * @return
		 */
		private Map<String, Object> parseData(String data) {
			Map<String, Object> dataMap = null;
			try {
				dataMap = Handler.parseData(data);
			} catch (ClassNotFoundException e) {
				dataMap = null;
			} catch (IOException e) {
				dataMap = null;
			}
			return dataMap;
		}
	}

	class OOCSIAuthenticationException extends Exception {

		private static final long serialVersionUID = 5074228098705122200L;

	}
}
