package nl.tue.id.oocsi;

import nl.tue.id.oocsi.client.OOCSIClient;
import nl.tue.id.oocsi.client.data.OOCSIVariable;

/**
 * OOCSIString is a system-level primitive that allows for automatic synchronizing of local variables (read and write)
 * with different OOCSI clients on the same channel. This realizes synchronization on a single data variable without
 * aggregation.
 *
 * @author matsfunk
 *
 */
public class OOCSIString extends OOCSIVariable<String> {

	public OOCSIString(OOCSIClient client, String channelName, String key) {
		super(client, channelName, key);
	}

	public OOCSIString(OOCSIClient client, String channelName, String key, String referenceValue) {
		super(client, channelName, key, referenceValue);
	}

	public OOCSIString(OOCSIClient client, String channelName, String key, String referenceValue, int timeout) {
		super(client, channelName, key, referenceValue, timeout);
	}

	@Override
	protected String extractValue(OOCSIEvent event, String key) {
		return event.getString(key);
	}

	/**
	 * set the limiting of incoming events in terms of "rate" and "seconds" timeframe; supports chained invocation
	 * 
	 * @param rate
	 * @param seconds
	 * @return
	 */
	public OOCSIString limit(int rate, int seconds) {
		super.limit(rate, seconds);

		return this;
	}

}
