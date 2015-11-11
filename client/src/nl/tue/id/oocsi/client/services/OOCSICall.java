package nl.tue.id.oocsi.client.services;

import java.util.Map;
import java.util.UUID;

import nl.tue.id.oocsi.OOCSIEvent;
import nl.tue.id.oocsi.client.OOCSIClient;
import nl.tue.id.oocsi.client.protocol.OOCSIMessage;

/**
 * call helper class for constructing, sending and receiving (function) calls over OOCSI
 * 
 * @author matsfunk
 */
public class OOCSICall extends OOCSIMessage {

	public static final String MESSAGE_HANDLE = "_MESSAGE_HANDLE";
	public static final String MESSAGE_ID = "_MESSAGE_ID";

	enum CALL_MODE {
		call_return, call_multi_return
	}

	private String uuid = "";
	private long expiration = 0;
	private int maxResponses = 1;
	private OOCSIEvent response = null;

	/**
	 * create a new message to the channel <channelName>
	 * 
	 * @param oocsi
	 * @param callName
	 * @param timeoutMS
	 * @param maxResponses
	 */
	public OOCSICall(OOCSIClient oocsi, String callName, int timeoutMS, int maxResponses) {
		this(oocsi, callName, callName, timeoutMS, maxResponses);
	}

	/**
	 * create a new message to the channel <channelName>
	 * 
	 * @param oocsi
	 * @param channelName
	 * @param callName
	 * @param timeoutMS
	 * @param maxResponses
	 */
	public OOCSICall(OOCSIClient oocsi, String channelName, String callName, int timeoutMS, int maxResponses) {
		super(oocsi, channelName);
		this.data(OOCSICall.MESSAGE_HANDLE, callName);

		this.expiration = System.currentTimeMillis() + timeoutMS;
		this.maxResponses = maxResponses;
	}

	/**
	 * return unique id of this OOCSI call
	 * 
	 * @return
	 */
	public String getId() {
		return uuid;
	}

	/**
	 * add a response to this open call
	 * 
	 * @param data
	 */
	public void respond(Map<String, Object> data) {
		response = new OOCSIEvent(super.sender, data, super.channelName);
	}

	/**
	 * check whether all values have been filled in for sending the call
	 * 
	 * @return
	 */
	public boolean canSend() {
		for (Map.Entry<String, Object> entry : data.entrySet()) {
			if (entry.getValue() == null) {
				return false;
			}
		}
		return true;
	}

	/**
	 * check whether this call is still not expired or has gotten a response
	 * 
	 * @return
	 */
	public boolean isValid() {
		return System.currentTimeMillis() < expiration && !hasResponse();
	}

	/**
	 * check whether this call has gotten a response
	 * 
	 * @return
	 */
	public boolean hasResponse() {
		return response != null;
	}

	/**
	 * retrieve the response as an OOCSIEvent
	 * 
	 * @return
	 */
	public OOCSIEvent getResponse() {
		return response;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.tue.id.oocsi.client.protocol.OOCSIMessage#send()
	 */
	@Override
	public void send() {
		// intercept to add message id
		uuid = UUID.randomUUID().toString();
		data(OOCSICall.MESSAGE_ID, uuid);

		// register centrally
		oocsi.register(this);
		// submit
		super.send();

		while (isValid()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// do nothing
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.tue.id.oocsi.client.protocol.OOCSIMessage#data(java.lang.String, java.lang.String)
	 */
	@Override
	public OOCSICall data(String key, String value) {
		return (OOCSICall) super.data(key, value);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.tue.id.oocsi.client.protocol.OOCSIMessage#data(java.lang.String, int)
	 */
	@Override
	public OOCSICall data(String key, int value) {
		return (OOCSICall) super.data(key, value);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.tue.id.oocsi.client.protocol.OOCSIMessage#data(java.lang.String, long)
	 */
	@Override
	public OOCSICall data(String key, long value) {
		return (OOCSICall) super.data(key, value);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.tue.id.oocsi.client.protocol.OOCSIMessage#data(java.lang.String, java.lang.Object)
	 */
	@Override
	public OOCSICall data(String key, Object value) {
		return (OOCSICall) super.data(key, value);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.tue.id.oocsi.client.protocol.OOCSIMessage#data(java.util.Map)
	 */
	@Override
	public OOCSICall data(Map<String, ? extends Object> bulkData) {
		return (OOCSICall) super.data(bulkData);
	}
}
