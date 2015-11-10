package nl.tue.id.oocsi.client.services;

import java.util.HashMap;
import java.util.Map;

import nl.tue.id.oocsi.OOCSIEvent;
import nl.tue.id.oocsi.client.OOCSIClient;
import nl.tue.id.oocsi.client.protocol.Handler;
import nl.tue.id.oocsi.client.protocol.OOCSIMessage;

/**
 * internal event handler for synchronized events with structured data
 *
 * @author matsfunk
 */
abstract public class Responder extends Handler {

	private OOCSIClient oocsi;

	private String callName;

	/**
	 * constructor for easy client-side instantiation with service methods
	 */
	public Responder() {
	}

	/**
	 * constructor for full instantiation without service methods
	 * 
	 * @param oocsi
	 * @param callName
	 */
	public Responder(OOCSIClient oocsi, String callName) {
		this.oocsi = oocsi;
		this.callName = callName;
	}

	/**
	 * set oocsi client for responses
	 * 
	 * @param oocsi
	 */
	public void setOocsi(OOCSIClient oocsi) {
		this.oocsi = oocsi;
	}

	/**
	 * set call name for responses
	 * 
	 * @param callName
	 */
	public void setCallName(String callName) {
		this.callName = callName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.tue.id.oocsi.client.protocol.Handler#receive(java.lang.String, java.util.Map, long, java.lang.String,
	 * java.lang.String)
	 */
	@Override
	public void receive(String sender, Map<String, Object> data, long timestamp, String channel, final String recipient) {

		// check if this needs a response
		if (!data.get(OOCSICall.MESSAGE_HANDLE).equals(callName)) {
			return;
		}

		// correct call to respond to
		Map<String, Object> response = new HashMap<String, Object>();
		respond(new OOCSIEvent(channel, data, sender, timestamp) {

			@Override
			public String getRecipient() {
				return recipient;
			}
		}, response);

		// send response
		response.put(OOCSICall.MESSAGE_ID, data.get(OOCSICall.MESSAGE_ID));
		new OOCSIMessage(oocsi, sender).data("", 1).data(response).send();
	}

	/**
	 * interface for responding to a call, needs to be implemented client-side
	 * 
	 * @param event
	 * @param response
	 */
	abstract public void respond(OOCSIEvent event, Map<String, Object> response);

}
