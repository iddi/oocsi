package nl.tue.id.oocsi.client.socket;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

	private final String name;
	private boolean reconnect = false;

	private final Map<String, Handler> channels;
	private final Map<String, Responder> services;

	protected SocketClientRunner runner;

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
	 * @deprecated Multicast lookup has been removed. Specify explicit server endpoints instead.
	 */
	@Deprecated
	public boolean startMulticastLookup() {
		log("WARNING: Multicast lookup has been removed. Specify explicit host and port via connect(host, port).");
		return false;
	}

	/**
	 * start pinging for a multi-cast lookup with optional pre-shared key validation
	 * 
	 * @param preSharedKey optional key
	 * @return
	 * @deprecated Multicast lookup has been removed. Specify explicit server endpoints instead.
	 */
	@Deprecated
	public boolean startMulticastLookup(String preSharedKey) {
		log("WARNING: Multicast lookup has been removed. Specify explicit host and port via connect(host, port).");
		return false;
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
		return runner != null && runner.isConnected() ? runner.getName() : name;
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
	 * subscribe to my own channel
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
	 * unsubscribe from my own channel
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
	 * unsubscribe from channel given by channelName
	 * 
	 * @param channelName
	 */
	public void unsubscribe(String channelName) {
		unsubscribe(channelName, null);
	}

	/**
	 * unsubscribe from channel given by channelName and handler
	 * 
	 * @param channelName
	 * @param handler
	 */
	public void unsubscribe(String channelName, Handler handler) {
		internalRemoveHandler(channelName, handler);
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

				// if there are no handlers left...
				if (mh.isEmpty()) {
					// remove channel
					channels.remove(channelName);

					// unregister at server
					if (runner != null) {
						runner.send("unsubscribe " + channelName);
					}
				}
			}
		} else {
			channels.remove(channelName);

			// unregister at server
			if (runner != null) {
				runner.send("unsubscribe " + channelName);
			}
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
