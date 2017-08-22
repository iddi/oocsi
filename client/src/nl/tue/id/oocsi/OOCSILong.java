package nl.tue.id.oocsi;

import nl.tue.id.oocsi.client.OOCSIClient;
import nl.tue.id.oocsi.client.data.OOCSIVariable;

/**
 * OOCSILong is a system-level primitive that allows for automatic synchronizing of local variables (read and write)
 * with different OOCSI clients on the same channel. This realizes synchronization on a single data variable without
 * aggregation.
 *
 * @author matsfunk
 *
 */
public class OOCSILong extends OOCSIVariable<Long> {

	public OOCSILong(OOCSIClient client, String channelName, String key) {
		super(client, channelName, key);
	}

	public OOCSILong(OOCSIClient client, String channelName, String key, long referenceValue) {
		super(client, channelName, key, referenceValue);
	}

	public OOCSILong(OOCSIClient client, String channelName, String key, long referenceValue, int timeout) {
		super(client, channelName, key, referenceValue, timeout);
	}

	public OOCSILong(long referenceValue, int timeout) {
		super(referenceValue, timeout);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.tue.id.oocsi.client.data.OOCSIVariable#get()
	 */
	@Override
	public Long get() {
		if (super.get() == null) {
			return 0l;
		}
		return super.get();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.tue.id.oocsi.client.data.OOCSIVariable#filter(java.lang.Object)
	 */
	@Override
	protected Long filter(Long var) {
		// check boundaries
		if (min != null && var < min) {
			var = min;
		} else if (max != null && var > max) {
			var = max;
		}

		// check mean and sigma
		if (sigma != null && mean != null) {
			// return null if value outside sigma deviation from mean
			if ((double) Math.abs(mean - var) > sigma) {
				var = (long) (mean - var > 0 ? mean - sigma / (float) values.size()
						: mean + sigma / (float) values.size());
			}
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
	protected Long adapt(Long var) {
		// history processing?
		if (windowLength > 0 && values != null && values.size() > 0) {
			// compute and store mean
			long sum = 0;
			for (Long v : values) {
				sum += v;
			}
			mean = (long) (sum / (float) values.size());

			return mean;
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
	public OOCSILong reference(Long reference) {
		return (OOCSILong) super.reference(reference);
	}

	/**
	 * set the timeout in milliseconds (also possible during operation); supports chained invocation
	 * 
	 * @param timeoutMS
	 * @return
	 */
	@Override
	public OOCSILong timeout(int timeoutMS) {
		return (OOCSILong) super.timeout(timeoutMS);
	}

	/**
	 * set the limiting of incoming events in terms of <rate> and <seconds> timeframe; supports chained invocation
	 * 
	 * @param rate
	 * @param seconds
	 * @return
	 */
	public OOCSILong limit(int rate, int seconds) {
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
	public OOCSILong min(Long min) {
		return (OOCSILong) super.min(min);
	}

	/**
	 * set the maximum value for (upper-)bounded variable (also possible during operation); supports chained invocation
	 * 
	 * @param max
	 * @return
	 */
	@Override
	public OOCSILong max(Long max) {
		return (OOCSILong) super.max(max);
	}

	/**
	 * set the length of the smoothing window, i.e., the buffer of historical values of this variable (also possible
	 * during operation, however, this will reset the buffer); supports chained invocation
	 * 
	 * @param windowLength
	 * @return
	 */
	@Override
	public OOCSILong smooth(int windowLength) {
		return (OOCSILong) super.smooth(windowLength);
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
	public OOCSILong smooth(int windowLength, Long sigma) {
		return (OOCSILong) super.smooth(windowLength, sigma);
	}

	/**
	 * creates a periodic feedback loop that feed either the last input value or the reference value into the variable
	 * (locally). If there is no reference value set, the former applies. The period is given in milliseconds.
	 * 
	 * @param periodMS
	 * @return
	 */
	@Override
	public OOCSILong generator(long periodMS) {
		return (OOCSILong) super.generator(periodMS);
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
	public OOCSILong generator(long periodMS, String outputChannel, String outputKey) {
		return (OOCSILong) super.generator(periodMS, outputChannel, outputKey);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.tue.id.oocsi.client.data.OOCSIVariable#connect(nl.tue.id.oocsi.client.data.OOCSIVariable)
	 */

	@Override
	public void connect(OOCSIVariable<Long> forward) {
		super.connect(forward);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.tue.id.oocsi.client.data.OOCSIVariable#disconnect(nl.tue.id.oocsi.client.data.OOCSIVariable)
	 */
	@Override
	public void disconnect(OOCSIVariable<Long> forward) {
		super.disconnect(forward);
	}

}
