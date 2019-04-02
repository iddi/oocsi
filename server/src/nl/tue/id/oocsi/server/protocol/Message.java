package nl.tue.id.oocsi.server.protocol;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * general message for the OOCSI protocol
 * 
 * @author matsfunk
 * 
 */
public class Message implements Serializable {

	/**
	 * id to be able to serialize across client and server
	 */
	private static final long serialVersionUID = 7907711514783823619L;

	/**
	 * id of sender (individual client)
	 */
	public String sender;
	/**
	 * id of receiver (can be a channel or an individual client)
	 */
	public String recipient;
	/**
	 * when the message was received on server
	 */
	public Date timestamp;
	/**
	 * data payload of message
	 */
	public Map<String, Object> data;

	/**
	 * create message from sender and recipient
	 * 
	 * @param sender
	 * @param recipient
	 */
	public Message(String sender, String recipient) {
		this(sender, recipient, new Date());
	}

	/**
	 * create message from sender and recipient with custom timestamp
	 * 
	 * @param sender
	 * @param recipient
	 * @param timestamp
	 */
	public Message(String sender, String recipient, Date timestamp) {
		this(sender, recipient, timestamp, new HashMap<String, Object>());
	}

	/**
	 * create full message
	 * 
	 * @param sender
	 * @param recipient
	 * @param timestamp
	 * @param data
	 */
	public Message(String sender, String recipient, Date timestamp, Map<String, Object> data) {
		this.sender = sender;
		this.recipient = recipient;
		this.timestamp = timestamp;
		this.data = data;
	}

	/**
	 * convenience method to add data (as a key/value pair) to an existing message
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	public Message addData(String key, Object value) {
		this.data.put(key, value);
		return this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "{sender: " + sender + ", recipient: " + recipient + ", timestamp: " + timestamp + ", data: " + data
				+ "}";
	}

}
