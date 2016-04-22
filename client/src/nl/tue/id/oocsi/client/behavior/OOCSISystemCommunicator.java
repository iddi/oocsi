package nl.tue.id.oocsi.client.behavior;

import java.util.Collections;

import nl.tue.id.oocsi.client.OOCSIClient;
import nl.tue.id.oocsi.client.protocol.Handler;
import nl.tue.id.oocsi.client.protocol.OOCSIMessage;

public class OOCSISystemCommunicator<T> {

	protected OOCSIClient client;
	protected String channelName;
	private Handler handler;

	public OOCSISystemCommunicator(OOCSIClient client, String channelName) {
		this(client, channelName, null);
	}

	public OOCSISystemCommunicator(OOCSIClient client, String channelName, Handler handler) {
		this.client = client;
		this.channelName = channelName;
		this.handler = handler;
	}

	protected void message(String command, T data) {
		new OOCSIMessage(client, channelName).data(command, data).send();
	}

	protected void message(String command) {
		new OOCSIMessage(client, channelName).data(command, "").send();
	}

	protected void triggerHandler() {
		if (handler != null) {
			handler.receive(channelName, Collections.<String, Object> emptyMap(), System.currentTimeMillis(),
					channelName, client.getName());
		}
	}
}
