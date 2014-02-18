package nl.tue.id.oocsi.server.model;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import nl.tue.id.oocsi.server.OOCSIServer;
import nl.tue.id.oocsi.server.protocol.Message;

/**
 * data structure for channel
 * 
 * @author mfunk
 * 
 */
public class Channel {

	protected String token;
	protected Map<String, Channel> subChannels = new ConcurrentHashMap<String, Channel>();

	public Channel(String token) {
		this.token = token;
	}

	/**
	 * get token of this channel
	 * 
	 * @return
	 */
	public String getName() {
		return token;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return token;
	}

	/**
	 * send message on this channel
	 * 
	 * @param message
	 */
	public void send(Message message) {
		// OOCSIServer.logEvent("Channel " + token + " received " + message);
		for (Channel subChannel : subChannels.values()) {
			if (!message.sender.equals(subChannel.getName())) {
				subChannel.send(message);
			}
		}
	}

	/**
	 * retrieve channel if existing
	 * 
	 * @param channelName
	 * @return
	 */
	public Channel getChannel(String channelName) {
		return subChannels.get(channelName);
	}

	/**
	 * list all channels
	 * 
	 * @return
	 */
	public String getChannels() {
		String result = "";
		for (Iterator<String> keys = subChannels.keySet().iterator(); keys.hasNext();) {
			String key = keys.next();
			result += key + (keys.hasNext() ? "," : "");
		}

		return result;
	}

	/**
	 * adds a channel if not existing
	 * 
	 * @param channel
	 */
	public void addChannel(Channel channel) {
		if (!token.equals(channel.getName()) && !subChannels.containsKey(channel.getName())) {
			subChannels.put(channel.getName(), channel);
			OOCSIServer.logConnection(token, channel.getName(), "added channel", new Date());
		}
	}

	/**
	 * removes a channel
	 * 
	 * @param channel
	 */
	public void removeChannel(Channel channel) {
		removeChannel(channel, false);
	}

	/**
	 * removes a channel (recursively) if existing
	 * 
	 * @param channel
	 * @param recursive
	 */
	public void removeChannel(Channel channel, boolean recursive) {
		if (subChannels.remove(channel.getName()) != null) {
			OOCSIServer.logConnection(token, channel.getName(), "removed channel", new Date());
		}

		if (recursive) {
			for (Channel subChannel : subChannels.values()) {
				subChannel.removeChannel(channel, recursive);
			}
		}
	}

	/**
	 * close unused channels recursively
	 * 
	 */
	protected void closeEmptyChannels() {
		for (Channel subChannel : subChannels.values()) {
			subChannel.closeEmptyChannels();

			// it is empty now, remove sub channel
			if (!(subChannel instanceof Client) && subChannel.subChannels.size() == 0) {
				subChannels.remove(subChannel.getName());
				OOCSIServer.logConnection(token, subChannel.getName(), "closed empty channel", new Date());
			}
		}
	}
}
