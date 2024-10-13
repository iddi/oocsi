package nl.tue.id.oocsi.server.protocol;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
	 * message attribute key to flag a message that should be retained on a channel (for n seconds)
	 */
	public static final String RETAIN_MESSAGE = "_RETAIN";
	/**
	 * message attribute key to flag a message that should be delivered with a delay (in at least n seconds)
	 */
	public static final String DELAY_MESSAGE = "_DELAY";

	/**
	 * id of sender (individual client)
	 */
	private String sender;

	/**
	 * id of receiver (can be a channel or an individual client)
	 */
	private String recipient;
	/**
	 * when the message was received on server
	 */
	private Date timestamp;
	/**
	 * data payload of message
	 */
	public Map<String, Object> data;
	/**
	 * until when the message is valid (used for retained messages)
	 */
	public Date validUntil;

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
		this.sender = sender;
		this.recipient = recipient;
		this.timestamp = timestamp;
		this.data = new ConcurrentHashMap<String, Object>();
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
		this(sender, recipient, timestamp);
		if (data != null) {
			this.data.putAll(data);
		}
	}

	public String getSender() {
		return sender;
	}

	public String getRecipient() {
		return recipient;
	}

	public Date getTimestamp() {
		return timestamp;
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

	/**
	 * clones with message with a new, given recipient
	 * 
	 * @param recipient
	 * @return
	 */
	public Message cloneForRecipient(String recipient) {
		return new Message(this.sender, recipient, this.timestamp, this.data);
	}

	/**
	 * check whether the message is still valid
	 * 
	 * @return
	 */
	public boolean isValid() {
		return validUntil == null || validUntil.after(new Date());
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
