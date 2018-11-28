package nl.tue.id.oocsi.client.behavior;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import nl.tue.id.oocsi.OOCSIEvent;
import nl.tue.id.oocsi.client.OOCSIClient;
import nl.tue.id.oocsi.client.protocol.EventHandler;
import nl.tue.id.oocsi.client.protocol.Handler;

/**
 * OOCSIConsensus is a system-level primitive that allows for easy consensus between different OOCSI clients on the same
 * channel. The consensus will be about a single key or data item, for which different options are given to vote for.
 * This realizes synchronization on data, not time for which we have OOCSISync.
 *
 * @author matsfunk
 */
public class OOCSIConsensus<T> extends OOCSISystemCommunicator<T> {

	// call constants
	private static final String CALL = "call";
	private static final String VOTE = "vote";
	private static final String CONSENSUS = "consensus";

	// configuration
	int timeout;

	// recording votes
	Map<String, T> votes = new HashMap<String, T>();

	// dynamics
	T myVote;
	T consensus;
	boolean votingDone = true;

	/**
	 * create a new consensus process
	 * 
	 * @param client
	 * @param channelName
	 * @param key
	 * @param timeoutMS
	 */
	public OOCSIConsensus(OOCSIClient client, String channelName, String key, int timeoutMS) {
		this(client, channelName, key, timeoutMS, null);
	}

	/**
	 * create a new consensus process with a callback that will be triggered when the consensus is reached
	 * 
	 * @param client
	 * @param channelName
	 * @param key
	 * @param timeoutMS
	 * @param handler
	 */
	public OOCSIConsensus(OOCSIClient client, String channelName, String key, int timeoutMS, Handler handler) {
		super(client, channelName + "_" + key + "_consensus", handler);

		this.timeout = timeoutMS;

		// first subscribe to sync channel
		subscribe(new EventHandler() {
			@Override
			public void receive(OOCSIEvent event) {
				if (event.has(CALL)) {
					// send our my vote
					message(VOTE, myVote);
				} else if (event.has(VOTE)) {
					// record vote
					try {
						@SuppressWarnings("unchecked")
						T vote = (T) event.getObject(VOTE);
						votes.put(event.getSender(), vote);
					} catch (ClassCastException e) {
						// ignore class cast exceptions
					}
				} else if (event.has(CONSENSUS)) {
					// store consensus
					try {
						@SuppressWarnings("unchecked")
						T vote = (T) event.getObject(CONSENSUS);
						consensus = vote;
					} catch (ClassCastException e) {
						// ignore class cast exceptions
					}
				}
			}
		});
	}

	/**
	 * set my vote for the consensus process
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
	 * returns the current consensus value or my last vote; attention: may return null in case no vote was given
	 * 
	 * @return
	 */
	public T get() {
		return consensus != null ? consensus : myVote;
	}

	/**
	 * returns the current consensus value or the default value if no consensus has been reached yet
	 * 
	 * @param defaultValue
	 * @return
	 */
	public T get(T defaultValue) {
		return consensus != null ? consensus : defaultValue;
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
		votes.put(client.getName(), myVote);

		// send out values to all others to inform them about the outcome
		message(CALL);

		// close voting after timeout has passed
		new Timer(true).schedule(new TimerTask() {
			@Override
			public void run() {
				votingDone = true;

				// report back if handler is given
				triggerHandler();

				// compute consensus across votes
				consensus = getAggregate(votes);

				// send out values to all others to inform them about the outcome
				message(CONSENSUS, consensus);
			}
		}, timeout);
	}

	/**
	 * compute the aggregate of all votes (after all votes have been recorded, the aggregate function is used to compute
	 * a single result value; different implementations might be plugged into this depending on needs)
	 * 
	 * @param votes
	 * @return
	 */
	protected T getAggregate(Map<String, T> votes) {
		return getMajorityAggregate(votes);
	}

	/**
	 * @param votes
	 * @return
	 */
	private T getMajorityAggregate(Map<String, T> votes) {
		// find consensus in votes
		Map<T, Integer> counter = new HashMap<T, Integer>();
		for (T vote : votes.values()) {
			if (counter.containsKey(vote)) {
				counter.put(vote, counter.get(vote) + 1);
			} else {
				counter.put(vote, 1);
			}
		}

		Integer[] array = counter.values().toArray(new Integer[] {});
		Arrays.sort(array);

		int mostFreq = array[array.length - 1];
		for (Map.Entry<T, Integer> e : counter.entrySet()) {
			if (e.getValue() == mostFreq) {
				return e.getKey();
			}
		}

		return myVote;
	}

	public void stop() {
		client.unsubscribe(channelName);
	}

	/** STATIC FACTORY METHODS */

	static public OOCSIConsensus<Integer> createIntegerConsensus(OOCSIClient client, String channelName, String key,
			int timeoutMS) {
		return createIntegerConsensus(client, channelName, key, timeoutMS, null);
	}

	static public OOCSIConsensus<Integer> createIntegerConsensus(OOCSIClient client, String channelName, String key,
			int timeoutMS, Handler handler) {
		OOCSIConsensus<Integer> oocsiConsensus = new OOCSIConsensus<Integer>(client, channelName, key, timeoutMS,
				handler);
		oocsiConsensus.set(0);
		return oocsiConsensus;
	}

	static public OOCSIConsensus<Integer> createIntegerAvgConsensus(OOCSIClient client, String channelName, String key,
			int timeoutMS) {
		return createIntegerAvgConsensus(client, channelName, key, timeoutMS, null);
	}

	static public OOCSIConsensus<Integer> createIntegerAvgConsensus(OOCSIClient client, String channelName, String key,
			int timeoutMS, Handler handler) {
		OOCSIConsensus<Integer> oocsiConsensus = new OOCSIConsensus<Integer>(client, channelName, key, timeoutMS,
				handler) {
			@Override
			protected Integer getAggregate(Map<String, Integer> votes) {
				float aggregate = 0;
				// find consensus in votes
				for (Integer vote : votes.values()) {
					if (vote instanceof Number) {
						aggregate += Float.parseFloat(vote.toString());
					}
				}
				return (int) (aggregate / votes.size());
			}
		};
		oocsiConsensus.set(0);
		return oocsiConsensus;
	}

	static public OOCSIConsensus<Float> createFloatAvgConsensus(OOCSIClient client, String channelName, String key,
			int timeoutMS) {
		return createFloatAvgConsensus(client, channelName, key, timeoutMS, null);
	}

	static public OOCSIConsensus<Float> createFloatAvgConsensus(OOCSIClient client, String channelName, String key,
			int timeoutMS, Handler handler) {
		OOCSIConsensus<Float> oocsiConsensus = new OOCSIConsensus<Float>(client, channelName, key, timeoutMS, handler) {

			/**
			 * @param votes
			 * @return
			 */
			@Override
			protected Float getAggregate(Map<String, Float> votes) {
				float aggregate = 0;
				// find consensus in votes
				for (Float vote : votes.values()) {
					if (vote instanceof Number) {
						aggregate += Float.parseFloat(vote.toString());
					}
				}
				return aggregate / votes.size();
			}

		};
		oocsiConsensus.set(0f);
		return oocsiConsensus;
	}

	static public OOCSIConsensus<String> createStringConsensus(OOCSIClient client, String channelName, String key,
			int timeoutMS) {
		OOCSIConsensus<String> oocsiConsensus = new OOCSIConsensus<String>(client, channelName, key, timeoutMS);
		oocsiConsensus.set("");
		return oocsiConsensus;
	}

	static public OOCSIConsensus<String> createStringConsensus(OOCSIClient client, String channelName, String key,
			int timeoutMS, Handler handler) {
		OOCSIConsensus<String> oocsiConsensus = new OOCSIConsensus<String>(client, channelName, key, timeoutMS,
				handler);
		oocsiConsensus.set("");
		return oocsiConsensus;
	}

	static public OOCSIConsensus<Boolean> createBooleanConsensus(OOCSIClient client, String channelName, String key,
			int timeoutMS) {
		return new OOCSIConsensus<Boolean>(client, channelName, key, timeoutMS);
	}

	static public OOCSIConsensus<Boolean> createBooleanConsensus(OOCSIClient client, String channelName, String key,
			int timeoutMS, Handler handler) {
		return new OOCSIConsensus<Boolean>(client, channelName, key, timeoutMS, handler);
	}
}
