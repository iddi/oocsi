import static org.junit.Assert.assertTrue;
import nl.tue.id.oocsi.client.OOCSIClient;
import nl.tue.id.oocsi.client.protocol.Handler;

import org.junit.Test;

public class ClientRequestTest {

	@Test
	public void testClientAvailable() {
		String clientName1 = "test_client1";

		OOCSIClient o = new OOCSIClient(clientName1);
		o.connect("localhost", 4444);

		assertTrue(o.isConnected());
		assertTrue(o.clients().contains(clientName1));
	}

	@Test
	public void testChannelAvailable() throws InterruptedException {
		String clientName2 = "test_client2";
		String channelName = "test_channel";

		OOCSIClient o = new OOCSIClient(clientName2);
		o.connect("localhost", 4444);

		assertTrue(o.isConnected());
		assertTrue(o.channels().contains(clientName2));

		o.subscribe(channelName, new Handler() {
			public void receive(String sender, String data, String timestamp) {
				// do nothing
			}
		});

		assertTrue(o.channels().contains(clientName2));
		assertTrue(o.channels().contains(channelName));
	}
}
