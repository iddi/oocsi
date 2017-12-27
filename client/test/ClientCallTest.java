import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import nl.tue.id.oocsi.OOCSIData;
import nl.tue.id.oocsi.OOCSIEvent;
import nl.tue.id.oocsi.client.OOCSIClient;
import nl.tue.id.oocsi.client.services.OOCSICall;
import nl.tue.id.oocsi.client.services.Responder;
import nl.tue.id.oocsi.client.services.Service;
import nl.tue.id.oocsi.client.services.Service.ServiceField;
import nl.tue.id.oocsi.client.services.Service.ServiceMethod;

/**
 * call / responder test cases
 *
 * @author matsfunk
 */
public class ClientCallTest {

	@Test
	public void testServiceInstantiation() throws InterruptedException {

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
			public void respond(OOCSIEvent event, OOCSIData response) {
				int value = event.getInt(serviceMethod2.input.get(0).name, 0);
				value += 29;
				response.data(serviceMethod2.output.get(0).name, value);
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
			call.sendAndWait();
			assertTrue(call.hasResponse());
			if (call.hasResponse()) {
				int responseValue = call.getFirstResponse().getInt(serviceMethod2.output.get(0).name,
						0/*
							 * (Integer) serviceMethod2 .output .get(0 ).defaultValue
							 */);
				assertEquals(30, responseValue);
			}
		}

		o1.disconnect();
		o2.disconnect();
	}

	@Test
	public void testResponse() throws InterruptedException {
		OOCSIClient o1 = new OOCSIClient("pingResponse");
		o1.connect("localhost", 4444);
		assertTrue(o1.isConnected());

		OOCSIClient o2 = new OOCSIClient("pongResponse");
		o2.connect("localhost", 4444);
		assertTrue(o2.isConnected());
		o2.register("addnineteen", new Responder(o2) {

			@Override
			public void respond(OOCSIEvent event, OOCSIData response) {
				int pp = event.getInt("addnineteen", -1);
				response.data("addedthat", pp + 19);
			}
		});

		{
			OOCSICall call = new OOCSICall(o1, "pongResponse", "addnineteen", 500, 1).data("addnineteen", 1);
			call.sendAndWait();
			assertTrue(call.hasResponse());
			OOCSIEvent response = call.getFirstResponse();
			assertEquals(20, response.getInt("addedthat", -1));
		}
		{
			OOCSICall call = new OOCSICall(o1, "pongResponse", "addnineteen", 500, 1).data("addnineteen", 100);
			call.sendAndWait();
			assertTrue(call.hasResponse());
			OOCSIEvent response = call.getFirstResponse();
			assertEquals(119, response.getInt("addedthat", -1));
		}

		o1.disconnect();
		o2.disconnect();
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
			public void respond(OOCSIEvent event, OOCSIData response) {
				int pp = event.getInt("addnineteen", -1);
				response.data("addedthat", pp + 19);
				System.out.println("response from pongR");
			}
		});
		o2.register("addnine", new Responder(o2) {

			@Override
			public void respond(OOCSIEvent event, OOCSIData response) {
				int pp = event.getInt("addnineteen", -1);
				response.data("addedthat", pp + 9);
			}
		});

		{
			OOCSICall call = new OOCSICall(o1, "addnineteen", "addnineteen", 500, 1).data("addnineteen", 1);
			call.sendAndWait();

			assertTrue(call.hasResponse());
			OOCSIEvent response = call.getFirstResponse();
			assertEquals(20, response.getInt("addedthat", -1));
		}
		{
			OOCSICall call = new OOCSICall(o1, "addnineteen", "addnineteen", 500, 1).data("addnineteen", 100);
			call.sendAndWait();

			assertTrue(call.hasResponse());
			OOCSIEvent response = call.getFirstResponse();
			assertEquals(119, response.getInt("addedthat", -1));
		}

		o1.disconnect();
		o2.disconnect();
	}

	@Test
	public void testResponderOverlap2() throws InterruptedException {
		OOCSIClient o1 = new OOCSIClient("pingR1");
		o1.connect("localhost", 4444);
		assertTrue(o1.isConnected());

		OOCSIClient o2 = new OOCSIClient("pongR1");
		o2.connect("localhost", 4444);
		assertTrue(o2.isConnected());
		o2.register("addnineteen2", new Responder(o2) {

			@Override
			public void respond(OOCSIEvent event, OOCSIData response) {
				int pp = event.getInt("addnineteen", -1);
				pp += 19;
				response.data("addedthat", pp);
				System.out.println("response from pongR1");
			}
		});

		OOCSIClient o3 = new OOCSIClient("pongR2");
		o3.connect("localhost", 4444);
		assertTrue(o3.isConnected());
		o3.register("addnineteen2", new Responder(o3) {

			@Override
			public void respond(OOCSIEvent event, OOCSIData response) {
				int pp = event.getInt("addnineteen", -1);
				pp += 19;
				response.data("addedthat", pp);
				System.out.println("response from pongR2");
			}
		});

		// test normal call/response handlers
		{
			OOCSICall call = new OOCSICall(o1, "addnineteen2", "addnineteen2", 500, 1).data("addnineteen", 1);
			call.sendAndWait();

			assertTrue(call.hasResponse());
			OOCSIEvent response = call.getFirstResponse();
			assertEquals(20, response.getInt("addedthat", -1));
		}

		// test direct response trigger 1
		{
			OOCSICall call = new OOCSICall(o1, "pongR1", "addnineteen2", 500, 1).data("addnineteen", 100);
			call.sendAndWait();
			assertTrue(call.hasResponse());
			OOCSIEvent response = call.getFirstResponse();
			assertEquals(119, response.getInt("addedthat", -1));
		}

		// test direct response trigger 2
		{
			OOCSICall call = new OOCSICall(o1, "pongR2", "addnineteen2", 500, 1).data("addnineteen", 100);
			call.sendAndWait();
			assertTrue(call.hasResponse());
			OOCSIEvent response = call.getFirstResponse();
			assertEquals(119, response.getInt("addedthat", -1));
		}

		o1.disconnect();
		o2.disconnect();
		o3.disconnect();
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

		o1.disconnect();
		o2.disconnect();
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
			public void respond(OOCSIEvent event, OOCSIData response) {
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

		o1.disconnect();
		o2.disconnect();
	}

}
