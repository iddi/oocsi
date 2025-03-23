import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.junit.Test;

import nl.tue.id.oocsi.client.OOCSIClient;
import nl.tue.id.oocsi.client.protocol.DataHandler;
import nl.tue.id.oocsi.client.protocol.OOCSIMessage;

public class ClientMessageRetainDelayTest {

	@Test
	public void testRetainedMessage() throws InterruptedException {
		final List<String> list = new Vector<String>();

		OOCSIClient o1a = new OOCSIClient("test_message_retain1a");
		o1a.connect("localhost", 4444);
		assertTrue(o1a.isConnected());

		OOCSIClient o1b = new OOCSIClient("test_message_retain1b");
		o1b.connect("localhost", 4444);
		assertTrue(o1b.isConnected());
		o1b.subscribe("channel_retain1", new DataHandler() {
			public void receive(String sender, Map<String, Object> data, long timestamp) {
				// do nothing
			}
		});

		Thread.sleep(200);

		// send retained message
		new OOCSIMessage(o1a, "channel_retain1").data("_RETAIN", 10).data("test", true).send();

		Thread.sleep(200);

		// disconnect to ensure that channel would normally be closed
		o1a.disconnect();
		o1b.disconnect();

		// connect with new client
		OOCSIClient o2 = new OOCSIClient("test_message_retain2");
		o2.connect("localhost", 4444);
		assertTrue(o2.isConnected());

		// subscribe and wait
		o2.subscribe("channel_retain1", new DataHandler() {
			public void receive(String sender, Map<String, Object> data, long timestamp) {
				list.add(sender + data.toString());
			}
		});
		Thread.sleep(200);

		// check results
		assertEquals(1, list.size());
		assertTrue(list.get(0).contains("true"));
	}

	@Test
	public void testRetainedMessageTimeout() throws InterruptedException {
		final List<String> list = new Vector<String>();

		OOCSIClient o1a = new OOCSIClient("test_message_retain3a");
		o1a.connect("localhost", 4444);
		assertTrue(o1a.isConnected());

		OOCSIClient o1b = new OOCSIClient("test_message_retain3b");
		o1b.connect("localhost", 4444);
		assertTrue(o1b.isConnected());
		o1b.subscribe("channel_retain2", new DataHandler() {
			public void receive(String sender, Map<String, Object> data, long timestamp) {
				// do nothing
			}
		});

		// send retained message
		new OOCSIMessage(o1a, "channel_retain2").data("_RETAIN", 1).data("test", true).send();

		// disconnect to ensure that channel would normally be closed
		o1a.disconnect();
		o1b.disconnect();
		Thread.sleep(1100);

		// connect with new client
		OOCSIClient o2 = new OOCSIClient("test_message_retain4");
		o2.connect("localhost", 4444);
		assertTrue(o2.isConnected());

		// subscribe and wait
		o2.subscribe("channel_retain2", new DataHandler() {
			public void receive(String sender, Map<String, Object> data, long timestamp) {
				list.add(sender + data.toString());
			}
		});
		Thread.sleep(200);

		// check that no retained message was received
		assertEquals(0, list.size());
	}

	@Test
	public void testDelayedMessage() throws InterruptedException {
		final List<String> list = new Vector<String>();

		OOCSIClient o1 = new OOCSIClient("test_message_delay1");
		o1.connect("localhost", 4444);
		assertTrue(o1.isConnected());

		OOCSIClient o2 = new OOCSIClient("test_message_delay2");
		o2.connect("localhost", 4444);
		assertTrue(o2.isConnected());
		o2.subscribe("channel_delay1", new DataHandler() {
			public void receive(String sender, Map<String, Object> data, long timestamp) {
				list.add(sender + data.toString());
			}
		});

		// wait briefly
		Thread.sleep(100);

		// send retained message
		new OOCSIMessage(o1, "channel_delay1").data("_DELAY", 1).data("test", true).send();

		// check that no retained message was received
		assertEquals(0, list.size());

		// wait for at least 1sec, better a little longer because the status task might need more time to run through
		Thread.sleep(2000);

		// check that no retained message was received
		assertEquals(1, list.size());

		o1.disconnect();
		o2.disconnect();
	}

	@Test
	public void testDelayedMessageBeforeRecipientExists() throws InterruptedException {
		final List<String> list = new Vector<String>();

		OOCSIClient o1 = new OOCSIClient("test_message_delay1b");
		o1.connect("localhost", 4444);
		assertTrue(o1.isConnected());

		// send retained message
		new OOCSIMessage(o1, "channel_delay2").data("_DELAY", 1).data("test", true).send();

		OOCSIClient o2 = new OOCSIClient("test_message_delay2b");
		o2.connect("localhost", 4444);
		assertTrue(o2.isConnected());

		o2.subscribe("channel_delay2", new DataHandler() {
			public void receive(String sender, Map<String, Object> data, long timestamp) {
				list.add(sender + data.toString());
			}
		});

		// wait briefly
		Thread.sleep(100);

		// check that no retained message was received
		assertEquals(0, list.size());

		// wait for at least 1sec, better a little longer because the status task might need more time to run through
		Thread.sleep(2000);

		// check that no retained message was received
		assertEquals(1, list.size());

		o1.disconnect();
		o2.disconnect();
	}

	@Test
	public void testDelayedMessageAfterSenderDisconnects() throws InterruptedException {
		final List<String> list = new Vector<String>();

		OOCSIClient o1 = new OOCSIClient("test_message_delay1c");
		o1.connect("localhost", 4444);
		assertTrue(o1.isConnected());

		// send retained message
		new OOCSIMessage(o1, "channel_delay3").data("_DELAY", 1).data("test", true).send();

		// wait briefly
		Thread.sleep(100);

		o1.disconnect();

		OOCSIClient o2 = new OOCSIClient("test_message_delay2c");
		o2.connect("localhost", 4444);
		assertTrue(o2.isConnected());
		o2.subscribe("channel_delay3", new DataHandler() {
			public void receive(String sender, Map<String, Object> data, long timestamp) {
				list.add(sender + data.toString());
			}
		});

		// wait briefly
		Thread.sleep(100);

		// check that no retained message was received
		assertEquals(0, list.size());

		// wait for at least 1sec, better a little longer because the status task might need more time to run through
		Thread.sleep(2000);

		assertEquals(1, list.size());
		o2.disconnect();
	}

}
