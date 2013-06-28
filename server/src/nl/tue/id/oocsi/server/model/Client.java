package nl.tue.id.oocsi.server.model;

import nl.tue.id.oocsi.server.protocol.Message;

public class Client extends Channel {

	public Client(String token) {
		super(token);
	}

	public void send(Message message) {

	}
}
