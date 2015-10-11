package nl.tue.id.oocsi.client.protocol;

import java.util.Map;
import java.util.UUID;

import nl.tue.id.oocsi.client.OOCSIClient;

/**
 * call helper class for constructing, sending and receiving (function) calls over OOCSI
 * 
 * @author matsfunk
 */
public class OOCSICall extends OOCSIMessage {

	private long expiration = 0;
	private String uuid = "";
	private Map<String, Object> response = null;

	enum CALL_MODE {
		call_return, call_multi_return

	}

	/**
	 * create a new message
	 * 
	 * @param oocsi
	 * @param channelName
	 * @param timeoutMS
	 * @param maxResponses
	 */
	public OOCSICall(OOCSIClient oocsi, String channelName, int timeoutMS, int maxResponses) {
		super(oocsi, channelName);

		expiration = System.currentTimeMillis() + timeoutMS;
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
		response = data;
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
	 * retrieve the response
	 * 
	 * @return
	 */
	public Map<String, Object> getResponse() {
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
		data("MESSAGE_ID", uuid);

		// register centrally
		oocsi.register(this);
		// submit
		super.send();

		while (isValid()) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// do nothing
			}
		}
	}

	@Override
	public OOCSICall data(String key, String value) {
		return (OOCSICall) super.data(key, value);
	}

	@Override
	public OOCSICall data(String key, int value) {
		return (OOCSICall) super.data(key, value);
	}

	@Override
	public OOCSICall data(String key, long value) {
		return (OOCSICall) super.data(key, value);
	}

	@Override
	public OOCSICall data(Map<String, ? extends Object> bulkData) {
		return (OOCSICall) super.data(bulkData);
	}
}
