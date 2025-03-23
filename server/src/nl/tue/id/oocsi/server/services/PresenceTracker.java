package nl.tue.id.oocsi.server.services;

import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import nl.tue.id.oocsi.server.model.Channel;
import nl.tue.id.oocsi.server.model.Channel.ChangeListener;
import nl.tue.id.oocsi.server.model.Client;
import nl.tue.id.oocsi.server.model.Server;
import nl.tue.id.oocsi.server.protocol.Message;

public class PresenceTracker implements ChangeListener {

	private static final String CREATED = "created";
	private static final String CLOSED = "closed";
	private static final String JOIN = "join";
	private static final String REFRESH = "refresh";
	private static final String LEAVE = "leave";
	private static final String TIMEOUT = "timeout";

	// mapping from tracked to tracking channel
	private final ConcurrentMap<String, Channel> presenceTracking = new ConcurrentHashMap<String, Channel>();
	private final Server server;

	public PresenceTracker(Server server) {
		this.server = server;
	}

	/**
	 * subscribe a listening client <code>subscriber</code> for presence on channel <code>presenceHostChannel</code>
	 * 
	 * @param trackedChannel
	 * @param subscriber
	 */
	public void subscribe(String trackedChannelStr, Channel subscriber) {
		presenceTracking.putIfAbsent(trackedChannelStr,
		        new Channel("presence(" + trackedChannelStr + ")", NULL_LISTENER));
		presenceTracking.get(trackedChannelStr).addChannel(subscriber);
	}

	/**
	 * unsubscribe a listening <code>subscriber</code> from tracking presence on channel
	 * <code>presenceHostChannel</code>
	 * 
	 * @param trackedChannelStr
	 * @param subscriber
	 */
	public void unsubscribe(String trackedChannelStr, Channel subscriber) {
		if (presenceTracking.containsKey(trackedChannelStr)) {
			presenceTracking.get(trackedChannelStr).removeChannel(subscriber);
		}
	}

	/**
	 * remove a subscribed client <code>subscriber</code> from all presence on any channel it might be subscribed to
	 * 
	 * @param subscriber
	 */
	public void remove(Channel subscriber) {
		for (Channel c : presenceTracking.values()) {
			c.removeChannel(subscriber, true);
		}
	}

	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override
	public synchronized void created(Channel trackedChannel) {
		String trackedChannelStr = trackedChannel.getName();
		Channel tracker = presenceTracking.get(trackedChannelStr);
		if (tracker != null) {
			tracker.send(new Message(trackedChannel.getName(), "presence(" + trackedChannel.getName() + ")")
			        .addData(trackedChannel instanceof Client ? "client" : "channel", trackedChannel.getName())
			        .addData(CREATED, ""));
		}
	}

	@Override
	public synchronized void closed(Channel trackedChannel) {
		String trackedChannelStr = trackedChannel.getName();
		Channel tracker = presenceTracking.get(trackedChannelStr);
		if (tracker != null) {
			tracker.send(new Message(trackedChannel.getName(), "presence(" + trackedChannel.getName() + ")")
			        .addData(trackedChannel instanceof Client ? "client" : "channel", trackedChannel.getName())
			        .addData(CLOSED, ""));
		}
	}

	@Override
	public synchronized void join(Channel trackedChannel, Channel guest) {
		String trackedChannelStr = trackedChannel.getName();
		Channel listeners = presenceTracking.get(trackedChannelStr);
		if (listeners != null) {
			listeners.send(new Message(trackedChannel.getName(), "presence(" + trackedChannel.getName() + ")")
			        .addData(trackedChannel instanceof Client ? "client" : "channel", trackedChannel.getName())
			        .addData(JOIN, guest.getName()));
		}
	}

	@Override
	public synchronized void refresh() {
		presenceTracking.entrySet().stream().forEach(e -> {
			Channel tracker = e.getValue();
			String trackedChannelStr = e.getKey();
			Channel trackedChannel = server.getChannel(trackedChannelStr);

			// then send out a refresh presence notice on the respective channel
			tracker.send(new Message(trackedChannelStr, "presence(" + trackedChannelStr + ")")
			        .addData(trackedChannel != null && trackedChannel instanceof Client ? "client" : "channel",
			                trackedChannelStr)
			        .addData(REFRESH,
			                trackedChannel == null ? Collections.EMPTY_LIST
			                        : trackedChannel.getChannels().stream().map(channel -> channel.getName())
			                                .collect(Collectors.toList())));
		});
	}

	@Override
	public synchronized void leave(Channel trackedChannel, Channel guest) {
		String trackedChannelStr = trackedChannel.getName();
		Channel tracker = presenceTracking.get(trackedChannelStr);
		if (tracker != null) {
			tracker.send(new Message(trackedChannelStr, "presence(" + trackedChannelStr + ")")
			        .addData(trackedChannel instanceof Client ? "client" : "channel", trackedChannelStr)
			        .addData(LEAVE, guest.getName()));
		}
	}

	@Override
	public synchronized void timeout(Channel subscriber) {
		presenceTracking.entrySet().stream().forEach(e -> {
			Channel tracker = e.getValue();
			// remove subscriber from all presence tracking channels; do this manually to avoid problems with the
			// presence tracking (on top of presence tracking)
			if (tracker.getChannels().remove(subscriber) && !subscriber.isPrivate()) {
				// if the subscriber was found and removed AND is non-private
				// then send out a timeout presence notice on the channel that
				// subscriber was removed from
				String trackedChannelStr = e.getKey();
				Channel trackedChannel = server.getChannel(trackedChannelStr);
				tracker.send(new Message(trackedChannelStr, "presence(" + trackedChannelStr + ")")
				        .addData(trackedChannel instanceof Client ? "client" : "channel", trackedChannelStr)
				        .addData(TIMEOUT, subscriber.getName()));
			}
		});
	}

	/**
	 * listener implementation that will not listen (for special (meta)channels)
	 * 
	 */
	public final static ChangeListener NULL_LISTENER = new ChangeListener() {

		@Override
		public void created(Channel trackedChannel) {
		}

		@Override
		public void closed(Channel trackedChannel) {
		}

		@Override
		public void join(Channel trackedChannel, Channel guest) {
		}

		@Override
		public void refresh() {
		}

		@Override
		public void leave(Channel trackedChannel, Channel guest) {
		}

		@Override
		public void timeout(Channel subscriber) {

		}
	};

}
