package nl.tue.id.oocsi.client.behavior;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import nl.tue.id.oocsi.client.OOCSIClient;
import nl.tue.id.oocsi.client.protocol.Handler;

/**
 * OOCSIGather is a system-level primitive that allows for easy collection of all values that different OOCSI clients
 * contribute for the same channel. The distribution will be about a single key or data item. This realizes an overview
 * on data.
 *
 * @author matsfunk
 */
public class OOCSIGather<T> extends OOCSISystemCommunicator<T> {

	// call constants
	private static final String CALL = "call";
	private static final String VOTE = "vote";

	// configuration
	int timeout;

	// dynamics
	T myVote;
	Map<String, T> votes = new HashMap<String, T>();
	boolean votingDone = true;

	/**
	 * create a new gathering process
	 * 
	 * @param client
	 * @param channelName
	 * @param key
	 * @param timeoutMS
	 */
	public OOCSIGather(OOCSIClient client, String channelName, String key, int timeoutMS) {
		this(client, channelName, key, timeoutMS, null);
	}

	/**
	 * create a new gathering process with a callback that will be called when the process is done
	 * 
	 * @param client
	 * @param channelName
	 * @param key
	 * @param timeoutMS
	 * @param handler
	 */
	public OOCSIGather(OOCSIClient client, String channelName, String key, int timeoutMS, Handler handler) {
		super(client, channelName + "_" + key + "_gather", handler);

		this.timeout = timeoutMS;

		// first subscribe to sync channel
		client.subscribe(this.channelName, new Handler() {

			@Override
			public void receive(String sender, Map<String, Object> data, long timestamp, String channel,
					String recipient) {

				if (data.containsKey(CALL)) {
					// new gathering, reset all votes
					votes.clear();
					// add my vote if it exists
					if (myVote != null) {
						votes.put(OOCSIGather.this.client.getName(), myVote);
					}

					// send out my vote
					message(VOTE, myVote);
				} else if (data.containsKey(VOTE)) {
					// record vote an incoming vote
					try {
						@SuppressWarnings("unchecked")
						T vote = (T) data.get(VOTE);
						if (vote != null) {
							votes.put(sender, vote);
						}
					} catch (ClassCastException e) {
					}
				}
			}
		});
	}

	/**
	 * stop participating in this gathering process
	 * 
	 */
	public void stop() {
		client.unsubscribe(this.channelName);
	}

	/**
	 * set my vote for the gathering process
	 * 
	 * @param myVote
	 */
	public void set(T myVote) {
		// only do if we are not voting right now
		if (!myVote.equals(this.myVote)) {

			// save own vote
			this.myVote = myVote;

			// start process
			start();
		}
	}

	/**
	 * returns the current set of options mapped to their frequency
	 * 
	 * @return
	 */
	public Map<T, Integer> get() {
		Map<T, Integer> resultSet = new HashMap<T, Integer>();
		for (T t : votes.values()) {
			if (resultSet.containsKey(t)) {
				resultSet.put(t, resultSet.get(t) + 1);
			} else {
				resultSet.put(t, 1);
			}
		}

		return resultSet;
	}

	/** INTERNAL */

	/**
	 * start the consensus process
	 * 
	 */
	private void start() {

		// only if timeout has passed (throttling)
		if (!votingDone) {
			return;
		}

		// clear previous votes
		votes.clear();

		// send out values to all others to inform them about the outcome
		message(CALL);

		// add my vote internally
		votes.put(client.getName(), myVote);

		// directly send my vote, so others know about it as well
		message(VOTE, myVote);

		// close voting after timeout has passed
		new Timer(true).schedule(new TimerTask() {
			@Override
			public void run() {
				votingDone = true;

				// report back if handler is given
				triggerHandler();
			}
		}, timeout);
	}
}
