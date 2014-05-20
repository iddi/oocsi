package nl.tue.id.oocsi.client;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import nl.tue.id.oocsi.client.protocol.DataHandler;
import nl.tue.id.oocsi.client.protocol.OOCSIEvent;
import nl.tue.id.oocsi.client.protocol.OOCSIMessage;

/**
 * communication interface for OOCSI client
 * 
 * @author matsfunk
 */
public class OOCSICommunicator extends OOCSIClient {

	private Object parent;
	private String name;

	/**
	 * constructor
	 * 
	 * @param parent
	 * @param name
	 */
	public OOCSICommunicator(Object parent, String name) {
		super(name);

		this.parent = parent;
		this.name = name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.tue.id.oocsi.client.OOCSIClient#connect(java.lang.String, int)
	 */
	@Override
	public boolean connect(String hostname, int port) {

		// connect delegate
		boolean result = super.connect(hostname, port);

		// default subscribe
		if (!subscribe(name, name)) {
			if (!subscribe(name, "handleOOCSIEvent")) {
				log(" - no handlers found for receiving direct messages");
			}
		}

		return result;
	}

	/**
	 * send data through a channel given by the channelName
	 * 
	 * @param channelName
	 * @return
	 */
	public OOCSIMessage channel(String channelName) {
		return new OOCSIMessage(this, channelName);
	}

	/**
	 * subscribe to channel <name> for handler with the given name <handlerName>
	 * 
	 * @param channelName
	 * @param handlerName
	 * @return
	 */
	public boolean subscribe(String channelName, String handlerName) {
		try {
			final Method handler = parent.getClass().getDeclaredMethod(handlerName, new Class[] { OOCSIEvent.class });
			subscribe(channelName, new DataHandler() {

				@Override
				public void receive(String channelName, Map<String, Object> data, String sender) {
					try {
						handler.invoke(parent, new Object[] { new OOCSIEvent(channelName, data, sender) });
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						e.printStackTrace();
					}
				}
			});

			log(" - subscribed to " + channelName);

			return true;
		} catch (Exception e) {
			// not found, just return false
			if (!name.equals(channelName)) {
				log(" - no subscribe handlers found for channel " + channelName);
			}

			return false;
		}
	}
}
