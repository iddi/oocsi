package nl.tue.id.oocsi.client.behavior;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import nl.tue.id.oocsi.client.OOCSIClient;
import nl.tue.id.oocsi.client.protocol.Handler;

/**
 * OOCSIAwareness is a system-level primitive that allows for local representations of different OOCSI clients on the
 * same channel and the data they share on that channel. The local representation will be about several keys or all
 * data. This realizes synchronization on data without aggregation.
 *
 * @author matsfunk
 */
public class OOCSIAwareness extends OOCSISystemCommunicator<String> {

	// constant
	private String OOCSIAwarenessTimeoutKey = "OOCSIAwarenessTimeout_" + Math.random();

	// configuration
	private int timeout = 0;
	private List<String> keys = new LinkedList<String>();

	// dynamics
	private Map<String, Map<String, Object>> representation = new ConcurrentHashMap<String, Map<String, Object>>();

	/**
	 * create a new awareness process on the given channel for ALL data
	 * 
	 * @param client
	 * @param channelName
	 */
	public OOCSIAwareness(OOCSIClient client, String channelName) {
		super(client, channelName);
		init(client, channelName);
	}

	/**
	 * create a new awareness proess on the given channel for the specified data (as keys)
	 * 
	 * @param client
	 * @param channelName
	 * @param keys
	 */
	public OOCSIAwareness(OOCSIClient client, String channelName, String... keys) {
		super(client, channelName);

		for (String key : keys) {
			this.keys.add(key);
		}

		init(client, channelName);
	}

	/**
	 * create a new awareness proess on the given channel for the specified data (as keys), a timeout is specified to
	 * "forget" nodes on the channel, unless they post data during the timeout duration
	 * 
	 * @param client
	 * @param channelName
	 * @param timeout
	 * @param keys
	 */
	public OOCSIAwareness(OOCSIClient client, String channelName, int timeout, String... keys) {
		super(client, channelName);
		this.timeout = timeout;

		for (String key : keys) {
			this.keys.add(key);
		}

		init(client, channelName);
	}

	/**
	 * initializes the awareness process
	 * 
	 * @param client
	 * @param channelName
	 */
	private void init(OOCSIClient client, String channelName) {
		client.subscribe(channelName, new Handler() {

			@Override
			public void receive(String sender, Map<String, Object> data, long timestamp, String channel,
					String recipient) {

				// get or create data map for sender
				Map<String, Object> map;
				if (!representation.containsKey(sender)) {
					representation.put(sender, map = new HashMap<String, Object>());
				} else {
					map = get(sender);
				}

				if (map != null) {
					// last updated
					map.put(OOCSIAwarenessTimeoutKey, System.currentTimeMillis());

					// store all if no keys are given
					if (keys.isEmpty()) {
						map.putAll(data);
					}
					// store keys selectively
					else {
						for (String koi : keys) {
							if (data.containsKey(koi)) {
								Object o = data.get(koi);
								if (o != null) {
									map.put(koi, o);
								}
							}
						}
					}
				}
			}
		});
	}

	/**
	 * retrieve the number of nodes
	 * 
	 * @return
	 */
	public int size() {
		return representation.size();
	}

	/**
	 * check whether nodes are locally represented
	 * 
	 * @return
	 */
	public boolean isEmpty() {
		return representation.isEmpty();
	}

	/**
	 * check whether a node given by nodeName is represented
	 * 
	 * @param nodeName
	 * @return
	 */
	public boolean containsNode(Object nodeName) {
		return representation.containsKey(nodeName);
	}

	private Map<String, Object> get(String node) {
		if (representation.containsKey(node)) {
			Map<String, Object> map = representation.get(node);
			if (timeout == 0 || ((Long) map.get(OOCSIAwarenessTimeoutKey)) > System.currentTimeMillis() - timeout) {
				return map;
			} else {
				representation.remove(node);
			}
		}

		return null;
	}

	/**
	 * get data with key from node with given name
	 * 
	 * @param nodeName
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public Integer get(String nodeName, String key, int defaultValue) {
		Map<String, Object> map = get(nodeName);
		return map != null ? (Integer) map.get(key) : defaultValue;
	}

	/**
	 * get data with key from node with given name
	 * 
	 * @param nodeName
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public Long get(String nodeName, String key, long defaultValue) {
		Map<String, Object> map = get(nodeName);
		return map != null ? (Long) map.get(key) : defaultValue;
	}

	/**
	 * get data with key from node with given name
	 * 
	 * @param nodeName
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public Double get(String nodeName, String key, double defaultValue) {
		Map<String, Object> map = get(nodeName);
		return map != null ? (Double) map.get(key) : defaultValue;
	}

	/**
	 * get data with key from node with given name
	 * 
	 * @param nodeName
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public float get(String nodeName, String key, float defaultValue) {
		Map<String, Object> map = get(nodeName);
		return map != null ? (Float) map.get(key) : defaultValue;
	}

	/**
	 * get data with key from node with given name
	 * 
	 * @param nodeName
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public String get(String nodeName, String key, String defaultValue) {
		Map<String, Object> map = get(nodeName);
		return map != null ? (String) map.get(key) : defaultValue;
	}

	/**
	 * get data with key from node with given name
	 * 
	 * @param nodeName
	 * @param key
	 * @return
	 */
	public Object get(String nodeName, String key) {
		Map<String, Object> map = get(nodeName);
		return map != null ? map.get(key) : null;
	}

	/**
	 * remove a node from the local representation
	 * 
	 * @param key
	 * @return
	 */
	public Map<String, Object> remove(String key) {
		return representation.remove(key);
	}

	/**
	 * retrieve set of node names
	 * 
	 * @return
	 */
	public Set<String> nodes() {
		return representation.keySet();
	}

	/**
	 * retrieve all keys that will be represented from node data (configured in constructor)
	 * 
	 * @return
	 */
	public List<String> keys() {
		return Collections.unmodifiableList(keys);
	}
}
