package nl.tue.id.oocsi.client.behavior;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import nl.tue.id.oocsi.client.OOCSIClient;
import nl.tue.id.oocsi.client.protocol.Handler;

/**
 * OOCSISync is a system-level primitive that allows for easy synchronization between different OOCSI clients on the
 * same channel. Synchronization is on triggers, not on data, for which we have OOCSIVote.
 * 
 * @author matsfunk
 */
public class OOCSISync extends OOCSISystemCommunicator<Integer> {

	// call constants
	private static final String SYNC = "sync";
	private int PERIOD = 20;

	// infrastructure, configuration
	private int periodMS;

	// dynamics
	private Timer timer;
	private boolean isSynced = false;
	private int progress = 0;
	private int signalCount = 0;

	/**
	 * creates a synchronization process among OOCSI clients on the same channel with a default of 2 secs between
	 * pulses. starts automatically.
	 * 
	 * @param client
	 * @param channelName
	 */
	public OOCSISync(OOCSIClient client, String channelName) {
		this(client, channelName, 2000);
	}

	/**
	 * creates a synchronization process among OOCSI clients on the same channel with a given time between pulses.
	 * starts automatically.
	 * 
	 * @param client
	 * @param channelName
	 * @param periodMS
	 */
	public OOCSISync(OOCSIClient client, String channelName, int periodMS) {
		this(client, channelName, periodMS, null);
	}

	/**
	 * creates a synchronization process among OOCSI clients on the same channel with a given time between pulses and a
	 * callback to trigger at every pulse. starts automatically.
	 * 
	 * @param client
	 * @param channelName
	 * @param periodMS
	 * @param handler
	 */
	public OOCSISync(OOCSIClient client, String channelName, int periodMS, Handler handler) {
		super(client, channelName + "_sync", handler);
		this.periodMS = periodMS;

		start();
	}

	/**
	 * start the synchronization process
	 * 
	 */
	public void start() {

		// first subscribe to sync channel
		client.subscribe(channelName, new Handler() {

			@Override
			public void receive(String sender, Map<String, Object> data, long timestamp, String channel,
					String recipient) {

				// compare the synchronizing from others to my own progress in the periodic sync cycle
				if (progress < getPeriodFraction(0.05) || progress > getPeriodFraction(0.95)) {
					isSynced = true;
				} else if (progress < getPeriodFraction(0.6)) {
					progress -= getPeriodFraction(0.1);
					isSynced = false;
				} else if (progress > getPeriodFraction(0.4)) {
					progress += getPeriodFraction(0.05);
					isSynced = false;
				} else {
					progress -= getPeriodFraction(0.05);
					isSynced = false;
				}
				signalCount++;
			}
		});

		// send out sync signal myself periodically
		(timer = new Timer(true)).schedule(new TimerTask() {
			@Override
			public void run() {
				if (progress-- <= 0) {
					// send sync signal to channel
					if (Math.round(Math.random() * signalCount / 2) == 0) {
						message(SYNC);
					}
					signalCount = 1;

					// trigger pulse
					triggerHandler();

					// reset progress
					progress = PERIOD;
				}
			}
		}, (int) (periodMS / PERIOD), (int) (periodMS / PERIOD));
	}

	/**
	 * stop the synchronization process
	 * 
	 */
	public void stop() {
		// cancel the thread
		timer.cancel();
		timer = null;

		// unsubscribe from sync channel
		client.unsubscribe(channelName);

		// reset
		reset();

		// sync primitive is now essentially at startup state, can be started again later on
	}

	/**
	 * reset dynamic properties
	 * 
	 */
	public void reset() {
		progress = (int) PERIOD;
		isSynced = false;
	}

	/**
	 * return the cycle progress (an integer value from 0 - 19)
	 * 
	 * @return
	 */
	public int getProgress() {
		return progress;
	}

	/**
	 * returns the resolution of this synchronization process
	 * 
	 * @return
	 */
	public int getResolution() {
		return (int) PERIOD;
	}

	/**
	 * set the resolution of this synchronization process (20 works well, but 100 gives nicer output for using in
	 * visuals); rule of thumb: the smaller the resolution, the faster the synchornization
	 * 
	 * @param resolution
	 */
	public void setResolution(int resolution) {
		PERIOD = resolution;
	}

	/**
	 * check if this system process is running
	 * 
	 * @return
	 */
	public boolean isRunning() {
		return timer != null;
	}

	/**
	 * are we synchronized to the channel?
	 * 
	 * @return
	 */
	public boolean isSynced() {
		return isSynced;
	}

	/** INTERNAL */

	private int getPeriodFraction(double fraction) {
		return (int) Math.round(Math.max(1, PERIOD * fraction));
	}

}
