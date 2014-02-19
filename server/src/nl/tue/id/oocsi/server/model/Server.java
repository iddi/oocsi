package nl.tue.id.oocsi.server.model;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import nl.tue.id.oocsi.server.OOCSIServer;
import nl.tue.id.oocsi.server.protocol.Protocol;

/**
 * data structure for server
 * 
 * 
 * @author mfunk
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
	 * return a list of clients of this server
	 * 
	 * @return
	 */
	public String getClients() {
		String result = "";
		for (Iterator<String> keys = clients.keySet().iterator(); keys.hasNext();) {
			String key = keys.next();
			result += key + (keys.hasNext() ? "," : "");
		}

		return result;
	}

	/**
	 * add a client to the server, under the condition that the client's name is
	 * not an existing channel
	 * 
	 * @param client
	 * @return
	 */
	public boolean addClient(Client client) {
		String clientName = client.getName();

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
		if (getChannel(clientName) == client) {
			// remove client from client list and sub channels (recursively)
			removeChannel(client, true);
			clients.remove(clientName);

			// close empty channels
			closeEmptyChannels();
		}
	}
}
