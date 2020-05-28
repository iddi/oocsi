package nl.tue.id.oocsi.client.protocol;

import java.util.HashMap;
import java.util.Map;

import nl.tue.id.oocsi.OOCSIData;
import nl.tue.id.oocsi.OOCSIEvent;
import nl.tue.id.oocsi.client.OOCSIClient;

/**
 * message helper class for constructing and sending events to OOCSI
 * 
 * @author matsfunk
 */
public class OOCSIMessage extends OOCSIEvent implements OOCSIData {

	protected OOCSIClient oocsi;
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
	public OOCSIMessage data(String key, boolean value) {

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
	public OOCSIMessage data(String key, float value) {

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
	public OOCSIMessage data(String key, double value) {

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
	 * store data in message
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	public OOCSIMessage data(String key, Object value) {

		// store data
		this.data.put(key, value);

		return this;
	}

	/**
	 * store bulk data in message
	 * 
	 * @param bulkData
	 * @return
	 */
	public OOCSIMessage data(Map<String, ? extends Object> bulkData) {

		// store data
		this.data.putAll(bulkData);

		return this;
	}

	/**
	 * store bulk data in message
	 * 
	 * @param bulkData
	 * @return
	 */
	public OOCSIMessage data(OOCSIData bulkData) {

		// store data
		this.data.putAll(bulkData.internal());

		return this;
	}

	/**
	 * send message
	 * 
	 */
	public synchronized void send() {

		// but send only once
		if (!isSent) {
			isSent = true;
			oocsi.send(channelName, data);
		}
	}

	@Override
	public Map<String, Object> internal() {
		return this.data;
	}
}
