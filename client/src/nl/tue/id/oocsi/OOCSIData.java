package nl.tue.id.oocsi;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("serial")
public class OOCSIData extends HashMap<String, Object> {

	Map<String, Object> data = this;

	/**
	 * store data in message
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	public OOCSIData data(String key, String value) {

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
	public OOCSIData data(String key, int value) {

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
	public OOCSIData data(String key, float value) {

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
	public OOCSIData data(String key, double value) {

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
	public OOCSIData data(String key, long value) {

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
	public OOCSIData data(String key, Object value) {

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
	public OOCSIData data(Map<String, ? extends Object> bulkData) {

		// store data
		this.data.putAll(bulkData);

		return this;
	}

}
