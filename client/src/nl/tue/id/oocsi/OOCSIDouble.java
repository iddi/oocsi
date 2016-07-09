package nl.tue.id.oocsi;

import nl.tue.id.oocsi.client.OOCSIClient;
import nl.tue.id.oocsi.client.data.OOCSIVariable;

/**
 * OOCSIDouble is a system-level primitive that allows for automatic synchronizing of local variables (read and write)
 * with different OOCSI clients on the same channel. This realizes synchronization on a single data variable without
 * aggregation.
 *
 * @author matsfunk
 *
 */
public class OOCSIDouble extends OOCSIVariable<Double> {

	public OOCSIDouble(OOCSIClient client, String channelName, String key) {
		super(client, channelName, key);
	}

	public OOCSIDouble(OOCSIClient client, String channelName, String key, double referenceValue) {
		super(client, channelName, key, referenceValue);
	}

	public OOCSIDouble(OOCSIClient client, String channelName, String key, double referenceValue, int timeout) {
		super(client, channelName, key, referenceValue, timeout);
	}

}
