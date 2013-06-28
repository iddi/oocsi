import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.tue.id.oocsi.client.OOCSIClient;
import nl.tue.id.oocsi.client.protocol.DataHandler;

import org.junit.Test;

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
	public void testSendReceive() throws InterruptedException {
		final List<String> list = new ArrayList<String>();

		OOCSIClient o1 = new OOCSIClient("test_client_1");
		o1.connect("localhost", 4444);
		assertTrue(o1.isConnected());
		o1.subscribe(new DataHandler() {
			public void receive(String sender, Map<String, Object> data,
					String timestamp) {
				list.add((String) data.get("data"));
			}
		});

		OOCSIClient o2 = new OOCSIClient("test_client_2");
		o2.connect("localhost", 4444);
		assertTrue(o2.isConnected());
		o2.subscribe(new DataHandler() {
			public void receive(String sender, Map<String, Object> data,
					String timestamp) {
				list.add((String) data.get("data"));
			}
		});

		o1.send("test_client_2", "hello2");
		Thread.yield();
		Thread.sleep(3000);

		assertEquals(1, list.size());
		assertEquals(list.get(0), "hello2");

		o2.send("test_client_1", "hello1");
		Thread.yield();
		Thread.sleep(3000);

		assertEquals(2, list.size());
		assertEquals(list.get(1), "hello1");

	}

	@Test
	public void testSendReceive2() throws InterruptedException {
		final List<Long> list = new ArrayList<Long>();

		OOCSIClient o1 = new OOCSIClient("test_client_3");
		o1.connect("localhost", 4444);
		assertTrue(o1.isConnected());
		o1.subscribe(new DataHandler() {
			public void receive(String sender, Map<String, Object> data,
					String timestamp) {
				list.add((Long) data.get("data"));
			}
		});
		Map<String, Object> map1 = new HashMap<String, Object>();
		map1.put("data", System.currentTimeMillis());

		OOCSIClient o2 = new OOCSIClient("test_client_4");
		o2.connect("localhost", 4444);
		assertTrue(o2.isConnected());
		o2.subscribe(new DataHandler() {
			public void receive(String sender, Map<String, Object> data,
					String timestamp) {
				list.add((Long) data.get("data"));
			}
		});
		Map<String, Object> map2 = new HashMap<String, Object>();
		map2.put("data", System.currentTimeMillis());

		o1.send("test_client_3", map1);
		Thread.yield();
		Thread.sleep(3000);

		assertEquals(1, list.size());
		assertEquals(list.get(0), map1.get("data"));

		o2.send("test_client_4", map2);
		Thread.yield();
		Thread.sleep(3000);

		assertEquals(2, list.size());
		assertEquals(list.get(1), map2.get("data"));

	}
}
