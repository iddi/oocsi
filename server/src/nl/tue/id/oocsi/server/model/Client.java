package nl.tue.id.oocsi.server.model;

import nl.tue.id.oocsi.server.protocol.Message;

/**
 * data structure for client
 * 
 * @author matsfunk
 * 
 */
abstract public class Client extends Channel {

	protected enum ClientType {
		OOCSI, PD, JSON, OSC
	}

	protected long lastAction = System.currentTimeMillis();

	/**
	 * constructor
	 * 
	 * @param token
	 */
	public Client(String token, ChangeListener presence) {
		super(token, presence);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.tue.id.oocsi.server.model.Channel#send(nl.tue.id.oocsi.server.protocol .Message)
	 */
	abstract public void send(Message message);

	/**
	 * disconnect this client from server
	 */
	abstract public void disconnect();

	/**
	 * check if this client is still connected
	 * 
	 * @return
	 */
	abstract public boolean isConnected();

	/**
	 * ping the client to fill in the last action property, ultimately determining an inactive client
	 * 
	 */
	abstract public void ping();

	/**
	 * retrieves time stamp of last action from the connected client
	 * 
	 * @return
	 */
	public long lastAction() {
		return lastAction;
	}
}
