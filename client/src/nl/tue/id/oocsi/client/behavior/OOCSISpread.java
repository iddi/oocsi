package nl.tue.id.oocsi.client.behavior;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import nl.tue.id.oocsi.OOCSIEvent;
import nl.tue.id.oocsi.client.OOCSIClient;
import nl.tue.id.oocsi.client.protocol.EventHandler;
import nl.tue.id.oocsi.client.protocol.Handler;

/**
 * OOCSISpread is a system-level primitive that allows for easy allocation of all OOCSI clients in a channel. The
 * allocation will be about a single key or data item, for which eventually an allocation is provided. This realizes a
 * distribution of roles that can be used to perform different tasks in a system.
 *
 * @author matsfunk
 */
public class OOCSISpread extends OOCSISystemCommunicator<Integer> {

	// call constants
	private static final String ROLE = "role";
	private static final int REBALANCE = 80;

	// infrastructure
	private int timeout;

	// confirmed roles
	private Map<String, Integer> roles = new HashMap<String, Integer>();

	// my role (1 - ...), 0: no role
	private int role = 0;

	// frame counters
	private long frameCount = 0;
	private long framesSinceAssignment = 0;

	// flag to enable or disable period re-balancing of allocations
	private boolean isRebalancing = false;

	/**
	 * create a new OOCSI spread
	 * 
	 * @param client
	 * @param channelName
	 * @param key
	 * @param timeoutMS
	 */
	public OOCSISpread(OOCSIClient client, String channelName, String key, int timeoutMS) {
		this(client, channelName, key, timeoutMS, null);
	}

	/**
	 * create a new OOCSI spread with a handler that will be triggered once a stable allocation has been established
	 * 
	 * @param client
	 * @param channelName
	 * @param key
	 * @param timeoutMS
	 * @param handler
	 */
	public OOCSISpread(OOCSIClient client, String channelName, String key, int timeoutMS, Handler handler) {
		super(client, channelName + "_" + key + "_spread", handler);

		this.timeout = timeoutMS;

		client.subscribe(this.channelName, new EventHandler() {

			@Override
			public void receive(OOCSIEvent event) {

				// record confirmed roles for others
				if (event.has(ROLE)) {
					int confirmedRole = event.getInt(ROLE, -1);
					String confirmedHandle = event.getSender();

					if (confirmedRole > -1 && confirmedHandle != null && confirmedHandle.length() > 0) {
						roles.put(confirmedHandle, confirmedRole);

						// if someone has the same role and I'm still in election, start a new round by resetting my
						// role
						if (confirmedRole == role) {
							role = 0;
							framesSinceAssignment = 0;
						}
					}
				}
			}
		});

		start();
	}

	/**
	 * internally start the allocation process
	 * 
	 */
	private void start() {
		new Timer(true).schedule(new TimerTask() {

			@Override
			public void run() {
				// counters for frames
				frameCount++;
				framesSinceAssignment++;

				// if not role was allocated
				if (role == 0) {
					// start with 1 and use cache to pick better next number
					int triedRole = 1;
					while (roles.values().contains(triedRole)) {
						triedRole++;
					}

					// assume this role for now
					role = triedRole;
					framesSinceAssignment = 0;
				}

				// if a role was allocated
				if (role > 0) {
					if (isRebalancing) {
						// sometimes clear the roles, to shake it up
						if (framesSinceAssignment > REBALANCE * role) {
							reset();
						}
					}

					// send my role periodically
					if (frameCount % 5 == 0) {
						// send out my role periodically
						message(ROLE, role);
					}
				}

				// after balancing, call trigger
				if (isDone()) {
					triggerHandler();
				}
			}
		}, OOCSISpread.this.timeout / 20 + Math.round(Math.random() * OOCSISpread.this.timeout),
				OOCSISpread.this.timeout / 20 + Math.round(Math.random() * OOCSISpread.this.timeout));
	}

	/**
	 * reset the current allocation
	 * 
	 */
	public void reset() {
		roles.clear();
		role = 0;
		frameCount = 0;
		framesSinceAssignment = 0;
	}

	/**
	 * return the current allocation of this instance
	 * 
	 * @return
	 */
	public int get() {
		return role;
	}

	/**
	 * return whether the allocation process is temporarily done (= no changes have occurred for a reasonable time)
	 * 
	 * @return
	 */
	public boolean isDone() {
		return role != -1 && framesSinceAssignment > 10;
	}

	/**
	 * return whether period re-balancing of allocation is switched on or off
	 * 
	 * @return
	 */
	public boolean isRebalancing() {
		return isRebalancing;
	}

	/**
	 * switch periodic re-balancing of allocation
	 * 
	 * @param rebalancing
	 */
	public void setRebalancing(boolean rebalancing) {
		this.isRebalancing = rebalancing;
	}
}
