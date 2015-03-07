package nl.tue.id.oocsi.server.services;

import nl.tue.id.oocsi.server.model.Client;
import nl.tue.id.oocsi.server.model.Server;

abstract public class AbstractService {

	protected Server server;

	public AbstractService(Server server) {
		this.server = server;
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
		return server.addClient(client);
	}

	/**
	 * remove a client from the client list in the server
	 * 
	 * @param client
	 */
	public void unregister(Client client) {
		server.removeClient(client);
	}

	/**
	 * process input from a client and return string response
	 * 
	 * @param client
	 * @param inputLine
	 * @return
	 */
	public String processInput(SocketClient client, String inputLine) {
		return server.processInput(client, inputLine);
	}
}
