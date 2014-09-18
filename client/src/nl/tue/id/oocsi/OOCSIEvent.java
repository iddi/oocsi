package nl.tue.id.oocsi;

import java.util.Map;

/**
 * event class for receiving events from OOCSI
 * 
 * @author mfunk
 * 
 */
public class OOCSIEvent {

	protected String channelName;
	protected String recipient;
	protected String sender;
	protected String timestamp;

	protected Map<String, Object> data;

	/**
	 * constructor
	 * 
	 * @param channelName
	 * @param data
	 * @param sender
	 */
	public OOCSIEvent(String sender, Map<String, Object> data, String timestamp, String channelName, String recipient) {
		this.channelName = channelName;
		this.data = data;
		this.recipient = recipient;
		this.sender = sender;
		this.timestamp = timestamp;
	}

	/**
	 * get value for the given key as boolean
	 * 
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public boolean getBoolean(String key, boolean defaultValue) {
		Object result = this.data.get(key);
		if (result != null) {
			if (result instanceof Boolean) {
				return ((Boolean) result).booleanValue();
			} else {
				try {
					return Boolean.parseBoolean(result.toString());
				} catch (NumberFormatException nfe) {
					return defaultValue;
				}
			}
		} else {
			return defaultValue;
		}
	}

	/**
	 * get value for the given key as int
	 * 
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public int getInt(String key, int defaultValue) {
		Object result = this.data.get(key);
		if (result != null) {
			if (result instanceof Integer) {
				return ((Integer) result).intValue();
			} else {
				try {
					return Integer.parseInt(result.toString());
				} catch (NumberFormatException nfe) {
					return defaultValue;
				}
			}
		} else {
			return defaultValue;
		}
	}

	/**
	 * get value for the given key as long
	 * 
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public long getLong(String key, long defaultValue) {
		Object result = this.data.get(key);
		if (result != null) {
			if (result instanceof Long) {
				return ((Long) result).longValue();
			} else {
				try {
					return Long.parseLong(result.toString());
				} catch (NumberFormatException nfe) {
					return defaultValue;
				}
			}
		} else {
			return defaultValue;
		}
	}

	/**
	 * get value for the given key as float
	 * 
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public float getFloat(String key, float defaultValue) {
		Object result = this.data.get(key);
		if (result != null) {
			if (result instanceof Float) {
				return ((Float) result).floatValue();
			} else {
				try {
					return Float.parseFloat(result.toString());
				} catch (NumberFormatException nfe) {
					return defaultValue;
				}
			}
		} else {
			return defaultValue;
		}
	}

	/**
	 * get value for the given key as double
	 * 
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public double getDouble(String key, double defaultValue) {
		Object result = this.data.get(key);
		if (result != null) {
			if (result instanceof Double) {
				return ((Double) result).doubleValue();
			} else {
				try {
					return Double.parseDouble(result.toString());
				} catch (NumberFormatException nfe) {
					return defaultValue;
				}
			}
		} else {
			return defaultValue;
		}
	}

	/**
	 * get the value for the given key as String
	 * 
	 * @param key
	 * @return
	 */
	public String getString(String key) {
		return this.data.get(key).toString();
	}

	/**
	 * get the value for the given key as String
	 * 
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public String getString(String key, String defaultValue) {
		Object result = this.data.get(key);
		return result != null ? result.toString() : defaultValue;
	}

	/**
	 * get the value for the given key as Object
	 * 
	 * @param key
	 * @return
	 */
	public Object getObject(String key) {
		return this.data.get(key);
	}

	/**
	 * check if the event contains data with the key
	 * 
	 * @param key
	 * @return
	 */
	public boolean has(String key) {
		return this.data.containsKey(key);
	}

	/**
	 * retrieve all keys from the event
	 * 
	 * @return
	 */
	public String[] keys() {
		return this.data.keySet().toArray(new String[] {});
	}

	/**
	 * get the name or handle of the sender who sent this event
	 * 
	 * @return
	 */
	public String getSender() {
		return sender;
	}

	/**
	 * get the name of the recipient (or channel) this event was sent to
	 * 
	 * @return
	 */
	public String getRecipient() {
		return recipient;
	}

	/**
	 * get the name of the channel (or recipient) this event was sent to
	 * 
	 * @return
	 */
	public String getChannel() {
		return channelName;
	}

	/**
	 * get the timestamp of the event
	 *
	 * @return
	 */
	public String getTimestamp() { return timestamp; }
}
