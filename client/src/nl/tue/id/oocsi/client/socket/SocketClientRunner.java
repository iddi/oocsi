package nl.tue.id.oocsi.client.socket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
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
import nl.tue.id.oocsi.client.services.OOCSICall;
import nl.tue.id.oocsi.client.services.Responder;
import nl.tue.id.oocsi.client.socket.SocketClient.OOCSIAuthenticationException;

public class SocketClientRunner implements Runnable {

	final private String name;
	final private String hostname;
	final private int port;

	// i/o
	private Socket socket;
	private BufferedReader input;
	private PrintWriter output;

	// connection flags
	protected boolean connectionEstablished = false;
	boolean reconnect = false;

	private boolean disconnected = false;
	private int reconnectCountDown = 100;
	private boolean relinquished = false;
	private boolean hasPrintedServerInfo = false;

	// testing
	private boolean noPing = false;
	private boolean noProcess = false;

	// management
	private final Map<String, Handler> channels;
	private final Map<String, Responder> services;
	final List<OOCSICall> openCalls;
	private final Queue<String> tempIncomingMessages;

	// thread pool
	private ExecutorService executor;

	public SocketClientRunner(String name, String hostname, int port, Map<String, Handler> channels,
	        Map<String, Responder> services) {
		this.name = name;
		this.hostname = hostname;
		this.port = port;
		this.channels = channels;
		this.services = services;

		this.openCalls = new LinkedList<OOCSICall>();
		this.tempIncomingMessages = new LinkedBlockingQueue<String>(1);

		this.executor = Executors.newCachedThreadPool();

		// start
		executor.execute(this);
	}

	/**
	 * for testing only
	 * 
	 * @param name
	 * @param hostname
	 * @param port
	 * @param channels
	 * @param services
	 * @param noPing
	 * @param noProcess
	 */
	public SocketClientRunner(String name, String hostname, int port, Map<String, Handler> channels,
	        Map<String, Responder> services, boolean noPing, boolean noProcess) {
		this(name, hostname, port, channels, services);
		this.noPing = noPing;
		this.noProcess = noProcess;
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
					if (hostname != null && connectAttempt(hostname, port)) {
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
			output.println(name + "(JSON)");

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
			if (!serverWelcomeMessage.contains("welcome " + name)) {
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
				this.subscribe(channelName);
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
	 * return true if the connection is currently in progress
	 * 
	 * @return
	 */
	protected boolean isConnectionInProgress() {
		return !disconnected && !connectionEstablished && reconnectCountDown > 0;
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
				while (input.ready() && !noProcess && (fromServer = input.readLine()) != null) {
					handleMessage(fromServer);
					cyclesSinceRead = 0;
				}

				// sleep if there is nothing to read
				Thread.sleep(10);

				// if no data came in for 20 secs, kill connection and reconnect
				if (cyclesSinceRead++ > 2000) {
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

		// check for JSON message
		if (fromServer.startsWith("{")) {
			Map<String, Object> map = parseData(fromServer);
			String channel = map.remove("recipient").toString();
			Handler c = channels.get(channel);
			if (c == null && channel.equals(name.replaceFirst(":.*", ""))) {
				c = channels.get(SocketClient.SELF);
			}
			String sender = map.remove("sender").toString();
			String timestamp = map.remove("timestamp").toString();

			handleMappedData(channel, fromServer, timestamp, sender, c, map);
			return;
		}

		// any other non-send message
		if (!fromServer.startsWith("send") && !noPing) {
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
			c = channels.get(SocketClient.SELF);
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
	private void handleMappedData(final String channel, final String data, final String timestamp, final String sender,
	        final Handler c, final Map<String, Object> dataMap) {
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

	///////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * send the message to the socket
	 * 
	 * @param rawMessage
	 */
	void send(String rawMessage) {
		if (isConnected() && output != null) {
			output.println(rawMessage);
		}
	}

	/**
	 * send message, wait for immediate results and return them (legacy comms)
	 * 
	 * @param message
	 * @return
	 */
	String sendSyncPoll(String message) {
		tempIncomingMessages.clear();
		send(message);
		return syncPoll();
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
	 * subscribe to given channelName
	 * 
	 * @param channelName
	 */
	void subscribe(String channelName) {
		// register at server
		send("subscribe " + channelName);

		// check for replacement
		if (channels.get(channelName) != null) {
			log(" - reconnected subscription for " + channelName);
		}
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

		// handshake with server if possible
		if (output != null) {
			output.println("quit");
		}
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

	///////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * check if still connected to OOCSI
	 * 
	 * @return
	 */
	public boolean isConnected() {
		return socket != null && !socket.isClosed() && socket.isConnected();
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

	///////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * let thread sleep for ms milliseconds
	 * 
	 */
	protected void sleep(int ms) {
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

}
