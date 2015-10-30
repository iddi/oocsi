package nl.tue.id.oocsi.client.protocol;

import java.util.HashMap;
import java.util.Map;

import nl.tue.id.oocsi.OOCSIEvent;
import nl.tue.id.oocsi.client.OOCSIClient;

/**
 * event handler for events with structured data
 *
 * @author matsfunk
 */
abstract public class Responder extends Handler {

	private OOCSIClient oocsi;

	public Responder(OOCSIClient oocsi) {
		this.oocsi = oocsi;
	}

	@Override
	public void receive(String sender, Map<String, Object> data, long timestamp, String channel, final String recipient) {
		Map<String, Object> response = new HashMap<String, Object>();
		respond(new OOCSIEvent(channel, data, sender, timestamp) {

			@Override
			public String getRecipient() {
				return recipient;
			}
		}, response);

		// send response
		response.put("MESSAGE_ID", data.get("MESSAGE_ID"));
		new OOCSIMessage(oocsi, sender).data("", 1).data(response).send();
	}

	abstract public void respond(OOCSIEvent event, Map<String, Object> response);

}
