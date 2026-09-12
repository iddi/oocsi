package nl.tue.id.oocsi.server.model;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nl.tue.id.oocsi.server.OOCSIServer;
import nl.tue.id.oocsi.server.protocol.Message;
import nl.tue.id.oocsi.server.protocol.Protocol;
import nl.tue.id.oocsi.server.services.PresenceTracker;

/**
 * data structure for server
 * 
 * 
 * @author matsfunk
 * 
 */
public class Server extends Channel {

	public static final Set<String> RESERVED_NAMES = Set.of(
	        OOCSIServer.SERVER,
	        OOCSIServer.OOCSI_CONNECTIONS,
	        OOCSIServer.OOCSI_EVENTS,
	        OOCSIServer.OOCSI_CHANNELS,
	        OOCSIServer.OOCSI_CLIENTS,
	        OOCSIServer.OOCSI_METRICS
	);

	protected static final int MAX_CLIENT_SUBSCRIPTIONS = 200;

	private static final Pattern CLIENT_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\-.:@#/]+$");
	private static final Pattern CHANNEL_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\-.:/?!@#$%*^<>=+~]+$");
	private static final Pattern PRESENCE_SUBSCRIPTION_PATTERN = Pattern.compile("^presence\\(([a-zA-Z0-9_\\-.:/?!@#$%*^<>=+~]+)\\)$");
	private static final Pattern FUNCTION_FILTER_PATTERN = Pattern.compile("^filter\\([a-zA-Z0-9_+\\-*/%^><=!&|() ,.:\'\"]+\\)$");
	private static final Pattern FUNCTION_TRANSFORM_PATTERN = Pattern.compile("^transform\\([a-zA-Z0-9_\\-]+,[a-zA-Z0-9_+\\-*/%^><=!&|() ,.:\'\"]+\\)$");
	private static final Pattern SCRIPT_OR_HTML = Pattern.compile("(?i)<\\s*/?\\s*[a-zA-Z][a-zA-Z0-9]*\\b|javascript:|on\\w+\\s*=");

	public static boolean isValidClientName(String clientName) {
		if (clientName == null || clientName.isEmpty() || clientName.length() > 200) {
			return false;
		}
		String baseName = clientName.replaceFirst(":.*", "").trim();
		if (baseName.isEmpty() || RESERVED_NAMES.contains(baseName)) {
			return false;
		}
		if (SCRIPT_OR_HTML.matcher(clientName).find()) {
			return false;
		}
		return CLIENT_NAME_PATTERN.matcher(clientName).matches();
	}

	public static boolean isValidChannelName(String channelName) {
		if (channelName == null || channelName.isEmpty() || channelName.length() > 200) {
			return false;
		}
		String baseName = channelName.replaceFirst(":.*", "").trim();
		if (baseName.isEmpty()) {
			return false;
		}
		if (SCRIPT_OR_HTML.matcher(channelName).find()) {
			return false;
		}
		return CHANNEL_NAME_PATTERN.matcher(channelName).matches();
	}

	public static boolean isValidSubscription(String subscription) {
		if (subscription == null || subscription.isEmpty() || subscription.length() > 512) {
			return false;
		}
		String sub = subscription.trim();
		if (sub.isEmpty() || SCRIPT_OR_HTML.matcher(sub).find()) {
			return false;
		}

		// 1. Presence subscription
		Matcher presenceMatcher = PRESENCE_SUBSCRIPTION_PATTERN.matcher(sub);
		if (presenceMatcher.matches()) {
			String presenceChannel = presenceMatcher.group(1);
			if (RESERVED_NAMES.contains(presenceChannel)) {
				return false;
			}
			return isValidChannelName(presenceChannel);
		}

		// 2. Function subscription
		if (sub.contains("[")) {
			if (!sub.endsWith("]")) {
				return false;
			}
			int firstOpen = sub.indexOf('[');
			int lastOpen = sub.lastIndexOf('[');
			int firstClose = sub.indexOf(']');
			int lastClose = sub.lastIndexOf(']');
			if (firstOpen != lastOpen || firstClose != lastClose || firstOpen >= firstClose) {
				return false;
			}

			String channelSpec = sub.substring(0, firstOpen).trim();
			String functions = sub.substring(firstOpen + 1, firstClose).trim();
			if (channelSpec.isEmpty() || functions.isEmpty() || functions.length() > 512) {
				return false;
			}

			// Validate channel part
			String baseName = channelSpec.replaceFirst(":.*", "").trim();
			if (baseName.endsWith("/?")) {
				baseName = baseName.substring(0, baseName.length() - 2).trim();
			}
			if (!isValidChannelName(baseName)) {
				return false;
			}

			// Validate function expressions
			String[] parts = functions.split(";");
			for (String part : parts) {
				String fct = part.trim();
				if (fct.isEmpty()) {
					return false;
				}
				if (!FUNCTION_FILTER_PATTERN.matcher(fct).matches()
				        && !FUNCTION_TRANSFORM_PATTERN.matcher(fct).matches()) {
					return false;
				}
				if (SCRIPT_OR_HTML.matcher(fct).find()) {
					return false;
				}
				if (!hasBalancedParentheses(fct)) {
					return false;
				}
			}
			return true;
		}

		// 3. Plain channel
		String baseName = sub.replaceFirst(":.*", "").trim();
		if (baseName.endsWith("/?")) {
			baseName = baseName.substring(0, baseName.length() - 2).trim();
		}
		return isValidChannelName(baseName);
	}

	private static boolean hasBalancedParentheses(String s) {
		int count = 0;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '(') {
				count++;
			} else if (c == ')') {
				count--;
				if (count < 0) {
					return false;
				}
			}
		}
		return count == 0;
	}

	protected final Map<String, Client> clients = new ConcurrentHashMap<String, Client>();
	protected final Protocol protocol;
	protected PresenceTracker presence;
	protected final Map<String, Message> delayedMessages;
	protected final Map<String, AtomicInteger> subscriptionCounts = new ConcurrentHashMap<>();

	/**
	 * create new server data structure
	 */
	public Server() {
		super("SERVER", PresenceTracker.NULL_LISTENER);

		// create presence tracker
		presence = new PresenceTracker(this);

		// start protocol controller
		protocol = new Protocol(this);

		// create map for delayed messages
		delayedMessages = new ConcurrentHashMap<>();
	}

	@Override
	public boolean send(Message message) {
		// disable direct send
		return false;
	}

	/**
	 * dispatch a delayed message; will replace earlier delayed and not yet delivered messages for this client
	 * 
	 * @param sender
	 * @param message
	 */
	public void sendDelayedMessage(String sender, Message message) {
		synchronized (delayedMessages) {
			String key = sender + ":" + message.getRecipient();
			delayedMessages.put(key, message);
		}
	}

	/**
	 * retrieve client from client list
	 * 
	 * @param clientName
	 * @return
	 */
	public Client getClient(String clientName) {
		return clients.get(clientName);
	}

	/**
	 * return a list of client of this server
	 * 
	 * @return
	 */
	public Collection<Client> getClients() {
		return clients.values();
	}

	/**
	 * list all clients as comma-separated String list
	 * 
	 * @return
	 */
	public String getClientList() {
		String result = "";
		for (Iterator<String> keys = clients.keySet().iterator(); keys.hasNext();) {
			String key = keys.next();
			result += key + (keys.hasNext() ? "," : "");
		}

		return result;
	}

	/**
	 * add a client to the server, under the condition that the client's name is not an existing channel
	 * 
	 * @param client
	 * @return
	 */
	public boolean addClient(Client client) {
		if (client == null) {
			return false;
		}
		String clientName = client.getName();
		if (!isValidClientName(clientName)) {
			return false;
		}

		// clean too old clients
		closeStaleClients();

		// add client to client list and sub channels
		if (!subChannels.containsKey(clientName) && getClient(clientName) == null) {
			if (clients.putIfAbsent(clientName, client) == null) {
				addChannel(client);
				presence.join(client, client);
				return true;
			}
		}

		return false;
	}

	/**
	 * remove a connected client from the server
	 * 
	 * @param client
	 */
	public void removeClient(Client client) {
		String clientName = client.getName();

		// check first if this is really the client to remove
		if (getClient(clientName) == client || getChannel(clientName) == client) {

			// only report presence information for public clients
			if (!client.isPrivate()) {
				// remove from presence tracking if tracking
				presence.leave(client, client);
				presence.remove(client);
			}

			// remove client from client list and sub channels (recursively)
			removeChannel(client, true);
			clients.remove(clientName);
			subscriptionCounts.remove(clientName);

			// disconnect client
			client.disconnect();

			// close empty channels (clean-up)
			closeEmptyChannels();
		}
	}

	/**
	 * check whether the server can accept this client
	 * 
	 * @param c
	 * @return
	 */
	public boolean canAcceptClient(Client c) {
		return true;
	}

	/**
	 * check all current clients for last activity
	 * 
	 */
	protected void closeStaleClients() {
		long now = System.currentTimeMillis();
		for (Client client : clients.values()) {
			if (client.lastAction() + 120000 < now || !client.isConnected()) {
				OOCSIServer
				        .log("Client " + client.getName() + " has not responded for 120 secs and will be disconnected");

				// remove from presence tracking if tracking
				presence.timeout(client);

				removeClient(client);
			}
		}
	}

	/**
	 * retrieve the change listener
	 * 
	 * @return
	 */
	public ChangeListener getChangeListener() {
		return presence;
	}

	/**
	 * refresh presence trackers and send out channel client lists
	 * 
	 */
	public void refreshPresence() {
		try {
			presence.refresh();
		} catch (Exception e) {
		}
	}

	private int countClientSubscriptions(Channel subscriber) {
		if (subscriber == null) {
			return 0;
		}
		AtomicInteger count = subscriptionCounts.get(subscriber.getName());
		return count != null ? count.get() : 0;
	}

	/**
	 * subscribe <subscriber> to <channel>
	 * 
	 * @param subscriber
	 * @param channel
	 */
	public void subscribe(Client subscriber, String channel) {

		if (subscriber == null || channel == null || !isValidSubscription(channel)) {
			return;
		}

		AtomicInteger subCount = subscriptionCounts.computeIfAbsent(subscriber.getName(), k -> new AtomicInteger(0));
		if (subCount.get() >= MAX_CLIENT_SUBSCRIPTIONS) {
			return;
		}

		// ------------------------------------------------------------------------------------------------------------
		// check for presence subscription
		Pattern presencePattern = Pattern.compile("presence\\(([a-zA-Z0-9_\\-.:/?!@#$%*^<>=+~]+)\\)");
		Matcher presenceMatcher = presencePattern.matcher(channel.trim());
		if (presenceMatcher.find()) {

			// extract presence channel name, or abort
			String presenceChannelName = presenceMatcher.group(1);
			if (presenceChannelName == null || presenceChannelName.trim().length() == 0
			        || !isValidChannelName(presenceChannelName)) {
				return;
			}

			// add presenceChannel to presence tracking if not existing
			// note: we use the name: String because the actual channel might not exist yet
			presence.subscribe(presenceChannelName, subscriber);

			return;
		}

		// ------------------------------------------------------------------------------------------------------------
		// functions for filtering and transformation
		String functions = null;
		Matcher functionMatcher = Pattern.compile("\\[(.*)\\]").matcher(channel);
		if (functionMatcher.find()) {
			functions = functionMatcher.group(1);
			if (functions != null && functions.length() > 512) {
				return;
			}
		} else {
			// check the functions part
			Pattern brokenPattern = Pattern.compile("\\[[^\\]]*");
			Matcher brokenMatcher = brokenPattern.matcher(channel);
			if (brokenMatcher.find()) {
				// if function extension is broken, quit
				return;
			}
		}

		String cleanToken = channel.replaceAll("\\[[^\\]]*\\]", "").trim();
		String channelName = cleanToken.replaceFirst(":.*", "").trim();

		// check channel length and validity first
		if (channelName.length() == 0 || !isValidChannelName(channelName)) {
			return;
		}

		// check for /? subscription on reserved or private channels
		String baseForSlash = null;
		String tokenWithoutSlash = cleanToken;
		if (channelName.endsWith("/?")) {
			baseForSlash = channelName.substring(0, channelName.length() - 2).trim();
			tokenWithoutSlash = cleanToken.substring(0, cleanToken.length() - 2).trim();
		} else if (cleanToken.contains("/?")) {
			tokenWithoutSlash = cleanToken.replace("/?", "").trim();
			baseForSlash = tokenWithoutSlash.replaceFirst(":.*", "").trim();
		}

		if (baseForSlash != null) {
			if (RESERVED_NAMES.contains(baseForSlash)) {
				return;
			}
			Channel baseChannel = subChannels.get(baseForSlash);
			if (baseChannel != null && baseChannel.isPrivate()
			        && !baseChannel.validate(tokenWithoutSlash)) {
				return;
			}
			channelName = baseForSlash + "/?";
			cleanToken = baseForSlash + "/?";
		}

		// find channel
		Channel c = subChannels.get(channelName);

		// create channel if not existing
		if (c == null) {
			c = new Channel(subscriber.getName().equals(channelName) ? channelName : cleanToken, presence);
			addChannel(c);
		}

		// add subscriber to channel if authorized
		boolean authorized = !c.isPrivate() || c.validate(cleanToken);
		if (authorized) {
			if (functions != null) {
				c.addChannel(new FunctionClient(subscriber, subscriber.getName(), functions, presence));
			} else {

				c.addChannel(subscriber);
			}
			subCount.incrementAndGet();
			OOCSIServer.logConnection(subscriber.getName(), channelName, "subscribed", new Date());
		}
	}

	/**
	 * unsubscribe <subscriber> from <channel> and close channel if empty
	 * 
	 * @param subscriber
	 * @param channelName
	 */
	public void unsubscribe(Channel subscriber, String channelName) {

		if (subscriber == null || channelName == null || !isValidSubscription(channelName)) {
			return;
		}

		// ------------------------------------------------------------------------------------------------------------
		// check for presence unsubscribe
		Pattern presencePattern = Pattern.compile("presence\\(([a-zA-Z0-9_\\-.:/?!@#$%*^<>=+~]+)\\)");
		Matcher presenceMatcher = presencePattern.matcher(channelName.trim());
		if (presenceMatcher.find()) {

			// extract presence channel name, or abort
			String presenceChannelName = presenceMatcher.group(1);
			if (presenceChannelName == null || presenceChannelName.trim().length() == 0) {
				return;
			}

			// remove the presence subscription for this channel
			presence.unsubscribe(presenceChannelName, subscriber);

			return;
		}

		// ------------------------------------------------------------------------------------------------------------
		// functions for filtering and transformation
		Pattern functionPattern = Pattern.compile("([a-zA-Z0-9_\\-.:/?!@#$%*^<>=+~]+)\\[(.*)\\]");
		Matcher functionMatcher = functionPattern.matcher(channelName);
		if (functionMatcher.find()) {
			channelName = functionMatcher.group(1);
		}

		// normal channel unsubscribe
		Channel c = getChannel(channelName);
		if (c != null) {
			c.removeChannel(subscriber);
			AtomicInteger count = subscriptionCounts.get(subscriber.getName());
			if (count != null && count.get() > 0) {
				count.decrementAndGet();
			}
			closeEmptyChannels();
			OOCSIServer.logConnection(subscriber.getName(), channelName, "unsubscribed", new Date());
		}
	}

	/**
	 * delegate the processing of input (from a service) to the protocol and return string response
	 * 
	 * @param sender
	 * @param input
	 * @return
	 */
	public String processInput(Client sender, String input) {
		return protocol.processInput(sender, input);
	}

}
