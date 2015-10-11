import static org.junit.Assert.assertTrue;

import java.util.Map;

import nl.tue.id.oocsi.OOCSIEvent;
import nl.tue.id.oocsi.client.OOCSIClient;
import nl.tue.id.oocsi.client.protocol.OOCSICall;
import nl.tue.id.oocsi.client.protocol.Responder;

import org.junit.Test;

public class ClientCallTest {

	@Test
	public void testReconnection() throws InterruptedException {
		// final List<String> list2 = new ArrayList<String>();
		int counter = 0;

		OOCSIClient o1 = new OOCSIClient("ping");
		o1.connect("localhost", 4444);
		assertTrue(o1.isConnected());

		OOCSIClient o2 = new OOCSIClient("pong");
		o2.connect("localhost", 4444);
		assertTrue(o2.isConnected());
		o2.subscribe(new Responder(o2) {

			@Override
			public void respond(OOCSIEvent event, Map<String, Object> response) {
				int pp = event.getInt("pingipung", -1);
				pp += 19;
				response.put("pingipung", pp);
			}
		});

		{
			OOCSICall call = new OOCSICall(o1, "pong", 500, 1).data("pingipung", 1);
			call.send();
			if (call.hasResponse()) {
				System.out.println("wow! pingipung: " + call.getResponse().get("pingipung"));
			} else {
				System.out.println("no response yet");
			}
		}
		{
			OOCSICall call = new OOCSICall(o1, "pong", 500, 1).data("pingipung", 100);
			call.send();
			if (call.hasResponse()) {
				System.out.println("wow! pingipung: " + call.getResponse().get("pingipung"));
			} else {
				System.out.println("no response yet");
			}
		}

		// while (o1.isConnected() && counter < 10000) {
		// Thread.sleep(100);
		// }
	}

}
