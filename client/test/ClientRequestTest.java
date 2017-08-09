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
		String clientName = "test_client_client_available_1";

		OOCSIClient o = new OOCSIClient(clientName);
		o.connect("localhost", 4444);

		assertTrue(o.isConnected());
		assertTrue(o.clients().contains(clientName));
	}

	@Test
	public void testChannelAvailable() throws InterruptedException {
		String clientName = "test_client_channel_available_2";
		String channelName = "test_channel";

		OOCSIClient o = new OOCSIClient(clientName);
		o.connect("localhost", 4444);

		assertTrue(o.isConnected());
		assertTrue(o.channels().contains(clientName));

		o.subscribe(channelName, new EventHandler() {
			public void receive(OOCSIEvent event) {
				// do nothing
			}
		});

		assertTrue(o.channels().contains(clientName));
		assertTrue(o.channels().contains(channelName));
	}

	@Test
	public void testMessageContents() throws InterruptedException {
		String clientNameRecipient = "test_client_message_contents_1";
		String clientNameSender = "test_client_message_contents_2";

		final List<OOCSIEvent> events = new ArrayList<OOCSIEvent>();

		// create recipient, connect and subscribe for event on a channel
		OOCSIClient recipient = new OOCSIClient(clientNameRecipient);
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
		OOCSIClient sender = new OOCSIClient(clientNameSender);
		sender.connect("localhost", 4444);
		new OOCSIMessage(sender, "mychannel").data("mykey", "myvalue").send();

		// assertions
		Thread.sleep(200);
		OOCSIEvent event = events.get(0);
		assertEquals(clientNameSender, event.getSender());
		assertEquals(clientNameRecipient, event.getRecipient());
		assertEquals("mychannel", event.getChannel());
		assertTrue(System.currentTimeMillis() - 300 < event.getTime());
	}
}
