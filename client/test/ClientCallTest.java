import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import nl.tue.id.oocsi.OOCSIEvent;
import nl.tue.id.oocsi.client.OOCSIClient;
import nl.tue.id.oocsi.client.services.OOCSICall;
import nl.tue.id.oocsi.client.services.Responder;
import nl.tue.id.oocsi.client.services.Service;
import nl.tue.id.oocsi.client.services.Service.ServiceField;
import nl.tue.id.oocsi.client.services.Service.ServiceMethod;

import org.junit.Test;

/**
 * call / responder test cases
 *
 * @author matsfunk
 */
public class ClientCallTest {

	@Test
	public void testServiceInstantiation() {

		Service s = new Service();
		s.name = "addition";
		s.category = "Math";
		s.uuid = "as8ca08sc98asc98asc8asc";

		ServiceMethod serviceMethod = s.newServiceMethod();
		serviceMethod.handle = "addnineteen2";
		serviceMethod.name = "addnineteen2";
		serviceMethod.input.add(new ServiceField<Integer>("value"));
		serviceMethod.output.add(new ServiceField<Integer>("additionOutput"));
		s.methods.add(serviceMethod);

		// register all methods of a service
		// for (ServiceMethod method : s.methods) {
		// method.responder();
		// }

		// hand made responder
		OOCSIClient o1 = new OOCSIClient("pingS");
		o1.connect("localhost", 4444);
		assertTrue(o1.isConnected());
		final ServiceMethod serviceMethod2 = s.methods.get(0);
		serviceMethod2.registerResponder(o1, new Responder() {
			public void respond(OOCSIEvent event, Map<String, Object> response) {
				int value = event.getInt(serviceMethod2.input.get(0).name, 0);
				value += 29;
				response.put(serviceMethod2.output.get(0).name, value);
			}
		});

		OOCSIClient o2 = new OOCSIClient("pongS");
		o2.connect("localhost", 4444);
		assertTrue(o2.isConnected());
		{
			OOCSICall call = serviceMethod2.buildCall(o2, 100, 1);
			assertTrue(!call.canSend());
		}
		{
			OOCSICall call = serviceMethod2.buildCall(o2, 1000, 1).data("value", 1);
			assertTrue(call.canSend());
			call.send();
			assertTrue(call.hasResponse());
			if (call.hasResponse()) {
				int responseValue = call.getResponse().getInt(serviceMethod2.output.get(0).name, 0/*
																								 * (Integer)
																								 * serviceMethod2.output
																								 * .get(0).defaultValue
																								 */);
				assertEquals(30, responseValue);
			}
		}
	}

	@Test
	public void testResponse() throws InterruptedException {
		OOCSIClient o1 = new OOCSIClient("ping");
		o1.connect("localhost", 4444);
		assertTrue(o1.isConnected());

		OOCSIClient o2 = new OOCSIClient("pong");
		o2.connect("localhost", 4444);
		assertTrue(o2.isConnected());
		o2.register("addnineteen", new Responder(o2) {

			@Override
			public void respond(OOCSIEvent event, Map<String, Object> response) {
				int pp = event.getInt("addnineteen", -1);
				pp += 19;
				response.put("addedthat", pp);
			}
		});

		{
			OOCSICall call = new OOCSICall(o1, "pong", "addnineteen", 500, 1).data("addnineteen", 1);
			call.send();

			assertTrue(call.hasResponse());
			OOCSIEvent response = call.getResponse();
			assertEquals(20, response.getInt("addedthat", -1));
		}
		{
			OOCSICall call = new OOCSICall(o1, "pong", "addnineteen", 500, 1).data("addnineteen", 100);
			call.send();
			assertTrue(call.hasResponse());
			OOCSIEvent response = call.getResponse();
			assertEquals(119, response.getInt("addedthat", -1));
		}
	}

	@Test
	public void testResponderOverlap() throws InterruptedException {
		OOCSIClient o1 = new OOCSIClient("pingR");
		o1.connect("localhost", 4444);
		assertTrue(o1.isConnected());

		OOCSIClient o2 = new OOCSIClient("pongR");
		o2.connect("localhost", 4444);
		assertTrue(o2.isConnected());
		o2.register("addnineteen", new Responder(o2) {

			@Override
			public void respond(OOCSIEvent event, Map<String, Object> response) {
				int pp = event.getInt("addnineteen", -1);
				pp += 19;
				response.put("addedthat", pp);
				System.out.println("response from pongR");
			}
		});
		o2.register("addnine", new Responder(o2) {

			@Override
			public void respond(OOCSIEvent event, Map<String, Object> response) {
				int pp = event.getInt("addnineteen", -1);
				pp += 9;
				response.put("addedthat", pp);
			}
		});

		{
			OOCSICall call = new OOCSICall(o1, "addnineteen", "addnineteen", 500, 1).data("addnineteen", 1);
			call.send();

			assertTrue(call.hasResponse());
			OOCSIEvent response = call.getResponse();
			assertEquals(20, response.getInt("addedthat", -1));
		}
		{
			OOCSICall call = new OOCSICall(o1, "addnineteen", "addnineteen", 500, 1).data("addnineteen", 100);
			call.send();

			assertTrue(call.hasResponse());
			OOCSIEvent response = call.getResponse();
			assertEquals(119, response.getInt("addedthat", -1));
		}
	}

	@Test
	public void testResponderOverlap2() throws InterruptedException {
		OOCSIClient o1 = new OOCSIClient("pingR1");
		o1.connect("localhost", 4444);
		assertTrue(o1.isConnected());

		{
			OOCSIClient o2 = new OOCSIClient("pongR1");
			o2.connect("localhost", 4444);
			assertTrue(o2.isConnected());
			o2.register("addnineteen2", new Responder(o2) {

				@Override
				public void respond(OOCSIEvent event, Map<String, Object> response) {
					int pp = event.getInt("addnineteen", -1);
					pp += 19;
					response.put("addedthat", pp);
					System.out.println("response from pongR1");
				}
			});
		}
		{
			OOCSIClient o2 = new OOCSIClient("pongR2");
			o2.connect("localhost", 4444);
			assertTrue(o2.isConnected());
			o2.register("addnineteen2", new Responder(o2) {

				@Override
				public void respond(OOCSIEvent event, Map<String, Object> response) {
					int pp = event.getInt("addnineteen", -1);
					pp += 19;
					response.put("addedthat", pp);
					System.out.println("response from pongR2");
				}
			});
		}

		// test normal call/response handlers
		{
			OOCSICall call = new OOCSICall(o1, "addnineteen2", "addnineteen2", 500, 1).data("addnineteen", 1);
			call.send();

			assertTrue(call.hasResponse());
			OOCSIEvent response = call.getResponse();
			assertEquals(20, response.getInt("addedthat", -1));
		}

		// test direct response trigger 1
		{
			OOCSICall call = new OOCSICall(o1, "pongR1", "addnineteen2", 500, 1).data("addnineteen", 100);
			call.send();
			assertTrue(call.hasResponse());
			OOCSIEvent response = call.getResponse();
			assertEquals(119, response.getInt("addedthat", -1));
		}

		// test direct response trigger 2
		{
			OOCSICall call = new OOCSICall(o1, "pongR2", "addnineteen2", 500, 1).data("addnineteen", 100);
			call.send();
			assertTrue(call.hasResponse());
			OOCSIEvent response = call.getResponse();
			assertEquals(119, response.getInt("addedthat", -1));
		}
	}

	@Test
	public void testResponderFail() throws InterruptedException {

		OOCSIClient o1 = new OOCSIClient("pingFail");
		o1.connect("localhost", 4444);
		assertTrue(o1.isConnected());

		OOCSIClient o2 = new OOCSIClient("pongFail");
		o2.connect("localhost", 4444);
		assertTrue(o2.isConnected());

		{
			OOCSICall call = new OOCSICall(o1, "pongFail", "addnineteen", 500, 1).data("addnineteen", 1);
			call.send();
			assertTrue(!call.hasResponse());
		}
		{
			OOCSICall call = new OOCSICall(o1, "pongFail", "addnineteen", 500, 1).data("addnineteen", 100);
			call.send();
			assertTrue(!call.hasResponse());
		}
	}

	@Test
	public void testResponderTimeout() throws InterruptedException {
		OOCSIClient o1 = new OOCSIClient("pingTO");
		o1.connect("localhost", 4444);
		assertTrue(o1.isConnected());

		OOCSIClient o2 = new OOCSIClient("pongTO");
		o2.connect("localhost", 4444);
		assertTrue(o2.isConnected());
		o2.register("addnineteen1", new Responder(o2) {

			@Override
			public void respond(OOCSIEvent event, Map<String, Object> response) {
				try {
					System.out.println("sleeping now");
					Thread.sleep(600);
				} catch (InterruptedException e) {
				}
			}
		});

		{
			OOCSICall call = new OOCSICall(o1, "pongTO", "addnineteen1", 500, 1).data("addnineteen", 1);
			call.send();
			assertTrue(!call.hasResponse());
		}
		{
			OOCSICall call = new OOCSICall(o1, "pongTO", "addnineteen1", 500, 1).data("addnineteen", 100);
			call.send();
			assertTrue(!call.hasResponse());
		}
	}

}
