package nl.tue.id.oocsi.client.data;

import nl.tue.id.oocsi.OOCSIEvent;
import nl.tue.id.oocsi.client.OOCSIClient;
import nl.tue.id.oocsi.client.behavior.OOCSISystemCommunicator;

/**
 * OOCSIVariable is a system-level primitive that allows for automatic synchronizing of local variables (read and write)
 * with different OOCSI clients on the same channel. This realizes synchronization on a single data variable without
 * aggregation. OOCSIVariable is a parametrized class, that means, once the data type is set it can reliably used in
 * reading and writing.
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
		this(client, channelName, key, referenceValue, 2000);
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

		// connect to channel
		client.subscribe(channelName, new nl.tue.id.oocsi.client.protocol.EventHandler() {

			@SuppressWarnings("unchecked")
			@Override
			public void receive(OOCSIEvent event) {
				Object object = event.getObject(OOCSIVariable.this.key);
				if (object != null) {
					try {
						internalVariable = (T) object;

						// update timeout
						lastWrite = System.currentTimeMillis();
					} catch (Exception e) {
						// do nothing
					}
				}
			}
		});
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
	 * set the variable and let the channel know
	 * 
	 * @param var
	 */
	public void set(T var) {
		this.internalVariable = var;
		message(key, internalVariable);
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
}
