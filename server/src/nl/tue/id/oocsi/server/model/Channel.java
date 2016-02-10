package nl.tue.id.oocsi.server.model;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import nl.tue.id.oocsi.server.OOCSIServer;
import nl.tue.id.oocsi.server.protocol.Message;

/**
 * data structure for channel
 * 
 * @author matsfunk
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
	 * 
	 */
	public String getName() {
		return token.replaceFirst(":.*", "");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return token.replaceFirst(":.*", "");
	}

	/**
	 * check whether this channel is private
	 * 
	 * @return
	 */
	public boolean isPrivate() {
		return token.contains(":");
	}

	/**
	 * check whether the given channelToken (with secret part) matches this channel's token (and its secret part)
	 * 
	 * @param channelToken
	 * @return
	 */
	public boolean validate(String channelToken) {
		return token.equals(channelToken);
	}

	/**
	 * check whether this channel accepts messages for recipient channelToken
	 * 
	 * @param channelToken
	 * @return
	 */
	public boolean accept(String channelToken) {
		return validate(channelToken);
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
	 * retrieve sub-channel if existing
	 * 
	 * @param channelName
	 * @return
	 */
	public Channel getChannel(String channelName) {
		return subChannels.get(channelName.replaceFirst(":.*", ""));
	}

	/**
	 * list all channels
	 * 
	 * @return
	 */
	public Collection<Channel> getChannels() {
		return subChannels.values();
	}

	/**
	 * list all channels as comma-separated String list
	 * 
	 * @return
	 */
	public String getChannelList() {
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
		if (!getName().equals(channel.getName()) && !subChannels.containsKey(channel.getName())) {
			subChannels.put(channel.getName(), channel);
			if (!channel.isPrivate()) {
				OOCSIServer.logConnection(getName(), channel.getName(), "added channel", new Date());
			}
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
			if (!channel.isPrivate()) {
				OOCSIServer.logConnection(getName(), channel.getName(), "removed channel", new Date());
			}
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
			if (subChannel != this) {
				subChannel.closeEmptyChannels();
			}

			// it is empty now, remove sub channel
			if (!(subChannel instanceof Client) && subChannel.subChannels.size() == 0) {
				subChannels.remove(subChannel.getName());
				if (!subChannel.isPrivate()) {
					OOCSIServer.logConnection(getName(), subChannel.getName(), "closed empty channel", new Date());
				}
			}
		}
	}
}
