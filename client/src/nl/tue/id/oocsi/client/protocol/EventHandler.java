package nl.tue.id.oocsi.client.protocol;

import java.util.Map;

import nl.tue.id.oocsi.OOCSIEvent;

abstract public class EventHandler extends Handler {

	@Override
	public void receive(String sender, Map<String, Object> data, long timestamp, String channel, final String recipient) {
		receive(new OOCSIEvent(channel, data, sender, timestamp) {

			@Override
			public String getRecipient() {
				return recipient;
			}
		});
	}

	abstract public void receive(OOCSIEvent event);
}
