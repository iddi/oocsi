package nl.tue.id.oocsi.client.protocol;

import java.util.HashMap;
import java.util.Map;

/**
 * rate limited event handler for events with structured data that will only let through <rate> events per <second>
 * secs; this counts for all incoming events per sender which protects against single senders overloading the system
 *
 * @author matsfunk
 */
abstract public class RateLimitedClientEventHandler extends EventHandler {

	// configuration
	private int rate = 0;
	private int seconds = 0;

	// dynamic variables
	private long timestamp = 0;
	private Map<String, Integer> counter = new HashMap<String, Integer>(100);

	/**
	 * creates a rate limited event handler that will at most let through <rate> event per <second> secs
	 * 
	 * @param rate
	 * @param seconds
	 */
	public RateLimitedClientEventHandler(int rate, int seconds) {
		this.rate = rate;
		this.seconds = seconds;
		this.timestamp = System.currentTimeMillis();
	}

	@Override
	public void send(String sender, String data, String timestamp, String channel, String recipient) {
		final long currentTimeMillis = System.currentTimeMillis();

		// if timeout has passed
		if (this.timestamp + seconds * 1000l < currentTimeMillis) {
			this.timestamp = currentTimeMillis;
			counter.clear();
			inc(sender);
			super.send(sender, data, timestamp, channel, recipient);
		}
		// if timeout has not passed = rate limit check necessary
		else {
			if (inc(sender) <= rate) {
				System.out.println(sender);
				super.send(sender, data, timestamp, channel, recipient);
			} else {
				// rate limit exceeded, no forwarding
			}
		}
	}

	private int inc(String sender) {
		int count = 1;
		if (counter.containsKey(sender)) {
			count += counter.get(sender);
		}
		counter.put(sender, count);

		return count;
	}
}
