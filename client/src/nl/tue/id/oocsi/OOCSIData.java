package nl.tue.id.oocsi;

import java.util.Map;

public interface OOCSIData {

	/**
	 * store data in message
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	OOCSIData data(String key, String value);

	/**
	 * store data in message
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	OOCSIData data(String key, int value);

	/**
	 * store data in message
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	OOCSIData data(String key, float value);

	/**
	 * store data in message
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	OOCSIData data(String key, double value);

	/**
	 * store data in message
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	OOCSIData data(String key, long value);

	/**
	 * store data in message
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	OOCSIData data(String key, Object value);

	/**
	 * store bulk data in message
	 * 
	 * @param bulkData
	 * @return
	 */
	OOCSIData data(Map<String, ? extends Object> bulkData);

	/**
	 * return internal representation of the data as a Map<String, Object>
	 * 
	 * @return
	 */
	Map<String, Object> internal();
}