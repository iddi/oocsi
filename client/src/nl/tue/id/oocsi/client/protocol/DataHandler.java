package nl.tue.id.oocsi.client.protocol;

import java.util.Map;

/**
 * event handler for events with structured data
 *
 * @author matsfunk
 */
abstract public class DataHandler extends Handler {

	@Override
	public void receive(String sender, Map<String, Object> data, long timestamp, String channel, String recipient) {
		receive(sender, data, timestamp);
	}

	abstract public void receive(String sender, Map<String, Object> data, long timestamp);

}
