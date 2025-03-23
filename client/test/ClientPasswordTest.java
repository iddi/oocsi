import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.junit.Test;

import nl.tue.id.oocsi.client.OOCSIClient;
import nl.tue.id.oocsi.client.protocol.DataHandler;

public class ClientPasswordTest {

	@Test
	public void testConnectAndKillWithPassword() throws InterruptedException {
		OOCSIClient o1 = new OOCSIClient("test_priv_client_1:12345");
		o1.connect("localhost", 4444);
		Thread.sleep(200);
		assertTrue(o1.isConnected());

		OOCSIClient o2 = new OOCSIClient("test_priv_client_1");
		o2.connect("localhost", 4444);
		Thread.sleep(200);
		assertTrue(!o2.isConnected());

		OOCSIClient o3 = new OOCSIClient("test_priv_client_1:345");
		o3.connect("localhost", 4444);
		Thread.sleep(200);
		assertTrue(!o3.isConnected());

		OOCSIClient o4 = new OOCSIClient("test_priv_client_1:12345");
		o4.connect("localhost", 4444);
		Thread.sleep(200);
		assertTrue(!o4.isConnected());

		o1.disconnect();
		o2.disconnect();
		o3.disconnect();
		o4.disconnect();

		OOCSIClient o5 = new OOCSIClient("test_priv_client_1:12345");
		o5.connect("localhost", 4444);
		Thread.sleep(200);
		assertTrue(o5.isConnected());
		o5.disconnect();
	}

	@Test
	public void testPasswordProtectedClient() throws InterruptedException {
		final List<String> list = new Vector<String>();

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

}
