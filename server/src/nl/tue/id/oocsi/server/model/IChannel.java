package nl.tue.id.oocsi.server.model;

import nl.tue.id.oocsi.server.protocol.Message;

public interface IChannel {

	/**
	 * send message on this channel
	 * 
	 * @param message
	 */
	void send(Message message);

	/**
	 * check whether this channel is private
	 * 
	 * @return
	 */
	boolean isPrivate();

	/**
	 * get token of this channel
	 * 
	 * @return
	 * 
	 */
	String getName();

}
