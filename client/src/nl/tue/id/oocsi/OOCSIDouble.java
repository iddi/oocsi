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

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.tue.id.oocsi.client.data.OOCSIVariable#get()
	 */
	@Override
	public Double get() {
		if (super.get() == null) {
			return 0d;
		}
		return super.get();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.tue.id.oocsi.client.data.OOCSIVariable#filter(java.lang.Object)
	 */
	@Override
	protected Double filter(Double var) {
		// check boundaries
		if (min != null && var < min) {
			var = min;
		} else if (max != null && var > max) {
			var = max;
		}

		// compute mean
		if (windowLength > 0 && values != null && values.size() > 0) {
			double sum = 0;
			for (Double v : values) {
				sum += v;
			}
			mean = (double) (sum / (float) values.size());
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
	protected Double adapt(Double var) {
		// history processing?
		if (windowLength > 0 && values != null && values.size() > 0) {
			// check deviation from mean
			if (sigma != null) {
				double sum = Math.abs(mean - var);

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
	public OOCSIDouble reference(Double reference) {
		return (OOCSIDouble) super.reference(reference);
	}

	/**
	 * set the timeout in milliseconds (also possible during operation); supports chained invocation
	 * 
	 * @param timeoutMS
	 * @return
	 */
	@Override
	public OOCSIDouble timeout(int timeoutMS) {
		return (OOCSIDouble) super.timeout(timeoutMS);
	}

	/**
	 * set the limiting of incoming events in terms of <rate> and <seconds> timeframe; supports chained invocation
	 * 
	 * @param rate
	 * @param seconds
	 * @return
	 */
	public OOCSIDouble limit(int rate, int seconds) {
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
	public OOCSIDouble min(Double min) {
		return (OOCSIDouble) super.min(min);
	}

	/**
	 * set the maximum value for (upper-)bounded variable (also possible during operation); supports chained invocation
	 * 
	 * @param max
	 * @return
	 */
	@Override
	public OOCSIDouble max(Double max) {
		return (OOCSIDouble) super.max(max);
	}

	/**
	 * set the length of the smoothing window, i.e., the buffer of historical values of this variable (also possible
	 * during operation, however, this will reset the buffer); supports chained invocation
	 * 
	 * @param windowLength
	 * @return
	 */
	@Override
	public OOCSIDouble smooth(int windowLength) {
		return (OOCSIDouble) super.smooth(windowLength);
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
	public OOCSIDouble smooth(int windowLength, Double sigma) {
		return (OOCSIDouble) super.smooth(windowLength, sigma);
	}

	/**
	 * creates a periodic feedback loop that feed either the last input value or the reference value into the variable
	 * (locally). If there is no reference value set, the former applies. The period is given in milliseconds.
	 * 
	 * @param periodMS
	 * @return
	 */
	@Override
	public OOCSIDouble generator(long periodMS) {
		return (OOCSIDouble) super.generator(periodMS);
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
	public OOCSIDouble generator(long periodMS, String outputChannel, String outputKey) {
		return (OOCSIDouble) super.generator(periodMS, outputChannel, outputKey);
	}
}
