package nl.tue.id.oocsi.client.protocol;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Handler that internally maintains a list of sub-handlers which will be called in FIFO order once this handler is
 * called; this is very helpful for multiple subscriptions to a channel
 *
 * @author matsfunk
 */
public class MultiHandler extends Handler {

	List<Handler> subscribers = new LinkedList<Handler>();

	public MultiHandler() {
	}

	public MultiHandler(Handler handler) {
		add(handler);
	}

	public void add(Handler handler) {
		subscribers.add(handler);
	}

	@Override
	public void receive(String sender, Map<String, Object> data, long timestamp, String channel, String recipient) {
		for (Handler handler : subscribers) {
			handler.receive(sender, data, timestamp, channel, recipient);
		}
	}
}
