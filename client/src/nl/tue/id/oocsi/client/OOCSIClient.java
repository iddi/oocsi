package nl.tue.id.oocsi.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import nl.tue.id.oocsi.client.data.OOCSIDevice;
import nl.tue.id.oocsi.client.protocol.Handler;
import nl.tue.id.oocsi.client.services.OOCSICall;
import nl.tue.id.oocsi.client.services.Responder;
import nl.tue.id.oocsi.client.socket.SocketClient;

/**
 * OOCSI client wrapper for socket client
 * 
 * @author matsfunk
 */
public class OOCSIClient {

	public static final String VERSION = "1.4.4";

	protected SocketClient sc;
	protected String name;

	protected Map<String, Handler> channels = new HashMap<String, Handler>();
	protected Map<String, Responder> services = new HashMap<String, Responder>();

	/**
	 * create OOCSI client with a RANDOM name as the system-wide handle
	 * 
	 */
	public OOCSIClient() {
		this(null);
	}

	/**
	 * create OOCSI client with the given name as the system-wide handle
	 * 
	 * @param name
	 */
	public OOCSIClient(String name) {

		// check for empty name and replace by random generated name
		if (name == null || name.isEmpty()) {
			name = "OOCSIClient_" + (UUID.randomUUID().toString().replaceAll("-", "").substring(0, 15));
		}

		// check oocsi name
		if (name.contains(" ")) {
			log("[ERROR] OOCSI name cannot contain spaces");
			log(" - OOCSI connection aborted");
			return;
		} else if (name.contains("#")) {
			char[] newName = name.toCharArray();

			// replace "#" with random number
			for (int i = 0; i < newName.length; i++) {
				if (newName[i] == '#') {
					int rand = (int) Math.random()*9;
					newName[i] = Integer.toString(rand).charAt(0);
				}
			}

			// concate char array to a new string
			String randomizedName = new String(newName);
			this.name = randomizedName;
		} else {
			this.name = name;
		}

		sc = new SocketClient(name, channels, services) {
			public void log(String message) {
				OOCSIClient.this.log(message);
			}
		};
		log("OOCSI client v" + VERSION + " started: " + name);
	}

	/**
	 * get name of this OOCSI client
	 * 
	 * @return
	 */
	public String getName() {
		return sc.getName();
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
	 * reconnects the connection to OOCSI server
	 * 
	 * this is for testing, do NOT use for normal operation
	 */
	public void reconnect() {
		sc.reconnect();
	}

	/**
	 * retrieve whether we are still trying to reconnect, or whether we have given up on this connection (server,
	 * handle, etc.)
	 * 
	 * @return
	 */
	public boolean isReconnect() {
		return sc.isReconnect();
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
	 * register a responder with the socket client with a given handle "callName"
	 * 
	 * @param callName
	 * @param responder
	 */
	public void register(String callName, Responder responder) {
		responder.setOocsi(this);
		responder.setCallName(callName);
		sc.subscribe(callName, responder);
		sc.register(callName, responder);
	}

	/**
	 * register a responder with the socket client with a given handle "callName" on channel "channelName"
	 * 
	 * @param channelName
	 * @param callName
	 * @param responder
	 */
	public void register(String channelName, String callName, Responder responder) {
		responder.setOocsi(this);
		responder.setCallName(callName);
		sc.subscribe(channelName, responder);
		sc.register(callName, responder);
	}

	/**
	 * unregister a responder with the socket client with a given handle "callName"
	 * 
	 * @param callName
	 */
	public void unregister(String callName) {
		sc.unsubscribe(callName);
		sc.unregister(callName);
	}

	/**
	 * unregister a responder with the socket client with a given handle "callName" on channel "channelName"
	 * 
	 * @param channelName
	 * @param callName
	 */
	public void unregister(String channelName, String callName) {
		sc.unsubscribe(channelName);
		sc.unregister(callName);
	}

	/**
	 * send a string message to the channel with the given name
	 * 
	 * @param channelName
	 * @param message
	 */
	public void send(String channelName, String message) {
		if (channelName != null && channelName.trim().length() > 0) {
			sc.send(channelName, message);
		}
	}

	/**
	 * send a composite message (map) to the channel with the given name
	 * 
	 * @param channelName
	 * @param data
	 */
	public void send(String channelName, Map<String, Object> data) {
		if (channelName != null && channelName.trim().length() > 0) {
			sc.send(channelName, data);
		}
	}

	/**
	 * create an OOCSI device instance with the client's name that can be configured and then submitted to the OOCSI
	 * server
	 * 
	 * @return
	 */
	public OOCSIDevice heyOOCSI() {
		return new OOCSIDevice(this, this.name);
	}

	/**
	 * create a named OOCSI device that can be configured and then submitted to the OOCSI server
	 * 
	 * @param deviceName
	 * @return
	 */
	public OOCSIDevice heyOOCSI(String deviceName) {
		return new OOCSIDevice(this, deviceName);
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
