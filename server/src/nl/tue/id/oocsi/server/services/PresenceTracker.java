package nl.tue.id.oocsi.server.services;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import nl.tue.id.oocsi.server.model.Channel;
import nl.tue.id.oocsi.server.model.Channel.ChangeListener;
import nl.tue.id.oocsi.server.model.Client;
import nl.tue.id.oocsi.server.protocol.Message;

public class PresenceTracker implements ChangeListener {

	private static final String CREATED = "created";
	private static final String CLOSED = "closed";
	private static final String JOIN = "join";
	private static final String REFRESH = "refresh";
	private static final String LEAVE = "leave";
	private static final String TIMEOUT = "timeout";

	private final ConcurrentMap<String, Channel> presenceTracking = new ConcurrentHashMap<String, Channel>();
	private final ConcurrentMap<String, Long> presenceTimeout = new ConcurrentHashMap<String, Long>();

	/**
	 * subscribe a listening client <code>subscriber</code> for presence on channel <code>presenceHostChannel</code>
	 * 
	 * @param presenceHostChannel
	 * @param subscriber
	 */
	public void subscribe(String presenceHostChannel, Channel subscriber) {
		presenceTracking.putIfAbsent(presenceHostChannel,
		        new Channel("presence(" + presenceHostChannel + ")", nullListener));
		presenceTracking.get(presenceHostChannel).addChannel(subscriber);
	}

	/**
	 * unsubscribe a listening <code>subscriber</code> from tracking presence on channel
	 * <code>presenceHostChannel</code>
	 * 
	 * @param presenceHostChannel
	 * @param subscriber
	 */
	public void unsubscribe(String presenceHostChannel, Channel subscriber) {
		if (presenceTracking.containsKey(presenceHostChannel)) {
			presenceTracking.get(presenceHostChannel).removeChannel(subscriber);
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
	public void created(Channel host) {
		Channel listeners = presenceTracking.get(host.getName());
		if (listeners != null) {
			listeners.send(new Message(host.getName(), "presence(" + host.getName() + ")")
			        .addData(host instanceof Client ? "client" : "channel", host.getName()).addData(CREATED, ""));
		}
	}

	@Override
	public void closed(Channel host) {
		Channel listeners = presenceTracking.get(host.getName());
		if (listeners != null) {
			listeners.send(new Message(host.getName(), "presence(" + host.getName() + ")")
			        .addData(host instanceof Client ? "client" : "channel", host.getName()).addData(CLOSED, ""));
		}
	}

	@Override
	public void join(Channel host, Channel guest) {
		Channel listeners = presenceTracking.get(host.getName());
		if (listeners != null) {
			listeners.send(new Message(host.getName(), "presence(" + host.getName() + ")")
			        .addData(host instanceof Client ? "client" : "channel", host.getName())
			        .addData(JOIN, guest.getName()));
		}
	}

	@Override
	public void refresh(Channel subscriber) {
		presenceTracking.entrySet().stream().forEach(e -> {
			Channel c = e.getValue();
			// remove subscriber from all presence tracking channels
			if (c.getChannels().contains(subscriber) && !subscriber.isPrivate()) {
				String host = e.getKey();
				long lastUpdate = presenceTimeout.getOrDefault(host + "_" + subscriber.getName(), -1L);
				// if the subscriber was found AND is non-private
				// then send out a refresh presence notice on the respective channel
				Channel listeners = presenceTracking.get(host);
				if (listeners != null && lastUpdate <= System.currentTimeMillis() - 9000) {
					listeners.send(new Message(host, "presence(" + host + ")").addData("client", subscriber.getName())
					        .addData(REFRESH, subscriber.getName()));

					// update timeout
					presenceTimeout.put(host + "_" + subscriber.getName(), System.currentTimeMillis());
				}
			}
		});
	}

	@Override
	public void leave(String host, String guest) {
		Channel listeners = presenceTracking.get(host);
		if (listeners != null) {
			listeners.send(new Message(host, "presence(" + host + ")").addData("client", host).addData(LEAVE, guest));
		}
	}

	@Override
	public void timeout(Channel subscriber) {
		presenceTracking.entrySet().stream().forEach(e -> {
			Channel c = e.getValue();
			// remove subscriber from all presence tracking channels
			if (c.getChannels().remove(subscriber) && !subscriber.isPrivate()) {
				// if the subscriber was found and removed AND is non-private
				// then send out a timeout presence notice on the channel that
				// subscriber was removed from
				String host = e.getKey();
				Channel listeners = presenceTracking.get(host);
				if (listeners != null) {
					listeners.send(new Message(host, "presence(" + host + ")").addData("client", host).addData(TIMEOUT,
					        subscriber.getName()));
				}
			}
		});
	}

	/**
	 * listener implementation that will not listen (for special (meta)channels)
	 * 
	 */
	public final static ChangeListener nullListener = new ChangeListener() {

		@Override
		public void created(Channel host) {
		}

		@Override
		public void closed(Channel host) {
		}

		@Override
		public void join(Channel host, Channel guest) {
		}

		@Override
		public void refresh(Channel subscriber) {
		}

		@Override
		public void leave(String host, String guest) {
		}

		@Override
		public void timeout(Channel subscriber) {

		}
	};

}
