package nl.tue.id.oocsi.server.model;

import java.util.Collection;
import java.util.Date;
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

	protected final Date created = new Date();
	protected final ChangeListener presence;
	protected final Map<String, Channel> subChannels = new ConcurrentHashMap<String, Channel>();

	protected String token;

	public Channel(String token, ChangeListener changeListener) {
		this.token = token;
		this.presence = changeListener;
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
		OOCSIServer.logEvent(message.sender, message.recipient, message.data, message.timestamp);
		for (Channel subChannel : subChannels.values()) {
			// no echo in channels; use ECHO channel for that
			if (message.sender.equals(subChannel.getName())) {
				continue;
			}

			// send event
			subChannel.send(message);

			// log event
			if (!subChannel.isPrivate()) {
				if (!message.recipient.equals(subChannel.getName())) {
					if (!message.sender.equals("SERVER") && !message.recipient.equals("OOCSI_events")) {
						OOCSIServer.logEvent(message.recipient, subChannel.getName(), message.data, message.timestamp);
					}
				}
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
		Channel channel = subChannels.get(channelName.replaceFirst(":.*", ""));
		return channel != null && channel.accept(channelName) ? channel : null;
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
		for (Channel channel : subChannels.values()) {
			if (!channel.isPrivate()) {
				result += channel.getName() + ",";
			}
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
				presence.join(this, channel);
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
				presence.leave(this.getName(), channel.getName());
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

	public static interface ChangeListener {
		public void created(Channel host);

		public void closed(Channel host);

		public void join(Channel host, Channel guest);

		public void refresh(Channel host, Channel guest);

		public void leave(String host, String guest);

		public void timeout(String host, String guest);

	}
}
