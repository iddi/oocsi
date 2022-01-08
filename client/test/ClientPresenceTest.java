import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import nl.tue.id.oocsi.client.OOCSIClient;
import nl.tue.id.oocsi.client.protocol.DataHandler;

public class ClientPresenceTest {

	@Test
	public void testChannelPresenceSubscription() throws InterruptedException {
		final List<String> list = new ArrayList<String>();

		OOCSIClient o1 = new OOCSIClient("test_presence_subscription_1");
		o1.connect("localhost", 4444);
		assertTrue(o1.isConnected());

		o1.subscribe("presence(something_new)", new DataHandler() {
			public void receive(String sender, Map<String, Object> data, long timestamp) {
				list.add(sender + data.toString());
			}
		});

		OOCSIClient o2 = new OOCSIClient("test_presence_subscription_2");
		o2.connect("localhost", 4444);
		assertTrue(o2.isConnected());

		o2.subscribe("something_new", new DataHandler() {
			public void receive(String sender, Map<String, Object> data, long timestamp) {
			}
		});
		Thread.sleep(200);

		// two events captured?
		assertEquals(2, list.size());

		assertTrue(list.get(0).contains("created"));
		assertTrue(list.get(1).contains("join"));

		o2.disconnect();
		Thread.sleep(200);

		// two more event captured?
		assertEquals(4, list.size());

		assertTrue(list.get(2).contains("leave"));
		assertTrue(list.get(3).contains("close"));

		o1.disconnect();
	}

	@Test
	public void testChannelPresenceSubscription2() throws InterruptedException {
		final List<String> list = new ArrayList<String>();

		OOCSIClient o2 = new OOCSIClient("test_presence_subscription_2b");
		o2.connect("localhost", 4444);
		assertTrue(o2.isConnected());

		o2.subscribe("something_new", new DataHandler() {
			public void receive(String sender, Map<String, Object> data, long timestamp) {
			}
		});
		Thread.sleep(200);

		OOCSIClient o1 = new OOCSIClient("test_presence_subscription_1b");
		o1.connect("localhost", 4444);
		assertTrue(o1.isConnected());

		o1.subscribe("presence(something_new)", new DataHandler() {
			public void receive(String sender, Map<String, Object> data, long timestamp) {
				list.add(sender + data.toString());
			}
		});

		OOCSIClient o3 = new OOCSIClient("test_presence_subscription_3b");
		o3.connect("localhost", 4444);
		assertTrue(o3.isConnected());

		o3.subscribe("something_new", new DataHandler() {
			public void receive(String sender, Map<String, Object> data, long timestamp) {
			}
		});
		Thread.sleep(200);

		assertEquals(1, list.size());

		assertTrue(list.get(0).contains("join"));

		o2.disconnect();
		Thread.sleep(200);

		// two more event captured?
		assertEquals(2, list.size());

		assertTrue(list.get(1).contains("leave"));

		o3.disconnect();
		Thread.sleep(200);

		// two more event captured?
		assertEquals(4, list.size());

		assertTrue(list.get(2).contains("leave"));
		assertTrue(list.get(3).contains("close"));

		o1.disconnect();
	}

	@Test
	public void testClientPresenceSubscription() throws InterruptedException {
		final List<String> list = new ArrayList<String>();

		OOCSIClient o1 = new OOCSIClient("test_presence_subscription_3");
		o1.connect("localhost", 4444);
		assertTrue(o1.isConnected());

		o1.subscribe("presence(test_presence_subscription_4)", new DataHandler() {
			public void receive(String sender, Map<String, Object> data, long timestamp) {
				list.add(sender + data.toString());
			}
		});

		OOCSIClient o2 = new OOCSIClient("test_presence_subscription_4");
		o2.connect("localhost", 4444);
		assertTrue(o2.isConnected());

		Thread.sleep(200);

		// two events captured?
		assertEquals(2, list.size());

		assertTrue(list.get(0).contains("created"));
		assertTrue(list.get(1).contains("join"));

		o2.disconnect();
		Thread.sleep(200);

		// two more event captured?
		assertEquals(4, list.size());

		assertTrue(list.get(2).contains("leave"));
		assertTrue(list.get(3).contains("close"));

		o1.disconnect();
	}

	@Test
	public void testClientPresenceRefresh() throws InterruptedException {
		final List<String> list = new ArrayList<String>();

		OOCSIClient o1 = new OOCSIClient("test_presence_subscription_5");
		o1.connect("localhost", 4444);
		assertTrue(o1.isConnected());

		o1.subscribe("presence(test_presence_subscription_6)", new DataHandler() {
			public void receive(String sender, Map<String, Object> data, long timestamp) {
				list.add(sender + data.toString());
			}
		});

		OOCSIClient o2 = new OOCSIClient("test_presence_subscription_6");
		o2.connect("localhost", 4444);
		assertTrue(o2.isConnected());

		Thread.sleep(200);

		// two events captured?
		assertEquals(2, list.size());

		assertTrue(list.get(0).contains("created"));
		assertTrue(list.get(1).contains("join"));

		Thread.sleep(10000);

		// two more event captured?
		assertEquals(3, list.size());
		assertTrue(list.get(2).contains("refresh"));

		o1.disconnect();
		o2.disconnect();
	}

}
