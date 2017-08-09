package nl.tue.id.oocsi.client.data;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;

import nl.tue.id.oocsi.OOCSIEvent;
import nl.tue.id.oocsi.client.OOCSIClient;
import nl.tue.id.oocsi.client.behavior.OOCSISystemCommunicator;
import nl.tue.id.oocsi.client.protocol.OOCSIMessage;

/**
 * OOCSIVariable is a system-level primitive that allows for automatic synchronizing of local variables (read and write)
 * with different OOCSI clients on the same channel. This realizes synchronization on a single data variable without
 * aggregation. OOCSIVariable is a parameterized class, that means, once the data type is set it can reliably used in
 * reading and writing.
 * 
 * OOCSIVariable supports internal aggregation in the form of smoothing (use smooth(n) for n sample window length). The
 * range of the variable can be restricted by using an upper or lower bound or both. When smoothing, a mean-deviation
 * parameter sigma can be added to filter out outliers in the input data.
 *
 * @author matsfunk
 *
 * @param <T>
 */
public class OOCSIVariable<T> extends OOCSISystemCommunicator<T> {

	// internal representation of the variable
	private T internalVariable;

	// key of internal variable in the OOCSI network
	private String key;

	// reference or default value for variable in case nothing has been received for <timeout> milliseconds
	private T internalReference;

	// amount of milliseconds to wait until the reference value is applied to <internalVariable>
	private int timeout;

	// last time the variable was updated
	private long lastWrite = System.currentTimeMillis();

	// bounded variable: min value
	protected T min = null;

	// bounded variable: max value
	protected T max = null;

	// protected variable: standard deviation upper bound
	protected T sigma = null;

	// protected variable: mean
	protected T mean = null;

	// historical window of past variable values
	protected ArrayBlockingQueue<T> values = null;

	// length of historical window of past variable values
	protected int windowLength = 0;

	// preserve the last input
	protected T lastInput = null;

	private nl.tue.id.oocsi.client.protocol.RateLimitedEventHandler eventHandler;

	/**
	 * Constructor for a simple OOCSI variable to sync on a given channel and key
	 * 
	 * @param client
	 * @param channelName
	 * @param key
	 */
	public OOCSIVariable(OOCSIClient client, String channelName, String key) {
		this(client, channelName, key, null, -1);
	}

	/**
	 * Constructor for a simple OOCSI variable to sync on a given channel and key, in case no value can be retrieved
	 * from the channel a reference value is provided which will be set automatically after a timeout of 2000 ms (2
	 * seconds)
	 * 
	 * @param client
	 * @param channelName
	 * @param key
	 * @param referenceValue
	 */
	public OOCSIVariable(OOCSIClient client, String channelName, String key, T referenceValue) {
		this(client, channelName, key, referenceValue, -1);
	}

	/**
	 * Constructor for a simple OOCSI variable to sync on a given channel and key, in case no value can be retrieved
	 * from the channel a reference value is provided which will be set automatically after the given timeout
	 * 
	 * @param client
	 * @param channelName
	 * @param key
	 * @param referenceValue
	 * @param timeout
	 */
	public OOCSIVariable(OOCSIClient client, String channelName, String key, T referenceValue, int timeout) {
		super(client, channelName);
		this.key = key;
		this.internalVariable = referenceValue;
		this.internalReference = referenceValue;
		this.timeout = timeout;

		this.eventHandler = new nl.tue.id.oocsi.client.protocol.RateLimitedEventHandler(0, 0) {

			@SuppressWarnings("unchecked")
			@Override
			public void receive(OOCSIEvent event) {
				Object object = event.getObject(OOCSIVariable.this.key);
				if (object != null) {
					try {
						// set variable from the network
						OOCSIVariable.this.internalSet((T) object);

						// update timeout
						lastWrite = System.currentTimeMillis();
					} catch (Exception e) {
						// do nothing
					}
				}
			}

			@Override
			public void exceeded(String sender, Map<String, Object> data, long timestamp, String channel,
					String recipient) {
				System.out.println(data.toString());
			}
		};

		// subscribe
		client.subscribe(channelName, eventHandler);
	}

	/**
	 * retrieve the current value of the variable (will check for expiration if a timeout is given; in this case the
	 * reference value is set)
	 * 
	 * @return
	 */
	public T get() {
		// check if current value is outdated and should be set to reference value
		if (timeout > -1 && internalReference != null && lastWrite < System.currentTimeMillis() - timeout) {
			internalVariable = internalReference;
		}

		return internalVariable;
	}

	/**
	 * set the variable and, if successful, let the channel know
	 * 
	 * @param var
	 */
	public void set(T var) {
		if (internalSet(var)) {
			if (values != null && values.size() > 0) {
				message(key, last());
			} else {
				message(key, internalVariable);
			}
		}
	}

	/**
	 * internally set the variable (return true), except in case it is filtered (return false)
	 * 
	 * @param var
	 * @return
	 */
	private boolean internalSet(T var) {

		lastInput = var;

		// filter dynamically
		var = filter(var);
		if (var == null) {
			return false;
		}

		lastInput = var;

		// add to history
		if (values != null) {
			// make space in history
			while (values.remainingCapacity() == 0) {
				values.poll();
			}
			// add to history
			values.offer(var);
		}

		// adapt and send out
		internalVariable = adapt(var);

		return true;
	}

	/**
	 * update the setting of the variable, but only if it is different from the reference value
	 * 
	 */
	public void update() {
		if (this.internalVariable != this.internalReference) {
			message(key, internalVariable);
		}
	}

	/**
	 * filter the newly entered variable setting (if variable type is numerical, this could be a min/max filter and
	 * additional filtering based on the standard deviation, or a different filter)
	 * 
	 * @param var
	 * @return
	 */
	protected T filter(T var) {
		return var;
	}

	/**
	 * adaptation of the variable based on the recent history (values) and the newly entered variable setting
	 * 
	 * @param var
	 * @return
	 */
	protected T adapt(T var) {
		return var;
	}

	/**
	 * retrieve the reference value
	 * 
	 * @return
	 */
	public T reference() {
		return internalReference;
	}

	/**
	 * retrieve the number of milliseconds that have passed since the last setting of the variable by another OOCSI
	 * client
	 * 
	 * @return
	 */
	public long fresh() {
		return System.currentTimeMillis() - lastWrite;
	}

	/**
	 * retrieve the last input value
	 * 
	 * @return
	 */
	public T last() {
		return lastInput;
	}

	/**
	 * set the reference value (also possible during operation); supports chained invocation
	 * 
	 * @param reference
	 * @return
	 */
	public OOCSIVariable<T> reference(T reference) {
		this.internalReference = reference;

		if (this.internalVariable == null) {
			this.internalVariable = reference;
		}

		return this;
	}

	/**
	 * set the timeout in milliseconds (also possible during operation); supports chained invocation
	 * 
	 * @param timeoutMS
	 * @return
	 */
	public OOCSIVariable<T> timeout(int timeoutMS) {
		this.timeout = timeoutMS;

		return this;
	}

	/**
	 * set the limiting of incoming events in terms of <rate> and <seconds> timeframe; supports chained invocation
	 * 
	 * @param rate
	 * @param seconds
	 * @return
	 */
	public OOCSIVariable<T> limit(int rate, int seconds) {
		eventHandler.limit(rate, seconds);

		return this;
	}

	/**
	 * set the minimum value for (lower-)bounded variable (also possible during operation); supports chained invocation
	 * 
	 * @param min
	 * @return
	 */
	public OOCSIVariable<T> min(T min) {
		this.min = min;

		return this;
	}

	/**
	 * set the maximum value for (upper-)bounded variable (also possible during operation); supports chained invocation
	 * 
	 * @param max
	 * @return
	 */
	public OOCSIVariable<T> max(T max) {
		this.max = max;

		return this;
	}

	/**
	 * set the length of the smoothing window, i.e., the buffer of historical values of this variable (also possible
	 * during operation, however, this will reset the buffer); supports chained invocation
	 * 
	 * @param windowLength
	 * @param sigma
	 * @return
	 */
	public OOCSIVariable<T> smooth(int windowLength) {
		return this.smooth(windowLength, null);
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
	public OOCSIVariable<T> smooth(int windowLength, T sigma) {
		this.windowLength = windowLength;
		this.sigma = sigma;
		this.values = new ArrayBlockingQueue<T>(windowLength);

		return this;
	}

	/**
	 * creates a periodic feedback loop that feed either the last input value or the reference value into the variable
	 * (locally). If there is no reference value set, the former applies. The period is given in milliseconds.
	 * 
	 * @param periodMS
	 * @return
	 */
	public OOCSIVariable<T> generator(long periodMS) {
		return this.generator(periodMS, null, null);
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
	public OOCSIVariable<T> generator(long periodMS, final String outputChannel, final String outputKey) {

		// start time to update this flow
		new Timer(true).schedule(new TimerTask() {
			public void run() {

				// update variable
				if (internalReference != null) {
					set(internalReference);
				} else if (last() != null) {
					set(last());
				}

				// send out
				if (outputChannel != null && outputChannel.trim().length() > 0 && outputKey != null
						&& outputKey.trim().length() > 0)
					new OOCSIMessage(client, outputChannel).data(outputKey, get()).send();

			}
		}, periodMS, periodMS);

		return this;
	}
}
