package nl.tue.id.oocsi.client.protocol;

import java.util.Map;

import nl.tue.id.oocsi.OOCSIEvent;

/**
 * event handler for events with structured data
 *
 * @author matsfunk
 */
abstract public class EventHandler extends Handler {

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.tue.id.oocsi.client.protocol.Handler#receive(java.lang.String, java.util.Map, long, java.lang.String,
	 * java.lang.String)
	 */
	@Override
	public void receive(String sender, Map<String, Object> data, long timestamp, String channel,
			final String recipient) {
		receive(new OOCSIEvent(channel, data, sender, timestamp) {

			@Override
			public String getRecipient() {
				return recipient;
			}
		});
	}

	/**
	 * abstract method to be implemented in anonymous classes that are instantiated by subscribing and registering for
	 * events; encapsulates all incoming data as OOCSIEvent object
	 * 
	 * @param event
	 */
	abstract public void receive(OOCSIEvent event);
}
