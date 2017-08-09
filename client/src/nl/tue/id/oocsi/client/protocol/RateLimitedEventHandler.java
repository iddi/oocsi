package nl.tue.id.oocsi.client.protocol;

import java.util.Map;

/**
 * rate limited event handler for events with structured data that will only let through <rate> events per <second>
 * secs; this counts for all incoming events
 *
 * @author matsfunk
 */
abstract public class RateLimitedEventHandler extends EventHandler {

	// configuration
	private int rate = 0;
	private int seconds = 0;

	// dynamic variables
	private long timestamp = 0;
	private int counter = 0;

	/**
	 * creates a rate limited event handler that will at most let through <rate> event per <second> secs
	 * 
	 * @param rate
	 * @param seconds
	 */
	public RateLimitedEventHandler(int rate, int seconds) {
		this.rate = rate;
		this.seconds = seconds;
		this.timestamp = System.currentTimeMillis();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.tue.id.oocsi.client.protocol.EventHandler#receive(java.lang.String, java.util.Map, long,
	 * java.lang.String, java.lang.String)
	 */
	@Override
	public void receive(String sender, Map<String, Object> data, long timestamp, String channel,
			final String recipient) {
		final long currentTimeMillis = System.currentTimeMillis();

		// if timeout has passed
		if (this.timestamp + seconds * 1000l < currentTimeMillis) {
			this.timestamp = currentTimeMillis;
			this.counter = 1;
			super.receive(sender, data, timestamp, channel, recipient);
		}
		// if timeout has not passed = rate limit check necessary
		else {
			if (++counter <= rate) {
				super.receive(sender, data, timestamp, channel, recipient);
			} else {
				// rate limit exceeded, no forwarding
				exceeded(sender, data, timestamp, channel, recipient);
			}
		}
	}

	public void exceeded(String sender, Map<String, Object> data, long timestamp, String channel, String recipient) {
		// do nothing
	}
}
