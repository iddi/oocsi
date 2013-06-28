package nl.tue.id.oocsi.server.model;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import nl.tue.id.oocsi.server.protocol.Protocol;

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

	public Client getClient(String clientName) {
		return clients.get(clientName);
	}

	/**
	 * return a list of clients of this server
	 * 
	 * @return
	 */
	public String getClients() {
		String result = "";
		for (Iterator<String> keys = clients.keySet().iterator(); keys
				.hasNext();) {
			String key = keys.next();
			result += key + (keys.hasNext() ? "," : "");
		}

		return result;
	}

	public boolean addClient(Client client) {
		String clientName = client.getName();

		// add client to client list and sub channels
		if (!subChannels.containsKey(clientName)
				&& getClient(clientName) == null) {
			addChannel(client);
			clients.put(clientName, client);

			return true;
		}

		return false;
	}

	public void removeClient(Client client) {
		String clientName = client.getName();

		// remove client from client list and sub channels (recursively)
		removeChannel(client, true);
		clients.remove(clientName);
	}
}
