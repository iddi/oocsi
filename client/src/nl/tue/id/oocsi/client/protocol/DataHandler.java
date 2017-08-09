package nl.tue.id.oocsi.client.protocol;

import java.util.Map;

/**
 * event handler for events with structured data
 *
 * @author matsfunk
 */
abstract public class DataHandler extends Handler {

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.tue.id.oocsi.client.protocol.Handler#receive(java.lang.String, java.util.Map, long, java.lang.String,
	 * java.lang.String)
	 */
	@Override
	public void receive(String sender, Map<String, Object> data, long timestamp, String channel, String recipient) {
		receive(sender, data, timestamp);
	}

	/**
	 * abstract method to be implemented in anonymous classes that are instantiated by subscribing and registering for
	 * events; encapsulates all incoming data as data map (mostly used for testing)
	 * 
	 * @param sender
	 * @param data
	 * @param timestamp
	 */
	abstract public void receive(String sender, Map<String, Object> data, long timestamp);

}
