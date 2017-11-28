import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import nl.tue.id.oocsi.client.OOCSIClient;
import nl.tue.id.oocsi.client.protocol.DataHandler;
import nl.tue.id.oocsi.client.protocol.OOCSIMessage;

import org.junit.Test;

public class ClientLoadTest {

	@Test
	public void testConnectionToServer() {
		OOCSIClient o = new OOCSIClient("test_client_connection_to_server");

		o.connect("localhost", 4444);

		assertTrue(o.isConnected());

		o.disconnect();
	}

	@Test
	public void testSendReceive() throws InterruptedException {
		final List<String> list = new ArrayList<String>();

		OOCSIClient o1 = new OOCSIClient("test_client_send_receive_1");
		o1.connect("localhost", 4444);
		assertTrue(o1.isConnected());
		o1.subscribe(new DataHandler() {
			public void receive(String sender, Map<String, Object> data, long timestamp) {
				list.add((String) data.get("data"));
			}
		});

		OOCSIClient o2 = new OOCSIClient("test_client_send_receive_2");
		o2.connect("localhost", 4444);
		assertTrue(o2.isConnected());
		o2.subscribe(new DataHandler() {
			public void receive(String sender, Map<String, Object> data, long timestamp) {
				list.add((String) data.get("data"));
			}
		});

		for (int i = 0; i < 1000; i++) {
			o1.send("test_client_send_receive_2", "hello " + i);
		}
		Thread.yield();
		Thread.sleep(6000);

		assertEquals(1000, list.size());
		assertEquals(list.get(0), "hello 0");

		list.clear();
		for (int i = 0; i < 1000; i++) {
			o2.send("test_client_send_receive_1", "hello1");
		}
		Thread.yield();
		Thread.sleep(3000);

		assertEquals(1000, list.size());
		assertEquals(list.get(1), "hello1");

		o1.disconnect();
		o2.disconnect();
	}

	@Test
	public void testSendingBigMessages() throws InterruptedException {
		final List<Object> list = new ArrayList<Object>();

		OOCSIClient o1 = new OOCSIClient("test_client_big_message_1");
		o1.connect("localhost", 4444);
		assertTrue(o1.isConnected());
		o1.subscribe(new DataHandler() {
			public void receive(String sender, Map<String, Object> data, long timestamp) {
				list.add((String) data.get("data"));
			}
		});

		OOCSIClient o2 = new OOCSIClient("test_client_big_message_2");
		o2.connect("localhost", 4444);
		assertTrue(o2.isConnected());
		o2.subscribe(new DataHandler() {
			public void receive(String sender, Map<String, Object> data, long timestamp) {
				list.add(data.get("load"));
			}
		});

		int[][] largePayload = new int[10000][10];
		for (int i = 0; i < 10000; i++) {
			largePayload[i] = new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0 };
		}
		new OOCSIMessage(o1, "test_client_big_message_2").data("load", largePayload).send();

		Thread.yield();
		Thread.sleep(1000);

		assertEquals(1, list.size());
		assertTrue(((int[][]) list.get(0)).length == 10000);
		assertEquals(3, ((int[][]) list.get(0))[0][2]);

		o1.disconnect();
		o2.disconnect();
	}

	// @Test
	// public void testSendReceive2() throws InterruptedException {
	// final List<Long> list = new ArrayList<Long>();
	//
	// OOCSI o1 = new OOCSI("test_client_3");
	// o1.connect("localhost", 4444);
	// assertTrue(o1.isConnected());
	// o1.subscribe(new DataHandler() {
	// public void receive(String sender, Map<String, Object> data,
	// String timestamp) {
	// list.add((Long) data.get("data"));
	// }
	// });
	// Map<String, Object> map1 = new HashMap<String, Object>();
	// map1.put("data", System.currentTimeMillis());
	//
	// OOCSI o2 = new OOCSI("test_client_4");
	// o2.connect("localhost", 4444);
	// assertTrue(o2.isConnected());
	// o2.subscribe(new DataHandler() {
	// public void receive(String sender, Map<String, Object> data,
	// String timestamp) {
	// list.add((Long) data.get("data"));
	// }
	// });
	// Map<String, Object> map2 = new HashMap<String, Object>();
	// map2.put("data", System.currentTimeMillis());
	//
	// o1.send("test_client_3", map1);
	// Thread.yield();
	// Thread.sleep(3000);
	//
	// assertEquals(1, list.size());
	// assertEquals(list.get(0), map1.get("data"));
	//
	// o2.send("test_client_4", map2);
	// Thread.yield();
	// Thread.sleep(3000);
	//
	// assertEquals(2, list.size());
	// assertEquals(list.get(1), map2.get("data"));
	//
	// }
}
