package nl.tue.id.oocsi;

import nl.tue.id.oocsi.client.OOCSIClient;
import nl.tue.id.oocsi.client.data.OOCSIVariable;

/**
 * OOCSIFloat is a system-level primitive that allows for automatic synchronizing of local variables (read and write)
 * with different OOCSI clients on the same channel. This realizes synchronization on a single data variable without
 * aggregation.
 *
 * @author matsfunk
 *
 */
public class OOCSIFloat extends OOCSIVariable<Float> {

	public OOCSIFloat(OOCSIClient client, String channelName, String key) {
		super(client, channelName, key);
	}

	public OOCSIFloat(OOCSIClient client, String channelName, String key, float referenceValue) {
		super(client, channelName, key, referenceValue);
	}

	public OOCSIFloat(OOCSIClient client, String channelName, String key, float referenceValue, int timeout) {
		super(client, channelName, key, referenceValue, timeout);
	}

}
