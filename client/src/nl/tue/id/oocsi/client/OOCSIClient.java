package nl.tue.id.oocsi.client;

import java.util.HashMap;
import java.util.Map;

import nl.tue.id.oocsi.client.protocol.Handler;
import nl.tue.id.oocsi.client.socket.SocketClient;

public class OOCSIClient {

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

	public void unsubscribe(String channelName) {
		sc.unsubscribe(channelName);
	}

	public void send(String channelName, String message) {
		sc.send(channelName, message);
	}

	public void send(String channelName, Map<String, Object> data) {
		sc.send(channelName, data);
	}

}
