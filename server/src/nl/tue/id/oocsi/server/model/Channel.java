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
	protected Message retainedMessage;

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
		return token.contains(":") || token.contains("/?");
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
		for (Channel subChannel : subChannels.values()) {
			// no echo in channels; use ECHO channel for that
			if (message.getSender().equals(subChannel.getName())) {
				continue;
			}

			// send event
			subChannel.send(message);

			// log event unless channel is private
			if (!subChannel.isPrivate()) {
				if (!message.getRecipient().equals(subChannel.getName())) {
					OOCSIServer.logEvent(message.getSender(), message.getRecipient(), subChannel.getName(),
					        message.data, message.getTimestamp());
				}
			}
		}

		// new message erases always retained message
		retainedMessage = null;

		// check for retained message flag and store message
		if (message.data.containsKey(Message.RETAIN_MESSAGE)) {
			Object retainTimeoutRaw = message.data.getOrDefault(Message.RETAIN_MESSAGE, "0");
			try {
				// retrieve timeout and restrict timeout to 2 days
				long timeout = Long.parseLong(retainTimeoutRaw.toString());
				timeout = Math.min(3600 * 24 * 2, timeout);
				// set timeout and store retained message
				message.validUntil = new Date(System.currentTimeMillis() + timeout * 1000);
				retainedMessage = message;
				OOCSIServer.log("Retained message stored for channel '" + this.token + "' for " + timeout + "secs.");
			} catch (Exception e) {
				// do nothing
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
	 * check whether a channel is private
	 * 
	 * @param channelName
	 * @return
	 */
	public static boolean isPrivate(String channelName) {
		return !channelName.equals(channelName.replaceFirst(":.*", ""));
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
	 * @param newChannel
	 */
	public void addChannel(Channel newChannel) {

		// check whether a channel is added recursively
		if (!getName().equals(newChannel.getName()) && !subChannels.containsKey(newChannel.getName())) {
			subChannels.put(newChannel.getName(), newChannel);

			// update presence information only for public clients
			if (!newChannel.isPrivate()) {
				if (newChannel instanceof Client) {
					// signal to presence tracker that a client "channel" is created
					presence.created(newChannel);
				}

				// signal to presence tracker that a subchannel "channel" joins "this" channel
				presence.join(this, newChannel);
				OOCSIServer.logConnection(getName(), newChannel.getName(), "added channel", new Date());
			}

			// send out the retained message to new client
			final Message retainedMessageCopy = retainedMessage;
			if (retainedMessageCopy != null && retainedMessageCopy.isValid()) {
				newChannel.send(retainedMessageCopy);
			} else {
				// clear invalid or null message
				retainedMessage = null;
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

			// update presence information once for public clients
			if (!channel.isPrivate()) {
				// signal to presence tracker that a subchannel "channel" leaves "this" channel
				presence.leave(this.getName(), channel.getName());
				OOCSIServer.logConnection(getName(), channel.getName(), "removed channel", new Date());

				if (channel instanceof Client) {
					// signal to presence tracker that a client "channel" is closed
					presence.closed(channel);
				}
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
			if (!(subChannel instanceof Client) && subChannel.subChannels.size() == 0
			        && (subChannel.retainedMessage == null || !subChannel.retainedMessage.isValid())) {
				// update presence information once for public clients
				if (!subChannel.isPrivate()) {
					// signal to presence tracker that a subchannel "channel" leaves "this" channel
					presence.leave(this.getName(), subChannel.getName());
					subChannels.remove(subChannel.getName());

					OOCSIServer.logConnection(getName(), subChannel.getName(), "closed empty channel", new Date());
				} else {
					subChannels.remove(subChannel.getName());
				}
			}
		}
	}

	public static interface ChangeListener {
		public void created(Channel host);

		public void closed(Channel host);

		public void join(Channel host, Channel guest);

		public void refresh(Channel subscriber);

		public void leave(String host, String guest);

		public void timeout(Channel subscriber);

	}
}
