package nl.tue.id.oocsi.client.protocol;

import java.util.HashMap;
import java.util.Map;

import nl.tue.id.oocsi.OOCSIData;

public class OOCSIDataImpl implements OOCSIData {

	private final Map<String, Object> data = new HashMap<String, Object>();

	/**
	 * store data in message
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	@Override
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
	@Override
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
	@Override
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
	@Override
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
	@Override
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
	@Override
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
	@Override
	public OOCSIData data(Map<String, ? extends Object> bulkData) {

		// store data
		this.data.putAll(bulkData);

		return this;
	}

	/**
	 * get internal map
	 * 
	 * @return
	 */
	public Map<String, Object> internal() {
		return data;
	}

}
