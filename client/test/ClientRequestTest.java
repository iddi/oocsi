import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import nl.tue.id.oocsi.OOCSIEvent;
import nl.tue.id.oocsi.client.OOCSIClient;
import nl.tue.id.oocsi.client.protocol.DataHandler;
import nl.tue.id.oocsi.client.protocol.EventHandler;
import nl.tue.id.oocsi.client.protocol.OOCSIMessage;

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

		o.subscribe(channelName, new EventHandler() {
			public void receive(OOCSIEvent event) {
				// do nothing
			}
		});

		assertTrue(o.channels().contains(clientName2));
		assertTrue(o.channels().contains(channelName));
	}

	@Test
	public void testMessageContents() throws InterruptedException {

		final List<OOCSIEvent> events = new ArrayList<OOCSIEvent>();

		// create recipient, connect and subscribe for event on a channel
		OOCSIClient recipient = new OOCSIClient("myrecipient");
		recipient.connect("localhost", 4444);
		recipient.subscribe("mychannel", new EventHandler() {
			@Override
			public void receive(OOCSIEvent event) {
				events.add(event);
			}
		});
		recipient.subscribe(new DataHandler() {
			public void receive(String sender, Map<String, Object> data, long timestamp) {
				System.out.println("sender");
			}
		});

		// create sender, connect and send
		OOCSIClient sender = new OOCSIClient("mysender");
		sender.connect("localhost", 4444);
		new OOCSIMessage(sender, "mychannel").data("mykey", "myvalue").send();

		// assertions
		Thread.sleep(200);
		OOCSIEvent event = events.get(0);
		assertEquals("mysender", event.getSender());
		assertEquals("myrecipient", event.getRecipient());
		assertEquals("mychannel", event.getChannel());
		assertTrue(System.currentTimeMillis() - 300 < event.getTime());
	}
}
