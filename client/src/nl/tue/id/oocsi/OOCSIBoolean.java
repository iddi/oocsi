package nl.tue.id.oocsi;

import nl.tue.id.oocsi.client.OOCSIClient;
import nl.tue.id.oocsi.client.data.OOCSIVariable;

/**
 * OOCSIBoolean is a system-level primitive that allows for automatic synchronizing of local variables (read and write)
 * with different OOCSI clients on the same channel. This realizes synchronization on a single data variable without
 * aggregation.
 *
 * @author matsfunk
 *
 */
public class OOCSIBoolean extends OOCSIVariable<Boolean> {

	public OOCSIBoolean(OOCSIClient client, String channelName, String key) {
		super(client, channelName, key);
	}

	public OOCSIBoolean(OOCSIClient client, String channelName, String key, boolean referenceValue) {
		super(client, channelName, key, referenceValue);
	}

	public OOCSIBoolean(OOCSIClient client, String channelName, String key, boolean referenceValue, int timeout) {
		super(client, channelName, key, referenceValue, timeout);
	}

	public OOCSIBoolean(boolean referenceValue, int timeout) {
		super(referenceValue, timeout);
	}

	/**
	 * set the limiting of incoming events in terms of "rate" and "seconds" timeframe; supports chained invocation
	 * 
	 * @param rate
	 * @param seconds
	 * @return
	 */
	public OOCSIBoolean limit(int rate, int seconds) {
		super.limit(rate, seconds);

		return this;
	}
}
