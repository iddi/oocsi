package nl.tue.id.oocsi.client.behavior;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import nl.tue.id.oocsi.OOCSIEvent;
import nl.tue.id.oocsi.client.OOCSIClient;
import nl.tue.id.oocsi.client.protocol.EventHandler;
import nl.tue.id.oocsi.client.protocol.Handler;
import nl.tue.id.oocsi.client.protocol.OOCSIMessage;

public class OOCSISystemCommunicator<T> {

	protected static final String HANDLE = "handle";

	protected OOCSIClient client;
	protected String channelName;
	private Handler handler;
	private Map<String, SystemFilter<?>> filters = new HashMap<String, SystemFilter<?>>();

	public OOCSISystemCommunicator(OOCSIClient client, String channelName) {
		this(client, channelName, null);
	}

	public OOCSISystemCommunicator(OOCSIClient client, String channelName, Handler handler) {
		this.client = client;
		this.channelName = channelName;
		this.handler = handler;
	}

	/**
	 * subscribe and inject filter checks
	 * 
	 * @param handler
	 */
	protected void subscribe(final EventHandler handler) {
		client.subscribe(channelName, new EventHandler() {
			@Override
			public void receive(OOCSIEvent event) {
				boolean accept = true;
				for (String key : filters.keySet()) {
					accept &= filters.get(key).accept(event);

					if (!accept) {
						return;
					}
				}

				if (accept) {
					handler.receive(event);
				}
			}
		});
	}

	protected void message(String command, T data) {
		if (client != null) {
			message().data(command, data).send();
		}
	}

	protected void message(String command) {
		if (client != null) {
			message().data(command, "").send();
		}
	}

	protected OOCSIMessage message() {
		if (client != null) {
			OOCSIMessage oocsiMessage = new OOCSIMessage(client, channelName).data(HANDLE, getHandle());
			for (String filterKey : filters.keySet()) {
				oocsiMessage.data(filterKey, filters.get(filterKey));
			}
			return oocsiMessage;
		}

		return null;
	}

	public void addFilter(String dataKey, SystemFilter<?> filter) {
		filters.put(dataKey, filter);
	}

	public void updateFilter(String dataKey, Object value) {
		filters.get(dataKey).value(value);
	}

	/**
	 * returns the unique handle for this object
	 * 
	 * @return
	 */
	protected String getHandle() {
		return client.getName() + "_" + OOCSISystemCommunicator.this.hashCode();
	}

	protected void triggerHandler() {
		if (handler != null) {
			handler.receive(channelName, Collections.<String, Object> emptyMap(), System.currentTimeMillis(),
					channelName, client.getName());
		}
	}

	abstract public class SystemFilter<K> {

		K value;

		public abstract boolean accept(OOCSIEvent event);

		@SuppressWarnings("unchecked")
		public void value(Object value) {
			this.value = (K) value;
		}

		public K value() {
			return value;
		}
	}
}
