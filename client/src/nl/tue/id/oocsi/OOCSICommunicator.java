package nl.tue.id.oocsi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import nl.tue.id.oocsi.client.OOCSIClient;
import nl.tue.id.oocsi.client.protocol.EventHandler;
import nl.tue.id.oocsi.client.protocol.OOCSIMessage;
import nl.tue.id.oocsi.client.services.OOCSICall;
import nl.tue.id.oocsi.client.services.Responder;

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
	 * @see nl.tue.id.oocsi.client.OOCSIClient#connect()
	 */
	@Override
	public boolean connect() {

		// connect delegate
		boolean result = super.connect();

		// default subscribe
		if (!subscribe(name, name)) {
			if (!subscribe(name, "handleOOCSIEvent")) {
				log(" - no handlers found for receiving direct messages");
			}
		}

		return result;
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
	 * create a call for service method <callName>
	 * 
	 * @param callName
	 * @return
	 */
	public OOCSICall call(String callName) {
		return call(callName, 1000, 1);
	}

	/**
	 * create a call for service method <callName>
	 * 
	 * @param callName
	 * @param timeoutMS
	 * @return
	 */
	public OOCSICall call(String callName, int timeoutMS) {
		return call(callName, timeoutMS, 1);
	}

	/**
	 * create a call for service method <callName>
	 * 
	 * @param callName
	 * @param timeoutMS
	 * @param maxResponses
	 * @return
	 */
	public OOCSICall call(String callName, int timeoutMS, int maxResponses) {
		return new OOCSICall(this, callName, timeoutMS, maxResponses);
	}

	/**
	 * subscribe to channel <channelName> for handler method <channelName> in the parent class; the handler method will
	 * be called with an OOCSIEvent object upon occurrence of an event; will try 'handleOOCSIEvent' as a fall-back in
	 * case no matching handler method is found for <channelName>
	 * 
	 * @param channelName
	 * @return
	 */
	public boolean subscribe(String channelName) {
		return subscribe(channelName, channelName) ? true : subscribe(channelName, "handleOOCSIEvent");
	}

	/**
	 * subscribe to channel <channelName> for handler method in the parent class with the given name <handlerName>; the
	 * handler method will be called with an OOCSIEvent object upon occurrence of an event
	 * 
	 * @param channelName
	 * @param handlerName
	 * @return
	 */
	public boolean subscribe(String channelName, String handlerName) {

		// try simple event handler with OOCSIEvent parameter

		try {
			final Method handler = parent.getClass().getDeclaredMethod(handlerName, new Class[] { OOCSIEvent.class });
			subscribe(channelName, new EventHandler() {

				@Override
				public void receive(OOCSIEvent event) {
					try {
						handler.invoke(parent, new Object[] { event });
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						e.printStackTrace();
					}
				}
			});

			log(" - subscribed to " + channelName + " with event handler");

			return true;
		} catch (Exception e) {
			// not found, just return false
			if (!name.equals(channelName)) {
				log(" - no event handler for channel " + channelName);
			}

			return false;
		}

	}

	/**
	 * subscribe to channel <responderName> for handler method <responderName> in the parent class; the handler method
	 * will be called with an OOCSIEvent object and a response map object upon occurrence of an event; will try
	 * 'respondToOOCSIEvent' as a fall-back in case no matching handler method is found for <responderName>
	 * 
	 * @param responderName
	 * @return
	 */
	public boolean register(String responderName) {
		return register(responderName, responderName) ? true : register(responderName, "respondToOOCSIEvent");
	}

	/**
	 * register a handler method in the parent class with the given name <handlerName> for the channel <channelName>;
	 * the handler method will be called with an OOCSIEvent object upon occurrence of an event
	 * 
	 * @param responderName
	 * @param handlerName
	 * @return
	 */
	public boolean register(String responderName, String handlerName) {

		// try responder event handler with OOCSIEvent and response map parameters

		try {
			final Method handler = parent.getClass().getDeclaredMethod(handlerName,
					new Class[] { OOCSIEvent.class, Map.class });
			subscribe(responderName, new Responder(this, responderName) {

				@Override
				public void respond(OOCSIEvent event, java.util.Map<String, Object> response) {
					try {
						handler.invoke(parent, new Object[] { event, response });
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						e.printStackTrace();
					}
				}
			});

			log(" - registered " + responderName + " as call responder");

			return true;
		} catch (Exception e) {
			// not found, just return false
			if (!name.equals(responderName)) {
				log(" - no call responders found for channel " + responderName);
			}

			return false;
		}

	}
}
