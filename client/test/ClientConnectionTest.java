import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import nl.tue.id.oocsi.client.OOCSIClient;
import nl.tue.id.oocsi.client.protocol.DataHandler;
import nl.tue.id.oocsi.client.protocol.Handler;

public class ClientConnectionTest {

	@Test
	public void testConnectionToServer() {
		OOCSIClient o = new OOCSIClient("test_client");

		o.connect("localhost", 4444);

		assertTrue(o.isConnected());
	}

	@Test
	public void testConnectAndDisconnect() throws InterruptedException {
		OOCSIClient o = new OOCSIClient("test_client_0");

		o.connect("localhost", 4444);

		assertTrue(o.isConnected());

		Thread.sleep(500);

		o.disconnect();

		Thread.sleep(500);

		assertTrue(!o.isConnected());
	}

	@Test
	public void testConnectToMulticastServer() throws InterruptedException {
		OOCSIClient o = new OOCSIClient("test_client_0_multicast");

		o.connect();

		assertTrue(o.isConnected());

		Thread.sleep(500);

		o.disconnect();

		Thread.sleep(500);

		assertTrue(!o.isConnected());
	}

	@Test
	public void testConnectReconnect() throws InterruptedException {
		OOCSIClient o = new OOCSIClient("test_client_0_reconnect");

		o.setReconnect(true);
		o.connect("localhost", 4444);

		Thread.sleep(2000);

		assertTrue(o.isConnected());

		Thread.sleep(500);

		o.reconnect();

		Thread.sleep(500);

		assertTrue(o.isConnected());
	}

	@Test
	public void testConnectReconnectSubscriptions() throws InterruptedException {
		final List<String> list = new ArrayList<String>();

		OOCSIClient o = new OOCSIClient("test_client_0_reconnect_subscriptions1");
		o.setReconnect(true);
		o.connect("localhost", 4444);
		o.subscribe("subscriptionTest", new Handler() {

			@Override
			public void receive(String sender, Map<String, Object> data, long timestamp, String channel,
					String recipient) {
				list.add("event received");
			}
		});

		Thread.sleep(1000);

		assertTrue(o.isConnected());

		{
			OOCSIClient o2 = new OOCSIClient("test_client_0_reconnect_subscriptions2");
			o2.connect("localhost", 4444);
			assertTrue(o2.isConnected());
			o2.send("subscriptionTest", "some unimportant data");
		}

		Thread.sleep(500);
		assertTrue(!list.isEmpty());

		list.clear();

		Thread.sleep(500);

		o.reconnect();

		Thread.sleep(500);

		assertTrue(o.isConnected());

		{
			OOCSIClient o2 = new OOCSIClient("test_client_0_reconnect_subscriptions3");
			o2.connect("localhost", 4444);
			assertTrue(o2.isConnected());
			o2.send("subscriptionTest", "some unimportant data");
		}

		Thread.sleep(500);
		assertTrue(!list.isEmpty());
	}

	@Test
	public void testSendReceive() throws InterruptedException {
		final List<String> list = new ArrayList<String>();

		OOCSIClient o1 = new OOCSIClient("test_client_1r");
		o1.connect("localhost", 4444);
		assertTrue(o1.isConnected());
		o1.subscribe(new DataHandler() {
			public void receive(String sender, Map<String, Object> data, long timestamp) {
				list.add((String) data.get("data"));
			}
		});

		OOCSIClient o2 = new OOCSIClient("test_client_2r");
		o2.connect("localhost", 4444);
		assertTrue(o2.isConnected());
		o2.subscribe(new DataHandler() {
			public void receive(String sender, Map<String, Object> data, long timestamp) {
				list.add((String) data.get("data"));
			}
		});

		o1.send("test_client_2r", "hello2");
		Thread.yield();
		Thread.sleep(3000);

		assertEquals(1, list.size());
		assertEquals(list.get(0), "hello2");

		o2.send("test_client_1r", "hello1");
		Thread.yield();
		Thread.sleep(3000);

		assertEquals(2, list.size());
		assertEquals(list.get(1), "hello1");

	}

	@Test
	public void testSendReceive2() throws InterruptedException {
		final List<Long> list = new ArrayList<Long>();

		OOCSIClient o1 = new OOCSIClient("test_client_3s");
		o1.connect("localhost", 4444);
		assertTrue(o1.isConnected());
		o1.subscribe(new DataHandler() {
			public void receive(String sender, Map<String, Object> data, long timestamp) {
				list.add((Long) data.get("data"));
			}
		});
		Map<String, Object> map1 = new HashMap<String, Object>();
		map1.put("data", System.currentTimeMillis());

		OOCSIClient o2 = new OOCSIClient("test_client_4s");
		o2.connect("localhost", 4444);
		assertTrue(o2.isConnected());
		o2.subscribe(new DataHandler() {
			public void receive(String sender, Map<String, Object> data, long timestamp) {
				list.add((Long) data.get("data"));
			}
		});
		Map<String, Object> map2 = new HashMap<String, Object>();
		map2.put("data", System.currentTimeMillis());

		o1.send("test_client_3s", map1);
		Thread.yield();
		Thread.sleep(3000);

		assertEquals(1, list.size());
		assertEquals(list.get(0), map1.get("data"));

		o2.send("test_client_4s", map2);
		Thread.yield();
		Thread.sleep(3000);

		assertEquals(2, list.size());
		assertEquals(list.get(1), map2.get("data"));

	}

	@Test
	public void testPasswordProtectedClient() throws InterruptedException {
		final List<String> list = new ArrayList<String>();

		OOCSIClient o1 = new OOCSIClient("test_priv_client_1:12345");
		o1.connect("localhost", 4444);
		assertTrue(o1.isConnected());
		o1.subscribe(new DataHandler() {
			public void receive(String sender, Map<String, Object> data, long timestamp) {
				list.add((String) data.get("data"));
			}
		});

		OOCSIClient o2 = new OOCSIClient("test_priv_client_2");
		o2.connect("localhost", 4444);
		assertTrue(o2.isConnected());
		o2.subscribe(new DataHandler() {
			public void receive(String sender, Map<String, Object> data, long timestamp) {
				list.add((String) data.get("data"));
			}
		});

		assertEquals(0, list.size());

		o2.send("test_priv_client_1", "hello1");
		Thread.yield();
		Thread.sleep(1000);

		assertEquals(1, list.size());
		assertEquals(list.get(0), "hello1");
	}

	@Test
	public void testPrivateChannel() throws InterruptedException {
		final List<String> list = new ArrayList<String>();

		OOCSIClient o1 = new OOCSIClient("test_client_1");
		o1.connect("localhost", 4444);
		assertTrue(o1.isConnected());
		o1.subscribe("test:password", new DataHandler() {
			public void receive(String sender, Map<String, Object> data, long timestamp) {
				list.add((String) data.get("data"));
			}
		});

		OOCSIClient o2 = new OOCSIClient("test_client_2");
		o2.connect("localhost", 4444);
		assertTrue(o2.isConnected());
		o2.subscribe("test", new DataHandler() {
			public void receive(String sender, Map<String, Object> data, long timestamp) {
				list.add((String) data.get("data"));
			}
		});

		OOCSIClient o3 = new OOCSIClient("test_client_3");
		o3.connect("localhost", 4444);
		assertTrue(o3.isConnected());

		// baseline
		assertEquals(0, list.size());

		// test without password
		o3.send("test", "hello1");
		Thread.yield();
		Thread.sleep(1000);
		assertEquals(0, list.size());

		// test wrong pass 1
		o3.send("test:pass", "hello1");
		Thread.yield();
		Thread.sleep(1000);
		assertEquals(0, list.size());

		// test wrong pass 2
		o3.send("test:passwwwwwooooorrrrddd", "hello1");
		Thread.yield();
		Thread.sleep(1000);
		assertEquals(0, list.size());

		// test wrong pass 3
		o3.send("test:password1", "hello1");
		Thread.yield();
		Thread.sleep(1000);
		assertEquals(0, list.size());

		// test correct password
		o3.send("test:password", "hello1");
		Thread.yield();
		Thread.sleep(1000);
		assertEquals(1, list.size());
		assertEquals(list.get(0), "hello1");
	}

}
