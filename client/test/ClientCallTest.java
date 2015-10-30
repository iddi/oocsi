import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.util.Map;

import nl.tue.id.oocsi.OOCSIEvent;
import nl.tue.id.oocsi.client.OOCSIClient;
import nl.tue.id.oocsi.client.protocol.OOCSICall;
import nl.tue.id.oocsi.client.protocol.Responder;

import org.junit.Test;

/**
 * call / responder test case
 *
 * @author matsfunk
 */
public class ClientCallTest {

	@Test
	public void testReconnection() throws InterruptedException {
		OOCSIClient o1 = new OOCSIClient("ping");
		o1.connect("localhost", 4444);
		assertTrue(o1.isConnected());

		OOCSIClient o2 = new OOCSIClient("pong");
		o2.connect("localhost", 4444);
		assertTrue(o2.isConnected());
		o2.subscribe(new Responder(o2) {

			@Override
			public void respond(OOCSIEvent event, Map<String, Object> response) {
				int pp = event.getInt("addnineteen", -1);
				pp += 19;
				response.put("addedthat", pp);
			}
		});

		{
			OOCSICall call = new OOCSICall(o1, "pong", 500, 1).data("addnineteen", 1);
			call.send();
			assertTrue(call.hasResponse());
			OOCSIEvent response = call.getResponse();
			assertEquals(20, response.getInt("addedthat", -1));
		}
		{
			OOCSICall call = new OOCSICall(o1, "pong", 500, 1).data("addnineteen", 100);
			call.send();
			assertTrue(call.hasResponse());
			OOCSIEvent response = call.getResponse();
			assertEquals(119, response.getInt("addedthat", -1));
		}
	}

	@Test
	public void testReconnectionFail() throws InterruptedException {
		OOCSIClient o1 = new OOCSIClient("pingFail");
		o1.connect("localhost", 4444);
		assertTrue(o1.isConnected());

		OOCSIClient o2 = new OOCSIClient("pongFail");
		o2.connect("localhost", 4444);
		assertTrue(o2.isConnected());

		{
			OOCSICall call = new OOCSICall(o1, "pongFail", 500, 1).data("addnineteen", 1);
			call.send();
			assertTrue(!call.hasResponse());
		}
		{
			OOCSICall call = new OOCSICall(o1, "pongFail", 500, 1).data("addnineteen", 100);
			call.send();
			assertTrue(!call.hasResponse());
		}
	}

	@Test
	public void testReconnectionTimeout() throws InterruptedException {
		OOCSIClient o1 = new OOCSIClient("pingTO");
		o1.connect("localhost", 4444);
		assertTrue(o1.isConnected());

		OOCSIClient o2 = new OOCSIClient("pongTO");
		o2.connect("localhost", 4444);
		assertTrue(o2.isConnected());
		o2.subscribe(new Responder(o2) {

			@Override
			public void respond(OOCSIEvent event, Map<String, Object> response) {
				try {
					Thread.sleep(600);
				} catch (InterruptedException e) {
				}
			}
		});

		{
			OOCSICall call = new OOCSICall(o1, "pongTO", 500, 1).data("addnineteen", 1);
			call.send();
			assertTrue(!call.hasResponse());
		}
		{
			OOCSICall call = new OOCSICall(o1, "pongTO", 500, 1).data("addnineteen", 100);
			call.send();
			assertTrue(!call.hasResponse());
		}
	}

}
