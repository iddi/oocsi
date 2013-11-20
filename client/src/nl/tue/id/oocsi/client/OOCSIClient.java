package nl.tue.id.oocsi.client;

import java.util.HashMap;
import java.util.Map;

import nl.tue.id.oocsi.client.protocol.Handler;
import nl.tue.id.oocsi.client.socket.SocketClient;

public class OOCSIClient {

	public static final String VERSION = "0.3";

	private static boolean isLogging = true;

	private Map<String, Handler> channels = new HashMap<String, Handler>();

	private SocketClient sc;

	/**
	 * 
	 * 
	 * @param name
	 */
	public OOCSIClient(String name) {

		// check oocsi name
		if (name.contains(" ")) {
			System.err.println("OOCSI name should not contain spaces");
			System.exit(-1);
		}

		sc = new SocketClient(name, channels);
		log("OOCSI client v" + VERSION + " started");
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
	 * retrieve the list of sub-channel of the channel with the given name on
	 * the server
	 * 
	 * @param channelName
	 * @return
	 */
	public String channels(String channelName) {
		return sc.channels(channelName);
	}

	/**
	 * logging of message on console (can be switched off)
	 * 
	 * @param message
	 */
	public static void log(String message) {
		if (isLogging) {
			System.out.println(message);
		}
	}
}
