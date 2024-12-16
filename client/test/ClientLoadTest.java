import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import nl.tue.id.oocsi.client.OOCSIClient;
import nl.tue.id.oocsi.client.protocol.DataHandler;
import nl.tue.id.oocsi.client.protocol.OOCSIMessage;

public class ClientLoadTest {

	@Test
	public void testSendReceive() throws InterruptedException {
		final AtomicInteger ai1 = new AtomicInteger(), ai2 = new AtomicInteger();

		OOCSIClient o1 = new OOCSIClient("test_client_send_receive_1");
		o1.connect("localhost", 4444);
		assertTrue(o1.isConnected());
		o1.subscribe(new DataHandler() {
			public void receive(String sender, Map<String, Object> data, long timestamp) {
				ai2.incrementAndGet();
			}
		});

		OOCSIClient o2 = new OOCSIClient("test_client_send_receive_2");
		o2.connect("localhost", 4444);
		assertTrue(o2.isConnected());
		o2.subscribe(new DataHandler() {
			public void receive(String sender, Map<String, Object> data, long timestamp) {
				ai1.incrementAndGet();
			}
		});

		for (int i = 0; i < 500; i++) {
			o1.send("test_client_send_receive_2", "hello " + i);
			Thread.sleep(10);
		}

		Thread.sleep(100);
		assertEquals(500, ai1.get());

		for (int i = 0; i < 500; i++) {
			o2.send("test_client_send_receive_1", "hello1");
			Thread.sleep(10);
		}

		Thread.sleep(100);
		assertEquals(500, ai2.get());

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

		int[][] largePayload = new int[100][10];
		for (int i = 0; i < 100; i++) {
			largePayload[i] = new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0 };
		}
		new OOCSIMessage(o1, "test_client_big_message_2").data("load", largePayload).send();

		Thread.yield();
		Thread.sleep(400);

		assertEquals(1, list.size());
		assertEquals(100, ((List) list.get(0)).size());
		assertEquals(3l, ((List) ((List) list.get(0)).get(0)).get(2));

		o1.disconnect();
		o2.disconnect();
	}

}
