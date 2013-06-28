package nl.tue.id.oocsi.client.protocol;

abstract public class Handler {

	final public void send(String sender, String data, String timestamp) {
		receive(sender, data, timestamp);
	}

	abstract public void receive(String sender, String data, String timestamp);

}
