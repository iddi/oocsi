package nl.tue.id.oocsi.client.protocol;

import java.io.IOException;
import java.util.Map;

import nl.tue.id.oocsi.client.data.JSONReader;

/**
 * event handler for events with structured data
 *
 * @author matsfunk
 */
abstract public class Handler {

	/**
	 * raw data wrapper; will parse the incoming data and forward the event to the actual handler
	 * 
	 * @param sender
	 * @param data
	 * @param timestamp
	 * @param channel
	 * @param recipient
	 */
	public void send(String sender, String data, String timestamp, String channel, String recipient) {
		try {
			final Map<String, Object> map = parseData(data);
			final long ts = parseTimestamp(timestamp);

			// forward event
			receive(sender, map, ts, channel, recipient);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * abstract method to be implemented in anonymous classes that are instantiated by subscribing and registering for
	 * events
	 * 
	 * @param sender
	 * @param data
	 * @param timestamp
	 * @param channel
	 * @param recipient
	 */
	abstract public void receive(String sender, Map<String, Object> data, long timestamp, String channel,
	        String recipient);

	/**
	 * parse the given "data" String into a Map
	 * 
	 * @param data
	 * @return
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, Object> parseData(String data) throws IOException, ClassNotFoundException {
		return (Map<String, Object>) new JSONReader().read(data);
	}

	/**
	 * parse the given "timestamp" String into a long value
	 * 
	 * @param timestamp
	 * @return
	 */
	public static long parseTimestamp(String timestamp) {
		long ts = System.currentTimeMillis();
		try {
			ts = Long.parseLong(timestamp);
		} catch (Exception e) {
			// do nothing
		}
		return ts;
	}
}
