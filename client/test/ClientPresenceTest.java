import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.junit.Test;

import nl.tue.id.oocsi.client.OOCSIClient;
import nl.tue.id.oocsi.client.protocol.DataHandler;

public class ClientPresenceTest {

	@Test
	public void testClientPresence() throws InterruptedException {
		final List<String> list = new Vector<String>();

		OOCSIClient o1 = new OOCSIClient("test_client_presence_1");
		o1.connect("localhost", 4444);
		assertTrue(o1.isConnected());

		// baseline
		assertEquals(0, list.size());

		o1.subscribe("presence(test_client_presence_2)", new DataHandler() {
			public void receive(String sender, Map<String, Object> data, long timestamp) {
				String dataAdd = sender + data.toString();
				if (!dataAdd.contains("refresh")) {
					list.add(dataAdd);
				}
			}
		});

		Thread.sleep(100);

		// test _client_presence_2 is not yet registered, so absent
		assertEquals(0, list.size());

		OOCSIClient o2 = new OOCSIClient("test_client_presence_2");
		o2.connect("localhost", 4444);
		assertTrue(o2.isConnected());

		Thread.sleep(100);

		assertEquals(1, list.size());
		assertTrue(list.get(0).contains("join"));

		o2.disconnect();

		Thread.sleep(100);

		assertEquals(2, list.size());

		// allow for two orderings
		assertTrue(list.get(1).contains("leave"));
		o1.disconnect();
	}

	@Test
	public void testChannelPresence() throws InterruptedException {
		final List<String> list = new Vector<String>();

		OOCSIClient o1 = new OOCSIClient("test_channel_presence_1");
		o1.connect("localhost", 4444);
		assertTrue(o1.isConnected());

		o1.subscribe("presence(test_presence)", new DataHandler() {
			public void receive(String sender, Map<String, Object> data, long timestamp) {
				String string = data.toString();
				// exclude refresh information
				if (!string.contains("refresh")) {
					list.add(sender + string);
				}
			}
		});

		Thread.sleep(100);

		// baseline
		assertEquals(0, list.size());

		OOCSIClient o2 = new OOCSIClient("test_channel_presence_2");
		o2.connect("localhost", 4444);
		assertTrue(o2.isConnected());

		Thread.sleep(200);

		assertEquals(0, list.size());

		o2.subscribe("test_presence", null);

		Thread.sleep(500);

		assertEquals(1, list.size());

		o2.subscribe("test_presence_wrong", null);

		Thread.sleep(100);

		assertEquals(1, list.size());

		o2.unsubscribe("test_presence");

		Thread.sleep(100);

		o2.disconnect();

		Thread.sleep(100);

		o1.disconnect();

		assertEquals(2, list.size());
	}

	@Test
	public void testChannelPresenceSubscription() throws InterruptedException {
		final List<String> list = new Vector<String>(2);

		OOCSIClient o1 = new OOCSIClient("test_presence_subscription_1");
		OOCSIClient o2 = new OOCSIClient("test_presence_subscription_2");

		try {
			o1.connect("localhost", 4444);
			assertTrue(o1.isConnected());

			o2.connect("localhost", 4444);
			assertTrue(o2.isConnected());

			o1.subscribe("presence(something_new)", new DataHandler() {
				public void receive(String sender, Map<String, Object> data, long timestamp) {
					String dataAdd = sender + data.toString();
					if (!dataAdd.contains("refresh")) {
						list.add(dataAdd);
					}
				}
			});

			Thread.sleep(200);

			o2.subscribe("something_new", new DataHandler() {
				public void receive(String sender, Map<String, Object> data, long timestamp) {
				}
			});

			Thread.sleep(200);

			// one event captured?
			assertEquals(1, list.size());
			assertTrue(list.get(0).contains("join"));

			o2.unsubscribe("something_new");

			Thread.sleep(100);

			// two events captured?
			assertEquals(2, list.size());
			assertTrue(list.get(1).contains("leave"));
		} finally {
			o1.disconnect();
			o2.disconnect();
		}
	}

	@Test
	public void testChannelPresenceSubscription2() throws InterruptedException {
		final List<String> list = new Vector<String>();

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
				String dataAdd = sender + data.toString();
				if (!dataAdd.contains("refresh")) {
					list.add(dataAdd);
				}
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
		assertEquals(3, list.size());

		assertTrue(list.get(2).contains("leave"));

		o1.disconnect();
	}

	@Test
	public void testClientPresenceSubscription() throws InterruptedException {
		final List<String> list = new Vector<String>();

		OOCSIClient o1 = new OOCSIClient("test_presence_subscription_3");
		o1.connect("localhost", 4444);
		assertTrue(o1.isConnected());

		o1.subscribe("presence(test_presence_subscription_4)", new DataHandler() {
			public void receive(String sender, Map<String, Object> data, long timestamp) {
				String dataAdd = sender + data.toString();
				if (!dataAdd.contains("refresh")) {
					list.add(dataAdd);
				}
			}
		});

		OOCSIClient o2 = new OOCSIClient("test_presence_subscription_4");
		o2.connect("localhost", 4444);
		assertTrue(o2.isConnected());

		Thread.sleep(500);

		// two events captured?
		assertEquals(1, list.size());

		assertTrue(list.get(0).contains("join"));

		o2.disconnect();
		Thread.sleep(200);

		// two more event captured?
		assertEquals(2, list.size());

		// allow for two orderings
		assertTrue(list.get(1).contains("leave"));

		o1.disconnect();
	}

	@Test
	public void testClientPresenceRefresh() throws InterruptedException {
		final List<String> listJoin = new Vector<String>();
		final List<String> listRefresh = new Vector<String>();

		OOCSIClient o1 = new OOCSIClient("test_presence_subscription_5");
		o1.connect("localhost", 4444);
		assertTrue(o1.isConnected());

		o1.subscribe("presence(test_presence_subscription_6)", new DataHandler() {
			public void receive(String sender, Map<String, Object> data, long timestamp) {
				String dataAdd = sender + data.toString();
				if (!dataAdd.contains("refresh")) {
					listJoin.add(dataAdd);
				} else {
					listRefresh.add(dataAdd);
				}
				System.err.println(dataAdd);
			}
		});

		OOCSIClient o2 = new OOCSIClient("test_presence_subscription_6");
		o2.connect("localhost", 4444);
		assertTrue(o2.isConnected());

		Thread.sleep(200);

		// two events captured?
		assertEquals(1, listJoin.size());
		assertTrue(listJoin.get(0).contains("join"));

		Thread.sleep(6000);

		// two more event captured?
		assertTrue(listRefresh.size() > 0);
		assertTrue(listRefresh.get(0).contains("refresh"));

		o1.disconnect();
		o2.disconnect();
	}

}
