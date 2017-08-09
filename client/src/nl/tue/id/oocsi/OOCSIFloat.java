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

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.tue.id.oocsi.client.data.OOCSIVariable#get()
	 */
	@Override
	public Float get() {
		if (super.get() == null) {
			return 0f;
		}
		return super.get();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.tue.id.oocsi.client.data.OOCSIVariable#filter(java.lang.Object)
	 */
	@Override
	protected Float filter(Float var) {
		// check boundaries
		if (min != null && var < min) {
			var = min;
		} else if (max != null && var > max) {
			var = max;
		}

		// return filtered value
		return super.filter(var);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.tue.id.oocsi.client.data.OOCSIVariable#adapt(java.lang.Object)
	 */
	@Override
	protected Float adapt(Float var) {
		// history processing?
		if (windowLength > 0 && values != null && values.size() > 0) {
			// check deviation from mean
			if (sigma != null) {
				// compute mean
				float sum = 0;
				for (Float v : values) {
					sum += v;
				}
				mean = (float) (sum / (float) values.size());

				float dev = Math.abs(mean - var);

				// return null if value outside sigma deviation from mean
				if (dev > sigma) {
					return mean - var > 0 ? mean - sigma / values.size() : mean + sigma / values.size();
				} else {
					return mean;
				}
			} else {
				// compute mean
				float sum = 0;
				for (Float v : values) {
					sum += v;
				}
				mean = (float) (sum / (float) values.size());
				return mean;
			}
		} else {
			return var;
		}
	}

	/**
	 * set the reference value (also possible during operation); supports chained invocation
	 * 
	 * @param reference
	 * @return
	 */
	@Override
	public OOCSIFloat reference(Float reference) {
		return (OOCSIFloat) super.reference(reference);
	}

	/**
	 * set the timeout in milliseconds (also possible during operation); supports chained invocation
	 * 
	 * @param timeoutMS
	 * @return
	 */
	@Override
	public OOCSIFloat timeout(int timeoutMS) {
		return (OOCSIFloat) super.timeout(timeoutMS);
	}

	/**
	 * set the limiting of incoming events in terms of <rate> and <seconds> timeframe; supports chained invocation
	 * 
	 * @param rate
	 * @param seconds
	 * @return
	 */
	public OOCSIFloat limit(int rate, int seconds) {
		super.limit(rate, seconds);

		return this;
	}

	/**
	 * set the minimum value for (lower-)bounded variable (also possible during operation); supports chained invocation
	 * 
	 * @param min
	 * @return
	 */
	@Override
	public OOCSIFloat min(Float min) {
		return (OOCSIFloat) super.min(min);
	}

	/**
	 * set the maximum value for (upper-)bounded variable (also possible during operation); supports chained invocation
	 * 
	 * @param max
	 * @return
	 */
	@Override
	public OOCSIFloat max(Float max) {
		return (OOCSIFloat) super.max(max);
	}

	/**
	 * set the length of the smoothing window, i.e., the buffer of historical values of this variable (also possible
	 * during operation, however, this will reset the buffer); supports chained invocation
	 * 
	 * @param windowLength
	 * @return
	 */

	@Override
	public OOCSIFloat smooth(int windowLength) {
		return (OOCSIFloat) super.smooth(windowLength);
	}

	/**
	 * set the length of the smoothing window, i.e., the buffer of historical values of this variable (also possible
	 * during operation, however, this will reset the buffer); supports chained invocation. the parameter sigma sets the
	 * upper bound for the standard deviation protected variable
	 * 
	 * @param windowLength
	 * @param sigma
	 * @return
	 */
	@Override
	public OOCSIFloat smooth(int windowLength, Float sigma) {
		return (OOCSIFloat) super.smooth(windowLength, sigma);
	}

	/**
	 * creates a periodic feedback loop that feed either the last input value or the reference value into the variable
	 * (locally). If there is no reference value set, the former applies. The period is given in milliseconds.
	 * 
	 * @param periodMS
	 * @return
	 */
	@Override
	public OOCSIFloat generator(long periodMS) {
		return (OOCSIFloat) super.generator(periodMS);
	}

	/**
	 * creates a periodic feedback loop that feed either the last input value or the reference value into the variable
	 * (locally). If there is no reference value set, the former applies. The period is given in milliseconds. In
	 * addition, the new value of the variable is sent out to the channel <outputChannel> with the given key <outputKey>
	 * 
	 * @param periodMS
	 * @param outputChannel
	 * @param outputKey
	 * @return
	 */
	@Override
	public OOCSIFloat generator(long periodMS, String outputChannel, String outputKey) {
		return (OOCSIFloat) super.generator(periodMS, outputChannel, outputKey);
	}
}
