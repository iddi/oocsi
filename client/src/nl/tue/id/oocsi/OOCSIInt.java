package nl.tue.id.oocsi;

import nl.tue.id.oocsi.client.OOCSIClient;
import nl.tue.id.oocsi.client.data.OOCSIVariable;

/**
 * OOCSIInt is a system-level primitive that allows for automatic synchronizing of local variables (read and write) with
 * different OOCSI clients on the same channel. This realizes synchronization on a single data variable without
 * aggregation.
 *
 * @author matsfunk
 *
 */
public class OOCSIInt extends OOCSIVariable<Integer> {

	public OOCSIInt(OOCSIClient client, String channelName, String key) {
		super(client, channelName, key);
	}

	public OOCSIInt(OOCSIClient client, String channelName, String key, int referenceValue) {
		super(client, channelName, key, referenceValue);
	}

	public OOCSIInt(OOCSIClient client, String channelName, String key, int referenceValue, int timeout) {
		super(client, channelName, key, referenceValue, timeout);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.tue.id.oocsi.client.data.OOCSIVariable#get()
	 */
	@Override
	public Integer get() {
		if (super.get() == null) {
			return 0;
		}
		return super.get();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.tue.id.oocsi.client.data.OOCSIVariable#filter(java.lang.Object)
	 */
	@Override
	protected Integer filter(Integer var) {
		// check boundaries
		if (min != null && var < min) {
			var = min;
		} else if (max != null && var > max) {
			var = max;
		}

		// compute mean
		if (windowLength > 0 && values != null && values.size() > 0) {
			int sum = 0;
			for (Integer v : values) {
				sum += v;
			}
			mean = (int) (sum / (float) values.size());
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
	protected Integer adapt(Integer var) {
		// history processing?
		if (windowLength > 0 && values != null && values.size() > 0 && mean != null) {
			// check deviation from mean
			if (sigma != null) {
				int sum = Math.abs(mean - var);

				// return null if value outside sigma deviation from mean
				if (sum > sigma) {
					return mean - var > 0 ? mean - sigma / values.size() : mean + sigma / values.size();
				} else {
					return mean;
				}
			} else {
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
	public OOCSIInt reference(Integer reference) {
		return (OOCSIInt) super.reference(reference);
	}

	/**
	 * set the timeout in milliseconds (also possible during operation); supports chained invocation
	 * 
	 * @param timeoutMS
	 * @return
	 */
	@Override
	public OOCSIInt timeout(int timeoutMS) {
		return (OOCSIInt) super.timeout(timeoutMS);
	}

	/**
	 * set the limiting of incoming events in terms of <rate> and <seconds> timeframe; supports chained invocation
	 * 
	 * @param rate
	 * @param seconds
	 * @return
	 */
	public OOCSIInt limit(int rate, int seconds) {
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
	public OOCSIInt min(Integer min) {
		return (OOCSIInt) super.min(min);
	}

	/**
	 * set the maximum value for (upper-)bounded variable (also possible during operation); supports chained invocation
	 * 
	 * @param max
	 * @return
	 */
	@Override
	public OOCSIInt max(Integer max) {
		return (OOCSIInt) super.max(max);
	}

	/**
	 * set the length of the smoothing window, i.e., the buffer of historical values of this variable (also possible
	 * during operation, however, this will reset the buffer); supports chained invocation
	 * 
	 * @param windowLength
	 * @return
	 */
	@Override
	public OOCSIInt smooth(int windowLength) {
		return (OOCSIInt) super.smooth(windowLength);
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
	public OOCSIInt smooth(int windowLength, Integer sigma) {
		return (OOCSIInt) super.smooth(windowLength, sigma);
	}

	/**
	 * creates a periodic feedback loop that feed either the last input value or the reference value into the variable
	 * (locally). If there is no reference value set, the former applies. The period is given in milliseconds.
	 * 
	 * @param periodMS
	 * @return
	 */
	@Override
	public OOCSIInt generator(long periodMS) {
		return (OOCSIInt) super.generator(periodMS);
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
	public OOCSIInt generator(long periodMS, String outputChannel, String outputKey) {
		return (OOCSIInt) super.generator(periodMS, outputChannel, outputKey);
	}
}
