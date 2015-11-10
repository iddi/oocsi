package nl.tue.id.oocsi.client;

import java.util.HashMap;
import java.util.Map;

import nl.tue.id.oocsi.client.protocol.Handler;
import nl.tue.id.oocsi.client.services.OOCSICall;
import nl.tue.id.oocsi.client.services.Responder;
import nl.tue.id.oocsi.client.socket.SocketClient;

/**
 * OOCSI client wrapper
 * 
 * @author matsfunk
 */
public class OOCSIClient {

	public static final String VERSION = "0.8";

	private Map<String, Handler> channels = new HashMap<String, Handler>();
	private Map<String, Responder> services = new HashMap<String, Responder>();

	private SocketClient sc;

	/**
	 * create OOCSI client with the given name as the system-wide handle
	 * 
	 * @param name
	 */
	public OOCSIClient(String name) {

		// check oocsi name
		if (name.contains(" ")) {
			log("OOCSI name should not contain spaces");
			System.exit(-1);
		}

		sc = new SocketClient(name, channels, services) {
			public void log(String message) {
				OOCSIClient.this.log(message);
			}
		};
		log("OOCSI client v" + VERSION + " started");
	}

	/**
	 * connect to OOCSI network without a concrete server given, i.e., wait for multi-cast messages broadcasting a
	 * server to connect to
	 * 
	 * @return
	 */
	public boolean connect() {
		return sc.startMulticastLookup();
	}

	/**
	 * connect to OOCSI network
	 * 
	 * @param hostname
	 * @param port
	 * @return
	 */
	public boolean connect(String hostname, int port) {
		return sc.connect(hostname, port);
	}

	/**
	 * check connection to OOCSI network
	 * 
	 * @return
	 */
	public boolean isConnected() {
		return sc.isConnected();
	}

	/**
	 * disconnect from OOCSI network
	 * 
	 */
	public void disconnect() {
		sc.disconnect();
	}

	/**
	 * kills the connection to OOCSI server
	 * 
	 * this is for testing, do NOT use for normal operation
	 */
	public void kill() {
		sc.kill();
	}

	/**
	 * set whether or not a reconnection attempt should be made if a connection fails
	 * 
	 * @param reconnect
	 */
	public void setReconnect(boolean reconnect) {
		sc.setReconnect(reconnect);
	}

	/**
	 * subscribe to the channel with the given name
	 * 
	 * @param channelName
	 * @param handler
	 */
	public void subscribe(String channelName, Handler handler) {
		sc.subscribe(channelName, handler);
	}

	/**
	 * subscribe to my own channel
	 * 
	 * @param handler
	 */
	public void subscribe(Handler handler) {
		sc.subscribe(handler);
	}

	/**
	 * unsubscribe from the channel with the given name
	 * 
	 * @param channelName
	 */
	public void unsubscribe(String channelName) {
		sc.unsubscribe(channelName);
	}

	/**
	 * register a call with the socket client
	 * 
	 * @param call
	 */
	public void register(OOCSICall call) {
		sc.register(call);
	}

	/**
	 * register a responder with the socket client with a given handle <callName>
	 * 
	 * @param callName
	 * @param responder
	 */
	public void register(String callName, Responder responder) {
		sc.subscribe(callName, responder);
		sc.register(callName, responder);
	}

	/**
	 * send a string message to the channel with the given name
	 * 
	 * @param channelName
	 * @param message
	 */
	public void send(String channelName, String message) {
		sc.send(channelName, message);
	}

	/**
	 * send a composite message (map) to the channel with the given name
	 * 
	 * @param channelName
	 * @param data
	 */
	public void send(String channelName, Map<String, Object> data) {
		sc.send(channelName, data);
	}

	/**
	 * retrieve the list of clients on the server
	 * 
	 * @return
	 */
	public String clients() {
		return sc.clients();
	}

	/**
	 * retrieve the list of channels on the server
	 * 
	 * @return
	 */
	public String channels() {
		return sc.channels();
	}

	/**
	 * retrieve the list of sub-channel of the channel with the given name on the server
	 * 
	 * @param channelName
	 * @return
	 */
	public String channels(String channelName) {
		return sc.channels(channelName);
	}

	/**
	 * logging of message on console (can be overridden by subclass)
	 * 
	 * @param message
	 */
	public void log(String message) {
		// no logging by default
	}
}
