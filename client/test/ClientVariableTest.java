import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import nl.tue.id.oocsi.OOCSIFloat;
import nl.tue.id.oocsi.OOCSIInt;
import nl.tue.id.oocsi.client.OOCSIClient;
import nl.tue.id.oocsi.client.data.OOCSIVariable;

public class ClientVariableTest {

	@Test
	public void testOOCSIBoolean() throws InterruptedException {

		OOCSIClient client1 = new OOCSIClient();
		client1.connect("localhost", 4444);
		OOCSIVariable<Boolean> of11 = new OOCSIVariable<>(client1, "localVariableTestChannel", "boolean1");
		OOCSIVariable<Boolean> of12 = new OOCSIVariable<>(client1, "localVariableTestChannel", "boolean2");

		OOCSIClient client2 = new OOCSIClient();
		client2.connect("localhost", 4444);
		OOCSIVariable<Boolean> of21 = new OOCSIVariable<>(client2, "localVariableTestChannel", "boolean1");
		OOCSIVariable<Boolean> of22 = new OOCSIVariable<>(client2, "localVariableTestChannel", "boolean2");

		Thread.sleep(150);

		// initially ---

		assertNull(of21.get());
		assertNull(of22.get());

		// towards ---

		of11.set(true);
		of12.set(false);

		Thread.sleep(1000);

		assertEquals(true, of21.get());
		assertEquals(false, of22.get());

		// back ---

		of21.set(false);
		of22.set(true);

		Thread.sleep(1000);

		assertEquals(false, of11.get());
		assertEquals(true, of12.get());

		client1.disconnect();
		client2.disconnect();
	}

	@Test
	public void testOOCSIInt() throws InterruptedException {

		OOCSIClient client1 = new OOCSIClient();
		client1.connect("localhost", 4444);
		OOCSIVariable<Integer> of11 = new OOCSIInt(client1, "localVariableTestChannel1", "integer1");
		OOCSIVariable<Integer> of12 = new OOCSIInt(client1, "localVariableTestChannel1", "integer2");

		OOCSIClient client2 = new OOCSIClient();
		client2.connect("localhost", 4444);
		OOCSIVariable<Integer> of21 = new OOCSIInt(client2, "localVariableTestChannel1", "integer1", 0);
		OOCSIVariable<Integer> of22 = new OOCSIInt(client2, "localVariableTestChannel1", "integer2", 0);

		Thread.sleep(150);

		// initially ---

		assertEquals(0, (int) of21.get());
		assertEquals(0, (int) of22.get());

		// towards ---

		of11.set(1);
		of12.set(-1);

		Thread.sleep(500);

		assertEquals(1, (int) of21.get());
		assertEquals(-1, (int) of22.get());

		// back ---

		of21.set(-10);
		of22.set(10);

		Thread.sleep(500);

		assertEquals(-10, (int) of11.get());
		assertEquals(10, (int) of12.get());

		client1.disconnect();
		client2.disconnect();
	}

	@Test
	public void testOOCSIFloat() throws InterruptedException {

		OOCSIClient client1 = new OOCSIClient();
		client1.connect("localhost", 4444);
		OOCSIVariable<Float> of11 = new OOCSIFloat(client1, "localVariableTestChannel3", "float1");
		OOCSIVariable<Float> of12 = new OOCSIFloat(client1, "localVariableTestChannel3", "float2");

		OOCSIClient client2 = new OOCSIClient();
		client2.connect("localhost", 4444);
		OOCSIVariable<Float> of21 = new OOCSIFloat(client2, "localVariableTestChannel3", "float1");
		OOCSIVariable<Float> of22 = new OOCSIFloat(client2, "localVariableTestChannel3", "float2");

		Thread.sleep(150);

		// initially ---

//		assertNull(of21.get());
//		assertNull(of22.get());

		// towards ---

		of11.set(1.2f);
		of12.set(-1.2f);

		Thread.sleep(1000);

		assertEquals(1.2f, of21.get(), 0);
		assertEquals(-1.2f, of22.get(), 0);

		// back ---

		of21.set(10.2f);
		of22.set(-10.2f);

		Thread.sleep(1000);

		assertEquals(10.2f, of11.get(), 0);
		assertEquals(-10.2f, of12.get(), 0);

		client1.disconnect();
		client2.disconnect();
	}

	@Test
	public void testVariableMinMax() throws InterruptedException {

		OOCSIClient client1 = new OOCSIClient();
		client1.connect("localhost", 4444);
		OOCSIFloat of11 = new OOCSIFloat(client1, "localVariableTestChannel2", "float3").min(2f).max(6f);
		OOCSIFloat of12 = new OOCSIFloat(client1, "localVariableTestChannel2", "float4");

		OOCSIClient client2 = new OOCSIClient();
		client2.connect("localhost", 4444);
		OOCSIFloat of21 = new OOCSIFloat(client2, "localVariableTestChannel2", "float3").min(3f).max(5f);
		OOCSIFloat of22 = new OOCSIFloat(client2, "localVariableTestChannel2", "float4");

		Thread.sleep(150);

		// initially ---

		assertEquals(0f, of21.get(), 0);
		assertEquals(0f, of22.get(), 0);

		// towards ---

		of11.set(1.f);
		of12.set(-1.2f);

		Thread.sleep(500);

		assertEquals(2f, of11.get(), 0);
		assertEquals(-1.2f, of12.get(), 0);
		assertEquals(3f, of21.get(), 0);
		assertEquals(-1.2f, of22.get(), 0);

		// back ---

		of21.set(10.2f);

		Thread.sleep(500);

		assertEquals(5f, of11.get(), 0);
		assertEquals(5f, of21.get(), 0);

		// back ---

		of21.set(-10.2f);

		Thread.sleep(500);

		assertEquals(3f, of11.get(), 0);
		assertEquals(3f, of21.get(), 0);

		client1.disconnect();
		client2.disconnect();
	}

	@Test
	public void testVariableRateLimit() throws InterruptedException {
		OOCSIClient client1 = new OOCSIClient();
		client1.connect("localhost", 4444);
		OOCSIFloat of11 = new OOCSIFloat(client1, "localVariableTestChannel4", "float1").limit(5, 1);

		OOCSIClient client2 = new OOCSIClient();
		client2.connect("localhost", 4444);
		OOCSIFloat of21 = new OOCSIFloat(client2, "localVariableTestChannel4", "float1").limit(5, 1);

		Thread.sleep(150);

		// initially ---

		assertEquals(0f, of21.get(), 0);

		// five settings will go through per burst (rate 5/s)
		of11.set(1.f);
		Thread.sleep(50);
		of11.set(2.f);
		Thread.sleep(50);
		of11.set(3.f);
		Thread.sleep(50);
		of11.set(4.f);
		Thread.sleep(50);
		of11.set(5.f);
		Thread.sleep(50);
		// this one will fail
		of11.set(6.f);

		Thread.sleep(500);

		assertTrue(6f > of21.get());

		client1.disconnect();
		client2.disconnect();
	}

	@Test
	public void testVariableSmooth() throws InterruptedException {

		OOCSIClient client1 = new OOCSIClient();
		client1.connect("localhost", 4444);
		OOCSIFloat of11 = new OOCSIFloat(client1, "localVariableTestChannel5", "float1").smooth(2);
		OOCSIFloat of12 = new OOCSIFloat(client1, "localVariableTestChannel5", "float2");

		OOCSIClient client2 = new OOCSIClient();
		client2.connect("localhost", 4444);
		OOCSIFloat of21 = new OOCSIFloat(client2, "localVariableTestChannel5", "float1").smooth(2);
		OOCSIFloat of22 = new OOCSIFloat(client2, "localVariableTestChannel5", "float2");

		Thread.sleep(150);

		// initially ---

		assertEquals(0f, of21.get(), 0);
		assertEquals(0f, of22.get(), 0);

		// towards ---

		of11.set(1.f);
		Thread.sleep(150);
		of11.set(2.f);
		Thread.sleep(150);
		of12.set(-1.2f);

		Thread.sleep(500);

		assertEquals(1.5f, of21.get(), 0);
		assertEquals(-1.2f, of22.get(), 0);

		// back ---

		of21.set(10.2f);
		of21.set(10.2f);

		Thread.sleep(500);

		assertEquals(10.2f, of11.get(), 0);

		// back ---

		of21.set(-10.2f);

		Thread.sleep(100);

		of21.set(10.2f);

		Thread.sleep(100);

		assertEquals(0f, of11.get(), 0);

		client1.disconnect();
		client2.disconnect();
	}

	@Test
	public void testVariableSmoothSigma() throws InterruptedException {

		OOCSIClient client1 = new OOCSIClient();
		client1.connect("localhost", 4444);
		OOCSIFloat of11 = new OOCSIFloat(client1, "localVariableTestChannel21", "float1s").smooth(2, 1f);
		OOCSIFloat of12 = new OOCSIFloat(client1, "localVariableTestChannel21", "float2s").smooth(2, 2f);

		OOCSIClient client2 = new OOCSIClient();
		client2.connect("localhost", 4444);
		OOCSIFloat of21 = new OOCSIFloat(client2, "localVariableTestChannel21", "float1s").smooth(2, 1f);
		OOCSIFloat of22 = new OOCSIFloat(client2, "localVariableTestChannel21", "float2s").smooth(2, 2f);

		Thread.sleep(150);

		// initially ---

		assertEquals(0f, of21.get(), 0);
		assertEquals(0f, of22.get(), 0);

		// towards ---

		of11.set(1.f);
		Thread.sleep(150);
		of12.set(1.f);
		Thread.sleep(150);
		of11.set(2.f);
		Thread.sleep(150);
		of12.set(2.f);

		Thread.sleep(150);

		assertEquals(1.5f, of21.get(), 0);
		assertEquals(1.5f, of22.get(), 0);

		// move up a lot ---

		of21.set(10.2f);
		Thread.sleep(50);
		of22.set(10.2f);

		Thread.sleep(500);

		// both will move up a bit, but of11 less because of a lower sigma
		assertEquals(2f, of11.get(), 0.1);
		assertTrue(of11.get() < of12.get());

		// move up ---

		of21.set(10.2f);
		of21.set(10.2f);

		Thread.sleep(500);

		assertEquals(2.625f, of11.get(), 0.1);

		// back ---

		of21.set(-10.2f);
		of21.set(10.2f);

		Thread.sleep(500);

		assertEquals(2.53f, of11.get(), 0.1);

		client1.disconnect();
		client2.disconnect();
	}

	@Test
	public void testVariableTimeout() throws InterruptedException {

		OOCSIClient client1 = new OOCSIClient();
		client1.connect("localhost", 4444);
		OOCSIFloat of11 = new OOCSIFloat(client1, "localVariableTestChannel23", "float1").reference(4f).timeout(500);
		OOCSIFloat of12 = new OOCSIFloat(client1, "localVariableTestChannel23", "float2").reference(4f).timeout(1000);

		Thread.sleep(150);

		// initialization should be reference value
		assertEquals(4f, of11.get(), 0);
		assertEquals(4f, of12.get(), 0);

		// setting the variable starts timeout
		of11.set(0f);
		of12.set(0f);

		// new values are directly available
		assertEquals(0f, of11.get(), 0);
		assertEquals(0f, of12.get(), 0);

		// wait more than first variable's timeout
		Thread.sleep(600);

		// first variable has switched back
		assertEquals(4f, of11.get(), 0);
		assertEquals(0f, of12.get(), 0);

		// wait for more time
		Thread.sleep(500);

		// also second variable has switched back
		assertEquals(4f, of11.get(), 0);
		assertEquals(4f, of12.get(), 0);

		client1.disconnect();
	}

	@Test
	public void testVariableConnect() throws InterruptedException {

		OOCSIFloat of11 = new OOCSIFloat(0f, -1);
		OOCSIFloat of12 = new OOCSIFloat(0f, -1);
		OOCSIFloat of21 = new OOCSIFloat(0f, -1);

		of11.connect(of21);
		of12.connect(of21);

		// initialization should be reference value
		assertEquals(0f, of11.get(), 0);
		assertEquals(0f, of12.get(), 0);
		assertEquals(0f, of21.get(), 0);

		// set the variables
		of11.set(8f);
		of12.set(8f);

		// new values are directly available
		assertEquals(8f, of11.get(), 0);
		assertEquals(8f, of12.get(), 0);
		assertEquals(8f, of21.get(), 0);

		// activate smoothing
		of21.smooth(2);

		// send events
		of11.set(10f);
		of12.set(20f);

		// wait for more time
		Thread.sleep(100);

		// check end point
		assertEquals(15f, of21.get(), 0);

		// send events
		of11.set(10f);

		// wait for more time
		Thread.sleep(100);

		// check end point
		assertEquals(15f, of21.get(), 0);

		// send events
		of11.set(10f);

		// wait for more time
		Thread.sleep(100);

		// check end point
		assertEquals(10f, of21.get(), 0);
	}

	@Test
	public void testConnectReconnectVariableSubscriptions() throws InterruptedException {

		OOCSIClient client1 = new OOCSIClient();
		client1.setReconnect(true);
		client1.connect("localhost", 4444);
		assertTrue(client1.isConnected());

		OOCSIFloat of11 = new OOCSIFloat(client1, "localVariableTestChannel24", "float1");

		of11.set(10f);

		// connect second client

		OOCSIClient client2 = new OOCSIClient();
		client2.connect("localhost", 4444);
		assertTrue(client2.isConnected());

		assertEquals(10f, of11.get(), 0);

		OOCSIFloat of21 = new OOCSIFloat(client2, "localVariableTestChannel24", "float1");

		Thread.sleep(200);
		of11.set(11f);
		Thread.sleep(500);

		assertEquals(11f, of11.get(), 0);
		assertEquals(11f, of21.get(), 0);

		client1.reconnect();
		assertTrue(!client1.isConnected());

		of21.set(10.6f);
		Thread.sleep(500);

		assertEquals(10.6f, of21.get(), 0);
		assertEquals(11f, of11.get(), 0);

		assertTrue(client1.isConnected());

		of21.set(12.6f);
		Thread.sleep(500);

		assertEquals(12.6f, of11.get(), 0);

		client1.disconnect();
		client2.disconnect();
	}

}
