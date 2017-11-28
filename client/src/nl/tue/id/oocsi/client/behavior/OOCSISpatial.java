package nl.tue.id.oocsi.client.behavior;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import nl.tue.id.oocsi.OOCSIData;
import nl.tue.id.oocsi.OOCSIEvent;
import nl.tue.id.oocsi.client.OOCSIClient;
import nl.tue.id.oocsi.client.behavior.OOCSISpatial.Position;
import nl.tue.id.oocsi.client.protocol.Handler;
import nl.tue.id.oocsi.client.protocol.MultiMessage;
import nl.tue.id.oocsi.client.protocol.OOCSIMessage;
import nl.tue.id.oocsi.client.services.OOCSICall;
import nl.tue.id.oocsi.client.services.Responder;

/**
 * OOCSISpatial is a system-level primitive that allows for routing across a multi-dimensional lattice of positions of
 * different OOCSI clients.
 *
 * @author matsfunk
 */
@SuppressWarnings("rawtypes")
public class OOCSISpatial extends OOCSISystemCommunicator<Position> {

	private static final int REBALANCING_TIMEOUT = 20000;

	// call constants
	private static final String CALL = "oocsi_spatial_call";
	private static final String VOTE = "oocsi_spatial_vote";

	private static final String DESTINATION = "oocsi_spatial_destination_request";
	private static final String DESTINATION_RESPONSE = "oocsi_spatial_destination_distance";
	private static final String ROUTING_PATH = "oocsi_spatial_routing_path";

	// configuration
	int timeout;

	// dynamics
	private Position metric;
	public Map<String, Position> positions = new ConcurrentHashMap<String, Position>();
	private boolean votingDone = true;

	/**
	 * same as all, one value and a distance metric, routing is easy (shortest by direct distance)
	 * 
	 * @param client
	 * @param channelName
	 * @param key
	 * @param myPosition
	 * @param neighbourThreshold
	 */
	public OOCSISpatial(OOCSIClient client, String channelName, String key, Position<?> neighborDistance) {
		this(client, channelName, key, (int) (Math.random() * REBALANCING_TIMEOUT), null);

		// define distance metric to separate neighbors from all others
		// define routing throughout the neighbors
		metric = neighborDistance;

		start();
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
	private OOCSISpatial(final OOCSIClient client, final String channelName, String key, int timeoutMS,
			Handler handler) {
		super(client, channelName + "_" + key + "_spatial", handler);

		this.timeout = timeoutMS;

		// first subscribe to sync channel
		client.subscribe(this.channelName, new Handler() {

			@Override
			public void receive(String sender, Map<String, Object> data, long timestamp, String channel,
					String recipient) {

				// call coming in?
				if (data.containsKey(CALL)) {

					// add my vote if it exists
					if (metric != null) {
						positions.put(OOCSISpatial.this.client.getName(), metric);
					}

					// send out my vote
					new OOCSIMessage(client, OOCSISpatial.this.channelName).data(VOTE, metric.serialise())
							.data(HANDLE, getHandle()).send();
				}
				// vote coming in?
				else if (data.containsKey(VOTE)) {
					// record vote an incoming vote
					try {
						Position<?> vote = metric.deserialise(data.get(VOTE).toString());
						if (vote != null) {
							positions.put(sender, vote);
						}
					} catch (ClassCastException e) {
					}
				}
			}
		});

		client.register(DESTINATION, new Responder(client) {
			@SuppressWarnings("unchecked")
			@Override
			public void respond(OOCSIEvent event, OOCSIData response) {
				if (event.has(DESTINATION)) {
					String destination = event.getString(DESTINATION);
					String path = event.getString(ROUTING_PATH, "");

					if (path.contains(client.getName() + ",")) {
						response.data(DESTINATION_RESPONSE, Float.MAX_VALUE);
						return;
					}

					if (getNeighbors().contains(destination)) {
						response.data(DESTINATION_RESPONSE, metric.distance(positions.get(destination)));
					} else {
						float minDistance = Float.MAX_VALUE;
						int timeoutMS2 = 2000 - path.split(",").length * 100;
						MultiMessage mm = neighborCall(DESTINATION).data(DESTINATION, destination).data(ROUTING_PATH,
								path + client.getName() + ",");
						mm.sendAndWait(timeoutMS2);
						for (OOCSIMessage om : mm.getMessages()) {
							if (om instanceof OOCSICall) {
								OOCSIEvent subEvent = ((OOCSICall) om).getFirstResponse();
								if (subEvent != null) {
									float pathLength = subEvent.getFloat(DESTINATION_RESPONSE, -1);
									if (pathLength < Float.MAX_VALUE) {
										float temp = metric.distance(positions.get(subEvent.getSender())) + pathLength;
										if (temp > -1 && temp < minDistance) {
											minDistance = temp;
										}
									}
								}
							}
						}

						response.data(DESTINATION_RESPONSE, minDistance);
					}
				} else {
					response.data(DESTINATION_RESPONSE, Float.MAX_VALUE);
				}
			}
		});

	}

	/**
	 * create an OOCSISpatial for 1D float positions
	 * 
	 * @param client
	 * @param channelName
	 * @param key
	 * @param myPosition
	 * @param neighborDistance
	 * @return
	 */
	static public OOCSISpatial createSpatial(OOCSIClient client, String channelName, String key, final float myPosition,
			final float neighborDistance) {
		return new OOCSISpatial(client, channelName, key, new Position1D(myPosition, neighborDistance));
	}

	/**
	 * create an OOCSISpatial for 2D float positions
	 *
	 * @param client
	 * @param channelName
	 * @param key
	 * @param myPositionX
	 * @param myPositionY
	 * @param neighborDistance
	 * @return
	 */
	static public OOCSISpatial createSpatial(OOCSIClient client, String channelName, String key,
			final float myPositionX, final float myPositionY, final float neighborDistance) {
		return new OOCSISpatial(client, channelName, key, new Position2D(myPositionX, myPositionY, neighborDistance));
	}

	/**
	 * stop participating in this gathering process
	 * 
	 */
	public void stop() {
		client.unsubscribe(this.channelName);
	}

	/**
	 * returns the current set of direct neighbors
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<String> getNeighbors() {
		List<String> resultSet = new LinkedList<String>();
		for (String key : positions.keySet()) {
			if (!client.getName().equals(key) && metric.isNeighbor(positions.get(key))) {
				resultSet.add(key);
			}
		}

		return resultSet;
	}

	/**
	 * returns a message container that includes messages to all neighbors which can be filled and sent all at once
	 * 
	 * @return
	 */
	public MultiMessage neighbors() {
		MultiMessage mm = new MultiMessage(client);
		for (String nb : getNeighbors()) {
			mm.add(new OOCSIMessage(client, nb));
		}

		return mm;
	}

	/**
	 * return a message container that includes OOCSICalls to all neighbors which can be filled and sent all at once
	 * 
	 * @param callName
	 * @return
	 */
	public MultiMessage neighborCall(String callName) {
		MultiMessage mm = new MultiMessage(client);
		for (String nb : getNeighbors()) {
			mm.add(new OOCSICall(client, nb, callName, 2000, 100));
		}

		return mm;
	}

	/**
	 * returns the handle of the closest neighbor
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public String getClosestNeighbor() {
		return metric.closestNeighbor(positions);
	}

	/**
	 * returns the handle of the neighbor through which the closest path to the destination can be routed at this moment
	 * 
	 * @param destination
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public String routing(String destination) {

		// is my direct neighbor?
		if (getNeighbors().contains(destination)) {
			return destination;
		}

		// not my direct neighbor...

		// send to all neighbors to ask their neighbors
		// add my name to path, so I don't have to answer my own question when my neighbor calls back
		MultiMessage mm = neighborCall(DESTINATION).data(DESTINATION, destination).data(ROUTING_PATH,
				client.getName() + ",");
		mm.sendAndWait();

		float minDistance = Float.MAX_VALUE;
		String neighbor = null;
		for (OOCSIMessage om : mm.getMessages()) {
			if (om instanceof OOCSICall) {
				OOCSIEvent event = ((OOCSICall) om).getFirstResponse();
				if (event != null) {
					// get length of path from neighbor onwards
					float pathLength = event.getFloat(DESTINATION_RESPONSE, -1);
					if (pathLength > -1 && pathLength < Float.MAX_VALUE) {
						// add distance to neighbor to path length
						pathLength = metric.distance(positions.get(event.getSender())) + pathLength;
						if (pathLength > -1 && pathLength < minDistance) {
							// if ok, this is now the smallest distance
							minDistance = pathLength;
							neighbor = event.getSender();
						}
					}
				}
			}
		}

		// now we send off the data towards the shortest route neighbor
		return neighbor;
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
		positions.clear();

		// request positions
		message(CALL);

		// add my vote internally
		positions.put(client.getName(), metric);

		// directly send my vote, so others know about it as well
		new OOCSIMessage(client, this.channelName).data(VOTE, metric.serialise()).data(HANDLE, getHandle()).send();

		// close voting after timeout has passed
		new Timer(true).schedule(new TimerTask() {
			@Override
			public void run() {
				votingDone = true;

				// report back if handler is given
				triggerHandler();
			}
			// trigger after 10 * timeout milliseconds
		}, timeout * 10);
	}

	abstract static class Position<T> implements DistanceMetric<T> {
		abstract public String serialise();

		abstract public Position<T> deserialise(String position);
	}

	public static interface DistanceMetric<T> {
		public float distance(T value);

		public boolean isNeighbor(T value);

		public String closestNeighbor(Map<String, T> positions);

	}

	static class Position1D extends Position<Position1D> {

		private float myPosition;
		private float neighborDistance;

		public Position1D(float position, float neighborDistance) {
			this.myPosition = position;
			this.neighborDistance = neighborDistance;
		}

		@Override
		public float distance(Position1D value) {
			return Math.abs(myPosition - value.myPosition);
		}

		@Override
		public boolean isNeighbor(Position1D value) {
			return distance(value) <= neighborDistance;
		}

		@Override
		public String closestNeighbor(Map<String, Position1D> positions) {
			String result = null;
			float minDistance = neighborDistance;
			for (String key : positions.keySet()) {
				float abs = distance(positions.get(key));
				if (abs > 0 && abs < minDistance) {
					minDistance = abs;
					result = key;
				}
			}

			return result;
		}

		@Override
		public String serialise() {
			return "" + myPosition;
		}

		@Override
		public Position1D deserialise(String position) {
			return new Position1D(Float.parseFloat(position), neighborDistance);
		}
	}

	static class Position2D extends Position<Position2D> {

		private float myPositionX;
		private float myPositionY;
		private float neighborDistance;

		public Position2D(float positionX, float positionY, float neighborDistance) {
			this.myPositionX = positionX;
			this.myPositionY = positionY;
			this.neighborDistance = neighborDistance;
		}

		@Override
		public float distance(Position2D value) {
			return (float) Math
					.sqrt(Math.pow(myPositionX - value.myPositionX, 2) + Math.pow(myPositionY - value.myPositionY, 2));
		}

		@Override
		public boolean isNeighbor(Position2D value) {
			return distance(value) <= neighborDistance;
		}

		@Override
		public String closestNeighbor(Map<String, Position2D> positions) {
			String result = null;
			float minDistance = neighborDistance;
			for (String key : positions.keySet()) {
				float abs = distance(positions.get(key));
				if (abs > 0 && abs < minDistance) {
					minDistance = abs;
					result = key;
				}
			}

			return result;
		}

		@Override
		public String serialise() {
			return myPositionX + ";" + myPositionY;
		}

		@Override
		public Position2D deserialise(String position) {
			String[] ps = position.split(";");
			return new Position2D(Float.parseFloat(ps[0]), Float.parseFloat(ps[1]), neighborDistance);
		}
	}
}