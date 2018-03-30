import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import nl.tue.id.oocsi.OOCSIEvent;
import nl.tue.id.oocsi.client.OOCSIClient;
import nl.tue.id.oocsi.client.protocol.DataHandler;
import nl.tue.id.oocsi.client.protocol.Handler;
import nl.tue.id.oocsi.client.protocol.RateLimitedClientEventHandler;
import nl.tue.id.oocsi.client.protocol.RateLimitedEventHandler;

public class ClientConnectionTest {

	@Test
	public void testConnectionToServer() {
		OOCSIClient o = new OOCSIClient("test_client");

		o.connect("localhost", 4444);

		assertTrue(o.isConnected());

		o.disconnect();
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

		o.disconnect();
	}

	@Test
	public void testConnectAndKill() throws InterruptedException {
		OOCSIClient o = new OOCSIClient("test_client_0_kill");

		o.setReconnect(false);
		o.connect("localhost", 4444);

		assertTrue(o.isConnected());

		Thread.sleep(500);

		{
			OOCSIClient o1 = new OOCSIClient("test_client_0_kill");
			o1.connect("localhost", 4444);

			Thread.sleep(500);

			assertTrue(o1.isConnected());
			o1.disconnect();
		}

		o.disconnect();
	}

	@Test
	public void testConnectAndKillWithPassword() throws InterruptedException {
		OOCSIClient o1 = new OOCSIClient("test_priv_client_1:12345");
		o1.connect("localhost", 4444);
		assertTrue(o1.isConnected());

		OOCSIClient o2 = new OOCSIClient("test_priv_client_1");
		o2.connect("localhost", 4444);
		assertTrue(!o2.isConnected());

		OOCSIClient o3 = new OOCSIClient("test_priv_client_1:345");
		o3.connect("localhost", 4444);
		assertTrue(!o3.isConnected());

		OOCSIClient o4 = new OOCSIClient("test_priv_client_1:12345");
		o4.connect("localhost", 4444);
		assertTrue(o4.isConnected());

		o1.disconnect();
		o2.disconnect();
		o3.disconnect();
		o4.disconnect();

	}

	@Test
	public void testConnectReconnect() throws InterruptedException {
		OOCSIClient o = new OOCSIClient("test_client_0_reconnect") {
			// @Override
			// public void log(String message) {
			// System.out.println(message);
			// }
		};

		o.setReconnect(true);
		o.connect("localhost", 4444);

		Thread.sleep(2000);

		assertTrue(o.isConnected());

		Thread.sleep(500);

		o.reconnect();

		assertTrue(!o.isConnected());

		Thread.sleep(500);

		assertTrue(o.isConnected());

		o.setReconnect(false);
		o.reconnect();

		assertTrue(!o.isConnected());

		Thread.sleep(500);

		assertTrue(!o.isConnected());

		o.disconnect();
	}

	@Test
	public void testConnectReconnectSubscriptions() throws InterruptedException {
		final List<String> list = new ArrayList<String>();

		OOCSIClient o = new OOCSIClient("test_client_0_reconnect_subscriptions1") {
			// @Override
			// public void log(String message) {
			// System.out.println(message);
			// }
		};
		;
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

		OOCSIClient o2 = new OOCSIClient("test_client_0_reconnect_subscriptions2");
		o2.connect("localhost", 4444);
		assertTrue(o2.isConnected());
		o2.send("subscriptionTest", "some unimportant data");

		Thread.sleep(500);
		assertTrue(!list.isEmpty());

		list.clear();

		Thread.sleep(500);

		o.reconnect();

		assertTrue(!o.isConnected());

		Thread.sleep(500);

		assertTrue(o.isConnected());

		OOCSIClient o3 = new OOCSIClient("test_client_0_reconnect_subscriptions3");
		o3.connect("localhost", 4444);
		assertTrue(o3.isConnected());
		o3.send("subscriptionTest", "some unimportant data");

		Thread.sleep(500);
		assertTrue(!list.isEmpty());

		o.disconnect();
		o2.disconnect();
		o3.disconnect();
	}

	@Test
	public void testSendReceive() throws InterruptedException {
		final List<String> list = new ArrayList<String>();

		OOCSIClient o1 = new OOCSIClient("test_client_1r");
		o1.connect("localhost", 4444);
		assertTrue(o1.isConnected());
		o1.subscribe(new DataHandler() {
			public void receive(String sender, Map<String, Object> data, long timestamp) {
				list.add(sender);
			}
		});

		OOCSIClient o2 = new OOCSIClient("test_client_2r");
		o2.connect("localhost", 4444);
		assertTrue(o2.isConnected());
		o2.subscribe(new DataHandler() {
			public void receive(String sender, Map<String, Object> data, long timestamp) {
				list.add(sender);
			}
		});

		o1.send("test_client_2r", "hello2");
		Thread.yield();
		Thread.sleep(3000);

		assertEquals(1, list.size());
		assertEquals("test_client_1r", list.get(0));

		o2.send("test_client_1r", "hello1");
		Thread.yield();
		Thread.sleep(3000);

		assertEquals(2, list.size());
		assertEquals("test_client_2r", list.get(1));

		o1.disconnect();
		o2.disconnect();
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

		o1.disconnect();
		o2.disconnect();
	}

	@Test
	public void testRateLimit() throws InterruptedException {
		final List<String> list = new ArrayList<String>();

		OOCSIClient o1 = new OOCSIClient("test_client_rate_limit_1");
		o1.connect("localhost", 4444);
		assertTrue(o1.isConnected());
		o1.subscribe(new RateLimitedEventHandler(5, 3) {
			public void receive(OOCSIEvent event) {
				list.add(event.getSender());
			}
		});

		OOCSIClient o2 = new OOCSIClient("test_client_rate_limit_2");
		o2.connect("localhost", 4444);
		assertTrue(o2.isConnected());
		o2.subscribe(new RateLimitedEventHandler(2, 3) {
			public void receive(OOCSIEvent event) {
				list.add(event.getSender());
			}
		});

		assertEquals(0, list.size());

		// appear
		o1.send("test_client_rate_limit_2", "hello1y");
		o1.send("test_client_rate_limit_2", "hello2y");
		// won't appear
		o1.send("test_client_rate_limit_2", "hello3n");
		Thread.sleep(1000);

		assertEquals(2, list.size());

		// appear all
		o2.send("test_client_rate_limit_1", "hello4y");
		o2.send("test_client_rate_limit_1", "hello5y");
		o2.send("test_client_rate_limit_1", "hello6y");
		Thread.sleep(1000);

		assertEquals(5, list.size());

		// won't appear
		o1.send("test_client_rate_limit_2", "hello7n");
		o1.send("test_client_rate_limit_2", "hello8n");
		o1.send("test_client_rate_limit_2", "hello9n");
		// appear
		o2.send("test_client_rate_limit_1", "hello10y");
		o2.send("test_client_rate_limit_1", "hello11y");
		// won't appear
		o2.send("test_client_rate_limit_1", "hello12n");
		Thread.sleep(500);

		assertEquals(7, list.size());

		// wait for timeout reset
		Thread.sleep(500);

		// appear
		o1.send("test_client_rate_limit_2", "hello13y");
		o1.send("test_client_rate_limit_2", "hello14y");
		// won't appear
		o1.send("test_client_rate_limit_2", "hello15n");

		// appear
		o2.send("test_client_rate_limit_1", "hello16y");
		o2.send("test_client_rate_limit_1", "hello17y");
		o2.send("test_client_rate_limit_1", "hello18y");
		Thread.sleep(500);

		for (String string : list) {
			System.out.println(string);
		}

		assertEquals(12, list.size());

		o1.disconnect();
		o2.disconnect();
	}

	@Test
	public void testRateLimitPerClient() throws InterruptedException {
		final List<String> list = new ArrayList<String>();

		OOCSIClient o1 = new OOCSIClient("test_client_rate_limit_pc_1");
		o1.connect("localhost", 4444);
		assertTrue(o1.isConnected());
		o1.subscribe(new RateLimitedClientEventHandler(2, 3) {
			public void receive(OOCSIEvent event) {
				list.add(event.getSender());
			}
		});

		OOCSIClient o2 = new OOCSIClient("test_client_rate_limit_pc_2");
		o2.connect("localhost", 4444);
		assertTrue(o2.isConnected());

		OOCSIClient o3 = new OOCSIClient("test_client_rate_limit_pc_3");
		o3.connect("localhost", 4444);
		assertTrue(o3.isConnected());

		assertEquals(0, list.size());

		// appear
		o2.send("test_client_rate_limit_pc_1", "hello1y");
		o2.send("test_client_rate_limit_pc_1", "hello2y");
		// won't appear
		o2.send("test_client_rate_limit_pc_1", "hello3n");
		Thread.sleep(1500);

		assertEquals(2, list.size());

		// appear all
		o3.send("test_client_rate_limit_pc_1", "hello4y");
		o3.send("test_client_rate_limit_pc_1", "hello5y");
		// won't appear
		o3.send("test_client_rate_limit_pc_1", "hello6n");
		Thread.sleep(500);

		assertEquals(4, list.size());

		// won't appear
		o2.send("test_client_rate_limit_pc_1", "hello7n");
		o2.send("test_client_rate_limit_pc_1", "hello8n");
		o2.send("test_client_rate_limit_pc_1", "hello9n");
		o3.send("test_client_rate_limit_pc_1", "hello10n");
		o3.send("test_client_rate_limit_pc_1", "hello11n");
		o3.send("test_client_rate_limit_pc_1", "hello12n");
		Thread.sleep(500);

		assertEquals(4, list.size());

		// wait for timeout reset
		Thread.sleep(1500);

		// appear
		o2.send("test_client_rate_limit_pc_1", "hello13y");
		o2.send("test_client_rate_limit_pc_1", "hello14y");
		// won't appear
		o2.send("test_client_rate_limit_pc_1", "hello15n");

		// appear
		o3.send("test_client_rate_limit_pc_1", "hello16y");
		o3.send("test_client_rate_limit_pc_1", "hello17y");
		// won't appear
		o3.send("test_client_rate_limit_pc_1", "hello18n");
		Thread.sleep(500);

		for (String string : list) {
			System.out.println(string);
		}
		assertEquals(8, list.size());

		o1.disconnect();
		o2.disconnect();
		o3.disconnect();
	}

	@Test
	public void testPasswordProtectedClient() throws InterruptedException {
		final List<String> list = new ArrayList<String>();

		OOCSIClient o1 = new OOCSIClient("test_priv_client_1:12345");
		o1.connect("localhost", 4444);
		assertTrue(o1.isConnected());
		o1.subscribe(new DataHandler() {
			public void receive(String sender, Map<String, Object> data, long timestamp) {
				list.add(sender);
			}
		});

		OOCSIClient o2 = new OOCSIClient("test_priv_client_2");
		o2.connect("localhost", 4444);
		assertTrue(o2.isConnected());
		o2.subscribe(new DataHandler() {
			public void receive(String sender, Map<String, Object> data, long timestamp) {
				list.add(sender);
			}
		});

		assertEquals(0, list.size());

		o2.send("test_priv_client_1", "hello1");
		Thread.yield();
		Thread.sleep(1000);

		assertEquals(1, list.size());
		assertEquals("test_priv_client_2", list.get(0));

		o1.disconnect();
		o2.disconnect();
	}

	@Test
	public void testPrivateChannel() throws InterruptedException {
		final List<String> list = new ArrayList<String>();

		OOCSIClient o1 = new OOCSIClient("test_client_private_1");
		o1.connect("localhost", 4444);
		assertTrue(o1.isConnected());
		o1.subscribe("test:password", new DataHandler() {
			public void receive(String sender, Map<String, Object> data, long timestamp) {
				list.add(sender);
			}
		});

		OOCSIClient o2 = new OOCSIClient("test_client_private_2");
		o2.connect("localhost", 4444);
		assertTrue(o2.isConnected());
		o2.subscribe("test", new DataHandler() {
			public void receive(String sender, Map<String, Object> data, long timestamp) {
				list.add(sender);
			}
		});

		OOCSIClient o3 = new OOCSIClient("test_client_private_3");
		o3.connect("localhost", 4444);
		assertTrue(o3.isConnected());

		// baseline
		assertEquals(0, list.size());

		// test without password
		o3.send("test", "hello1");
		Thread.sleep(100);
		assertEquals(0, list.size());

		// test wrong pass 1
		o3.send("test:pass", "hello1");
		Thread.sleep(100);
		assertEquals(0, list.size());

		// test wrong pass 2
		o3.send("test:passwwwwwooooorrrrddd", "hello1");
		Thread.sleep(100);
		assertEquals(0, list.size());

		// test wrong pass 3
		o3.send("test:password1", "hello1");
		Thread.sleep(100);
		assertEquals(0, list.size());

		// test correct password
		o3.send("test:password", "hello1");
		Thread.sleep(100);
		assertEquals(1, list.size());

		o1.disconnect();
		o2.disconnect();
	}

	// @Test
	// public void testConnectToMulticastServer() throws InterruptedException {
	// OOCSIClient o = new OOCSIClient("test_client_0_multicast");
	//
	// o.connect();
	//
	// assertTrue(o.isConnected());
	//
	// Thread.sleep(500);
	//
	// o.disconnect();
	//
	// Thread.sleep(500);
	//
	// assertTrue(!o.isConnected());
	// }

}
