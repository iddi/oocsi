package nl.tue.id.oocsi.server.model;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nl.tue.id.oocsi.server.OOCSIServer;
import nl.tue.id.oocsi.server.protocol.Protocol;
import nl.tue.id.oocsi.server.services.PresenceTracker;
import nl.tue.id.oocsi.server.services.SocketClient;

/**
 * data structure for server
 * 
 * 
 * @author matsfunk
 * 
 */
public class Server extends Channel {

	protected final Map<String, Client> clients = new ConcurrentHashMap<String, Client>();
	protected final Protocol protocol;
	protected final PresenceTracker presence;

	/**
	 * create new server data structure
	 */
	public Server(PresenceTracker presence) {
		super("SERVER", presence);

		// add presence tracker as such
		this.presence = presence;

		// start protocol controller
		protocol = new Protocol(this);
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
		String clientName = client.getName();

		// clean too old clients
		closeStaleClients();

		// check earlier connection from same client, if yes, remove to enable reconnect
		if (clients.containsKey(clientName)) {
			Client existingClient = clients.get(clientName);
			if (existingClient instanceof SocketClient && client instanceof SocketClient) {
				SocketClient socketClientOld = (SocketClient) existingClient;
				SocketClient socketClientNew = (SocketClient) client;
				if (socketClientOld.getIPAddress() != null && socketClientNew.getIPAddress() != null) {
					if (socketClientOld.getIPAddress().equals(socketClientNew.getIPAddress())) {
						// check mini-timeout (last action of old socket at least 2 seconds ago)
						if (socketClientOld.lastAction < System.currentTimeMillis() - 2000) {

							// kill old socket
							presence.leave(clientName, clientName);
							removeClient(existingClient);

							// log
							OOCSIServer.logConnection(clientName, clientName, "replaced client at same IP", new Date());

							// add new socket
							addChannel(client);
							clients.put(clientName, client);
							presence.join(client, client);

							return true;
						}
					}
				}
			}
		}

		// add client to client list and sub channels
		if (!clients.containsKey(clientName) && !subChannels.containsKey(clientName) && getClient(clientName) == null
				&& clientName != OOCSIServer.OOCSI_CONNECTIONS && clientName != OOCSIServer.OOCSI_EVENTS) {
			addChannel(client);
			clients.put(clientName, client);
			presence.join(client, client);

			return true;
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
			// remove client from client list and sub channels (recursively)
			removeChannel(client, true);
			clients.remove(clientName);

			// remove from presence tracking if tracking
			presence.remove(client);

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
		for (Client existingClient : clients.values()) {
			if (now - existingClient.lastAction() > 120000 || !existingClient.isConnected()) {
				OOCSIServer.log("Client " + existingClient.getName()
						+ " has not responded for 120 secs and will be disconnected");

				// remove from presence tracking if tracking
				presence.timeout(existingClient.getName(), existingClient.getName());

				removeClient(existingClient);
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
	 * subscribe <subscriber> to <channel>
	 * 
	 * @param subscriber
	 * @param channel
	 */
	public void subscribe(Client subscriber, String channel) {

		// remove password for private channel
		String channelName = channel.replaceFirst(":.*", "").trim();

		// check for presence subscription
		Pattern presencePattern = Pattern.compile("presence\\(([\\w_-]+)\\)");
		Matcher presenceMatcher = presencePattern.matcher(channelName);
		if (presenceMatcher.find()) {
			String presenceChannelName = presenceMatcher.group(1);
			if (presenceChannelName == null || presenceChannelName.trim().length() == 0) {
				return;
			}

			// add to tracking
			presence.add(presenceChannelName, subscriber);

			// record presence within channel
			Channel ch = getChannel(presenceChannelName);
			if (ch != null) {
				for (Channel sch : ch.getChannels()) {
					presence.join(ch, sch);
				}

				return;
			}

			// record presence of client
			Client client = getClient(presenceChannelName);
			if (client != null) {
				presence.join(client, client);
			} else {
				// // record absence
				// presence.absent(presenceChannelName, presenceChannelName);
			}

			return;
		}

		// non-presence tracking behavior /////////////////////////////////////////////

		// functions for filtering and transformation
		String functions = null;
		Pattern functionPattern = Pattern.compile("([\\w_-]+)\\[(.*)\\]");
		Matcher functionMatcher = functionPattern.matcher(channelName);
		if (functionMatcher.find()) {
			channelName = functionMatcher.group(1);
			functions = functionMatcher.group(2);
		} else {
			// check the functions part
			Pattern brokenPattern = Pattern.compile("\\[[^\\]]*");
			Matcher brokenMatcher = brokenPattern.matcher(channelName);
			if (brokenMatcher.find()) {
				// if function extension is broken, quit
				return;
			}
		}

		// find channel
		Channel c = getChannel(channelName);

		// create channel if not existing
		if (c == null) {
			if (functions != null) {
				// TODO assumption that functions are used without password
				// and vice versa
				c = new Channel(channelName, presence);
			} else {
				c = new Channel(subscriber.getName().equals(channelName) ? channelName : channel, presence);
			}
			addChannel(c);
		}

		// add subscriber to channel
		if (functions != null || c.validate(channel)) {
			if (functions != null) {
				c.addChannel(new FunctionClient(subscriber, channelName, functions, presence));
			} else {

				c.addChannel(subscriber);
			}
			OOCSIServer.logConnection(subscriber.getName(), channelName, "subscribed", new Date());
		}
	}

	/**
	 * unsubscribe <subscriber> from <channel> and close channel if empty
	 * 
	 * @param subscriber
	 * @param channel
	 */
	public void unsubscribe(Channel subscriber, String channel) {
		Channel c = getChannel(channel);
		if (c != null) {
			c.removeChannel(subscriber);
			closeEmptyChannels();
		}
		OOCSIServer.logConnection(subscriber.getName(), channel, "unsubscribed", new Date());
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
