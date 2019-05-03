package nl.tue.id.oocsi.server.services;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import nl.tue.id.oocsi.server.model.Channel;
import nl.tue.id.oocsi.server.model.Channel.ChangeListener;
import nl.tue.id.oocsi.server.protocol.Message;

public class PresenceTracker implements ChangeListener {

	private static final String JOIN = "join";
	private static final String REFRESH = "refresh";
	private static final String LEAVE = "leave";
	private static final String TIMEOUT = "timeout";
	protected ConcurrentMap<String, Channel> presenceTracking = new ConcurrentHashMap<String, Channel>();

	/**
	 * add a listening client <code>subscriber</code> for presence on channel <code>presenceChannelName</code>
	 * 
	 * @param presenceChannelName
	 * @param subscriber
	 */
	public void add(String presenceChannelName, Channel subscriber) {
		presenceTracking.putIfAbsent(presenceChannelName,
				new Channel("presence(" + presenceChannelName + ")", nullListener));
		presenceTracking.get(presenceChannelName).addChannel(subscriber);
	}

	/**
	 * remove a listening client <code>channel</code> from all presence on any channel it might be subscribed to
	 * 
	 * @param channel
	 */
	public void remove(Channel channel) {
		for (Channel c : presenceTracking.values()) {
			c.removeChannel(channel, true);
		}
	}

	@Override
	public void created(Channel host) {
		// tbd.
	}

	@Override
	public void closed(Channel host) {
		// tbd.
	}

	@Override
	public void join(Channel host, Channel guest) {
		Channel listeners = presenceTracking.get(host.getName());
		if (listeners != null) {
			if (host != guest) {
				listeners.send(new Message(host.getName(), "presence(" + host.getName() + ")")
						.addData("channel", host.getName()).addData(JOIN, guest.getName()));
			} else {
				listeners.send(new Message(host.getName(), "presence(" + host.getName() + ")")
						.addData("client", host.getName()).addData(JOIN, guest.getName()));
			}
		}
	}

	@Override
	public void refresh(Channel host, Channel guest) {
		Channel listeners = presenceTracking.get(host.getName());
		if (listeners != null) {
			if (host != guest) {
				listeners.send(new Message(host.getName(), "presence(" + host.getName() + ")")
						.addData("channel", host.getName()).addData(REFRESH, guest.getName()));
			} else {
				listeners.send(new Message(host.getName(), "presence(" + host.getName() + ")")
						.addData("client", host.getName()).addData(REFRESH, guest.getName()));
			}
		}
	}

	@Override
	public void leave(String host, String guest) {
		Channel listeners = presenceTracking.get(host);
		if (listeners != null) {
			if (!host.equals(guest)) {
				listeners.send(
						new Message(host, "presence(" + host + ")").addData("channel", host).addData(LEAVE, guest));
			} else {
				listeners.send(
						new Message(host, "presence(" + host + ")").addData("client", guest).addData(LEAVE, guest));
			}
		}
	}

	@Override
	public void timeout(String host, String guest) {
		Channel listeners = presenceTracking.get(host);
		if (listeners != null) {
			if (!host.equals(guest)) {
				listeners.send(
						new Message(host, "presence(" + host + ")").addData("channel", host).addData(TIMEOUT, guest));
			} else {
				listeners.send(
						new Message(host, "presence(" + host + ")").addData("client", guest).addData(TIMEOUT, guest));
			}
		}
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
		public void leave(String host, String guest) {
		}

		@Override
		public void join(Channel host, Channel guest) {
		}

		@Override
		public void refresh(Channel host, Channel guest) {
		}

		@Override
		public void closed(Channel host) {
		}

		@Override
		public void timeout(String host, String guest) {

		}
	};

}
