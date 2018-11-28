package nl.tue.id.oocsi.client.behavior;

import java.util.Timer;
import java.util.TimerTask;

import nl.tue.id.oocsi.OOCSIEvent;
import nl.tue.id.oocsi.client.OOCSIClient;
import nl.tue.id.oocsi.client.protocol.EventHandler;
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
	private static final String CYCLE = "cycle";
	// sync resolution
	private static int PERIOD = 20;
	// progress sync point (to avoid spikes in network communication)
	private static int progressSync = (int) (Math.random() * (float) PERIOD);

	// infrastructure, configuration
	private int periodMS;

	// dynamics
	private Timer timer;
	private boolean isSynced = false;
	private int progress = 0;
	private int cycle = 0;

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

		// first subscribe to sync channel
		subscribe(new EventHandler() {
			@Override
			public void receive(OOCSIEvent event) {
				if (event.has(SYNC)) {
					int progressSync = PERIOD - event.getInt(SYNC, 0);
					int virtualProgress = (progress + progressSync) % PERIOD;

					// compare the synchronizing from others to my own progress in the periodic sync cycle
					if (virtualProgress < getPeriodFraction(0.05) || virtualProgress > getPeriodFraction(0.95)) {
						isSynced = true;
					} else if (virtualProgress <= getPeriodFraction(0.9)) {
						long d = Math.round(getPeriodFraction(1. / PERIOD) * Math.random())
								- Math.round((float) virtualProgress / PERIOD);
						progress -= d;
						isSynced = false;
					} else {
						isSynced = false;
					}

					// cycle update
					if (event.has(CYCLE)) {
						cycle = Math.max(cycle, event.getInt(CYCLE, 0));
					}
				}
			}
		});

		start();
	}

	/**
	 * start the synchronization process
	 * 
	 */
	public void start() {
		// send out sync signal myself periodically
		(timer = new Timer(true)).schedule(new TimerTask() {
			@Override
			public void run() {
				// send sync signal to channel
				if (progress == progressSync) {
					message().data(SYNC, progress).data(CYCLE, cycle).send();
				}

				// reset progress
				if (progress-- <= 0) {
					// trigger pulse
					triggerHandler();

					// increase cycle
					cycle++;

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
	 * return the current cycle count
	 * 
	 * @return
	 */
	public int getCycle() {
		return cycle;
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
