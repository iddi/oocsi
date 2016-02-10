package nl.tue.id.oocsi.server.model;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import nl.tue.id.oocsi.server.OOCSIServer;
import nl.tue.id.oocsi.server.protocol.Protocol;

/**
 * data structure for server
 * 
 * 
 * @author matsfunk
 * 
 */
public class Server extends Channel {

	protected Map<String, Client> clients = new ConcurrentHashMap<String, Client>();
	protected Protocol protocol;

	/**
	 * create new server data structure
	 */
	public Server() {
		super("SERVER");

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

		// add client to client list and sub channels
		if (!clients.containsKey(clientName) && !subChannels.containsKey(clientName) && getClient(clientName) == null
				&& clientName != OOCSIServer.OOCSI_CONNECTIONS && clientName != OOCSIServer.OOCSI_EVENTS) {
			addChannel(client);
			clients.put(clientName, client);

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

			// disconnect client
			client.disconnect();

			// close empty channels (clean-up)
			closeEmptyChannels();
		}
	}

	/**
	 * check all current clients for last activity
	 * 
	 */
	private void closeStaleClients() {
		long now = System.currentTimeMillis();
		for (Client existingClient : clients.values()) {
			if (now - existingClient.lastAction() > 120000 || !existingClient.isConnected()) {
				removeClient(existingClient);
			}
		}
	}

	/**
	 * subscribe <subscriber> to <channel>
	 * 
	 * @param subscriber
	 * @param channel
	 */
	public void subscribe(Channel subscriber, String channel) {
		String channelName = channel.replaceFirst(":.*", "");
		Channel c = getChannel(channelName);
		if (c != null) {
			if (c.validate(channel)) {
				c.addChannel(subscriber);
				OOCSIServer.logConnection(subscriber.getName(), channelName, "subscribed", new Date());
			}
		} else {
			Channel newChannel = new Channel(subscriber.getName().equals(channelName) ? channelName : channel);
			addChannel(newChannel);
			newChannel.addChannel(subscriber);
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
	public String processInput(Channel sender, String input) {
		return protocol.processInput(sender, input);
	}
}
