package nl.tue.id.oocsi.client.socket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.util.Map;

import nl.tue.id.oocsi.client.data.JSONWriter;
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

	static final String SELF = "SELF";
	private static final int MULTICAST_PORT = 4448;
	private static final String MULTICAST_GROUP = "224.0.0.144";

	private final String name;
	private boolean reconnect = false;

	private final Map<String, Handler> channels;
	private final Map<String, Responder> services;

	private SocketClientRunner runner;

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
				Thread.sleep(1000);
			}

			// nothing found for 10 * 5 seconds
			socket.leaveGroup(group);
			return isConnected();
		} catch (IOException ioe) {
			// problem occurred with connection
			return false;
		} catch (InterruptedException ioe) {
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
	public synchronized boolean connect(final String hostname, final int port) {

		// handle existing runner, graceful shutdown
		if (runner != null) {
			runner.disconnect();
		}

		// start connection thread with a logging redirect to this class
		runner = new SocketClientRunner(name, hostname, port, channels, services) {
			@Override
			public void log(String message) {
				SocketClient.this.log(message);
			}
		};
		runner.reconnect = reconnect;

		// check back on connection progress
		while (runner.isConnectionInProgress()) {
			runner.sleep(100);
		}

		// return connection status
		return runner.connectionEstablished;
	}

	/**
	 * check if still connected to OOCSI
	 * 
	 * @return
	 */
	public boolean isConnected() {
		return runner != null && runner.isConnected();
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
	 * set whether or not a reconnection attempt should be made if a connection fails
	 * 
	 * @param reconnect
	 */
	public void setReconnect(boolean reconnect) {
		this.reconnect = reconnect;
		if (runner != null) {
			runner.reconnect = reconnect;
		}
	}

	/**
	 * subscribe to channel given by channelName
	 * 
	 * @param channelName
	 * @param handler
	 */
	public void subscribe(String channelName, Handler handler) {

		// subscribe to channel if not done yet
		if (runner != null && !internalIsSubscribed(channelName)) {
			runner.subscribe(channelName);
		}

		// add handler to internal multi-handler
		internalAddHandler(channelName, handler);
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
		if (runner != null) {
			runner.send("subscribe " + name);
		}

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
		if (runner != null) {
			runner.send("unsubscribe " + channelName);
		}

		// remove handler
		internalRemoveHandler(channelName, null);
	}

	/**
	 * unsubscribe from my channel
	 * 
	 */
	public void unsubscribe() {

		// unregister at server
		if (runner != null) {
			runner.send("unsubscribe " + name);
		}

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
		if (runner != null) {
			runner.openCalls.add(call);
		}
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
		if (runner != null) {
			runner.send("sendraw " + channelName + " " + message);
		}
	}

	/**
	 * send message with data payload (map of key value pairs which will be serialized before sending)
	 * 
	 * @param channelName
	 * @param data
	 */
	public void send(String channelName, Map<String, Object> data) {
		// send message with raw data
		if (runner != null) {
			runner.send("send " + channelName + " " + serialize(data));
		}
	}

	/**
	 * retrieve the current channels on server
	 * 
	 * @return
	 */
	public String clients() {
		return runner != null ? runner.sendSyncPoll("clients") : "";
	}

	/**
	 * retrieve the current channels on server
	 * 
	 * @return
	 */
	public String channels() {
		return runner != null ? runner.sendSyncPoll("channels") : "";
	}

	/**
	 * retrieve the current sub-channels of the given channel on server
	 * 
	 * @param channelName
	 * @return
	 */
	public String channels(String channelName) {
		return runner != null ? runner.sendSyncPoll("channels " + channelName) : "";
	}

	public void disconnect() {
		if (runner != null) {
			runner.disconnect();
		}
	}

	public void kill() {
		if (runner != null) {
			runner.kill();
		}
	}

	public void reconnect() {
		if (runner != null) {
			runner.reconnect();
		}
	}

	public boolean isReconnect() {
		return runner != null && runner.reconnect;
	}

	/**
	 * serialize a map of key value pairs
	 * 
	 * @param data
	 * @return
	 */
	private String serialize(Map<String, Object> data) {
		return new JSONWriter().write(data);
	}

	/**
	 * logging of message on console (can be overridden by subclass)
	 */
	public void log(String message) {
		// no logging by default
	}

	static public class OOCSIAuthenticationException extends Exception {

		private static final long serialVersionUID = 5074228098705122200L;

	}
}
