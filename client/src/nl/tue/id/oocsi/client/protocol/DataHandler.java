package nl.tue.id.oocsi.client.protocol;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Map;

import nl.tue.id.oocsi.client.socket.Base64Coder;

abstract public class DataHandler extends Handler {

	public void receive(String sender, String data, String timestamp) {

		try {
			ByteArrayInputStream bais = new ByteArrayInputStream(
					Base64Coder.decode(data));
			ObjectInputStream ois = new ObjectInputStream(bais);
			Object outputObject = ois.readObject();
			@SuppressWarnings("unchecked")
			Map<String, Object> map = (Map<String, Object>) outputObject;

			receive(sender, map, timestamp);

		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

	}

	abstract public void receive(String sender, Map<String, Object> data,
			String timestamp);

}
