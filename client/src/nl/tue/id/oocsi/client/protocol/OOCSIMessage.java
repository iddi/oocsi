package nl.tue.id.oocsi.client.protocol;

import java.util.HashMap;

import nl.tue.id.oocsi.client.OOCSIClient;

/**
 * message class for sending events to OOCSI
 * 
 * @author mfunk
 * 
 */
public class OOCSIMessage extends OOCSIEvent {

	private OOCSIClient oocsi;
	private boolean isSent = false;

	/**
	 * create a new message
	 * 
	 * @param oocsi
	 * @param channelName
	 */
	public OOCSIMessage(OOCSIClient oocsi, String channelName) {
		super(channelName, new HashMap<String, Object>(), "");
		this.oocsi = oocsi;
	}

	/**
	 * store data in message
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	public OOCSIMessage data(String key, String value) {

		// store data
		this.data.put(key, value);

		return this;
	}

	/**
	 * store data in message
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	public OOCSIMessage data(String key, int value) {

		// store data
		this.data.put(key, value);

		return this;
	}

	/**
	 * store data in message
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	public OOCSIMessage data(String key, long value) {

		// store data
		this.data.put(key, value);

		return this;
	}

	/**
	 * send message
	 * 
	 */
	public void send() {

		// but send only once
		if (!isSent) {
			isSent = true;
			oocsi.send(channelName, data);
		}
	}
}
