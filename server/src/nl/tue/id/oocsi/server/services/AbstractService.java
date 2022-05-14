package nl.tue.id.oocsi.server.services;

import nl.tue.id.oocsi.server.model.Channel.ChangeListener;
import nl.tue.id.oocsi.server.model.Client;
import nl.tue.id.oocsi.server.model.Server;

/**
 * abstract service component class
 * 
 * @author matsfunk
 * 
 */
abstract public class AbstractService {

	protected final Server server;
	protected final ChangeListener presence;

	public AbstractService(Server server) {
		this.server = server;
		this.presence = server.getChangeListener();
	}

	abstract public void start();

	abstract public void stop();

	/**
	 * add a client to the client list in the server
	 * 
	 * @param client
	 * @return
	 */
	public boolean register(Client client) {
		return client.getName().contains(" ") ? false : server.addClient(client);
	}

	/**
	 * remove a client from the client list in the server
	 * 
	 * @param client
	 */
	public void unregister(Client client) {
		presence.leave(client.getName(), client.getName());
		server.removeClient(client);
	}

	/**
	 * process input from a client and return string response
	 * 
	 * @param client
	 * @param inputLine
	 * @return
	 */
	public String processInput(Client client, String inputLine) {
		return server.processInput(client, inputLine);
	}
}
