package nl.tue.id.oocsi.client.protocol;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import nl.tue.id.oocsi.client.OOCSIClient;
import nl.tue.id.oocsi.client.services.OOCSICall;

public class MultiMessage extends OOCSIMessage {
	List<OOCSIMessage> messages = new LinkedList<OOCSIMessage>();

	public MultiMessage(OOCSIClient oocsi) {
		super(oocsi, "");
	}

	public MultiMessage(OOCSIClient oocsi, String channelName) {
		super(oocsi, "");
		add(new OOCSIMessage(oocsi, channelName));
	}

	public MultiMessage add(OOCSIMessage msg) {
		messages.add(msg);
		return this;
	}

	@Override
	public MultiMessage data(String key, String value) {
		for (OOCSIMessage msg : messages) {
			msg.data(key, value);
		}
		return this;
	}

	@Override
	public MultiMessage data(String key, boolean value) {
		for (OOCSIMessage msg : messages) {
			msg.data(key, value);
		}
		return this;
	}

	@Override
	public MultiMessage data(String key, int value) {
		for (OOCSIMessage msg : messages) {
			msg.data(key, value);
		}
		return this;
	}

	@Override
	public MultiMessage data(String key, float value) {
		for (OOCSIMessage msg : messages) {
			msg.data(key, value);
		}
		return this;
	}

	@Override
	public MultiMessage data(String key, double value) {
		for (OOCSIMessage msg : messages) {
			msg.data(key, value);
		}
		return this;
	}

	@Override
	public MultiMessage data(String key, long value) {
		for (OOCSIMessage msg : messages) {
			msg.data(key, value);
		}
		return this;
	}

	@Override
	public MultiMessage data(String key, Object value) {
		for (OOCSIMessage msg : messages) {
			msg.data(key, value);
		}
		return this;
	}

	@Override
	public MultiMessage data(Map<String, ? extends Object> bulkData) {
		for (OOCSIMessage msg : messages) {
			msg.data(bulkData);
		}
		return this;
	}

	@Override
	public void send() {
		for (OOCSIMessage msg : messages) {
			msg.send();
		}
	}

	/**
	 * send and wait for implicit timeout of 2 seconds
	 * 
	 */
	public void sendAndWait() {
		sendAndWait(2000);
	}

	/**
	 * send and wait for given timeout
	 * 
	 * @param timeoutMS
	 */
	public void sendAndWait(int timeoutMS) {

		boolean hasCalls = false;
		for (OOCSIMessage msg : messages) {
			msg.send();

			// check if there are any calls in the list of messages
			if (msg instanceof OOCSICall) {
				hasCalls |= true;
			}
		}

		// start waiting
		try {
			// either in one long wait
			if (!hasCalls) {
				Thread.sleep(timeoutMS);
			}
			// or until all calls have responses
			else {
				for (int i = 0; i < 10; i++) {
					boolean delivered = true;
					for (OOCSIMessage msg : messages) {
						if (msg instanceof OOCSICall) {
							delivered &= ((OOCSICall) msg).hasResponse();
						}
					}

					if (delivered) {
						break;
					}

					Thread.sleep(timeoutMS / 10);
				}
			}
		} catch (InterruptedException e) {
		}
	}

	/**
	 * retrieve all messages to allow for call returns
	 * 
	 * @return
	 */
	public List<OOCSIMessage> getMessages() {
		return messages;
	}
}
