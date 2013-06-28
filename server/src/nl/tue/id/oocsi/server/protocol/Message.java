package nl.tue.id.oocsi.server.protocol;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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
	 * @param s
	 * @param r
	 */
	public Message(String s, String r) {
		this(s, r, new Date());
	}

	/**
	 * create message from sender and recipient with custom timestamp
	 * 
	 * @param s
	 * @param r
	 * @param ts
	 */
	public Message(String s, String r, Date ts) {
		this(s, r, ts, new HashMap<String, Object>());
	}

	/**
	 * create full message
	 * 
	 * @param s
	 * @param r
	 * @param ts
	 * @param data
	 */
	public Message(String s, String r, Date ts, Map<String, Object> data) {
		this.sender = s;
		this.recipient = r;
		this.timestamp = ts;
		this.data = data;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "{sender: " + sender + ", recipient: " + recipient
				+ ", timestamp: " + timestamp + ", data: " + data + "}";
	}

}
