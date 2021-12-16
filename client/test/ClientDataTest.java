import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import nl.tue.id.oocsi.OOCSIEvent;
import nl.tue.id.oocsi.client.OOCSIClient;
import nl.tue.id.oocsi.client.protocol.OOCSIMessage;

/**
 * call / responder test cases
 *
 * @author matsfunk
 */
public class ClientDataTest {

	@Test
	public void testStringData() throws InterruptedException {
		final List<OOCSIEvent> events = new ArrayList<OOCSIEvent>();

		OOCSIClient o1 = new OOCSIClient("string1");
		o1.connect("localhost", 4444);
		assertTrue(o1.isConnected());

		OOCSIClient o2 = new OOCSIClient("string2");
		o2.connect("localhost", 4444);
		assertTrue(o2.isConnected());
		o2.subscribe("stringTesting", new nl.tue.id.oocsi.client.protocol.EventHandler() {
			@Override
			public void receive(OOCSIEvent event) {
				events.add(event);
			}
		});

		Thread.sleep(100);

		{
			new OOCSIMessage(o1, "stringTesting").data("string1", "This is a string with spaces.").send();
			Thread.sleep(150);
			assertEquals(1, events.size());
			events.clear();
		}
	}

	@Test
	public void testArrayData() throws InterruptedException {

		final List<OOCSIEvent> events = new ArrayList<OOCSIEvent>();

		OOCSIClient o1 = new OOCSIClient("array1");
		o1.connect("localhost", 4444);
		assertTrue(o1.isConnected());

		OOCSIClient o2 = new OOCSIClient("array2");
		o2.connect("localhost", 4444);
		assertTrue(o2.isConnected());
		o2.subscribe("arrayTesting", new nl.tue.id.oocsi.client.protocol.EventHandler() {
			@Override
			public void receive(OOCSIEvent event) {
				events.add(event);
			}
		});

		// test boolean array
		{
			boolean[] ba = { true, false, false, true };
			new OOCSIMessage(o1, "arrayTesting").data("array", ba).send();
			Thread.sleep(150);
			assertEquals(1, events.size());
			boolean[] bb = events.get(0).getBooleanArray("array", new boolean[] {});
			assertArrayEquals(ba, bb);
			events.clear();
		}

		// test int array
		{
			int[] ba = { 1, 2, 3, -1, -2, -3 };
			new OOCSIMessage(o1, "arrayTesting").data("array", ba).send();
			Thread.sleep(150);
			assertEquals(1, events.size());
			int[] bb = events.get(0).getIntArray("array", new int[] {});
			assertArrayEquals(ba, bb);
			events.clear();
		}

		// test float array
		{
			float[] ba = { 1.1f, 2.9f, 3.12345f, -1.43f, -2.211111f, -3.58809f };
			new OOCSIMessage(o1, "arrayTesting").data("array", ba).send();
			Thread.sleep(150);
			assertEquals(1, events.size());
			float[] bb = events.get(0).getFloatArray("array", new float[] {});
			assertArrayEquals(ba, bb, 0);
			events.clear();
		}

		// test double array
		{
			double[] ba = { 1.1d, 2.9d, 3.12345d, -1.43d, -2.211111d, -3.58809d, Math.PI };
			new OOCSIMessage(o1, "arrayTesting").data("array", ba).send();
			Thread.sleep(150);
			assertEquals(1, events.size());
			double[] bb = events.get(0).getDoubleArray("array", new double[] {});
			assertArrayEquals(ba, bb, 0.001);
			events.clear();
		}

		// test String array
		{
			String[] ba = { "hello", "world", "with", "", "🦆" };
			new OOCSIMessage(o1, "arrayTesting").data("array", ba).send();
			Thread.sleep(150);
			assertEquals(1, events.size());
			String[] bb = events.get(0).getStringArray("array", new String[] {});
			assertArrayEquals(ba, bb);
			events.clear();
		}

		o1.disconnect();
		o2.disconnect();
	}
}
