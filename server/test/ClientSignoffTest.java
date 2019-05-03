import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;

import nl.tue.id.oocsi.client.OOCSIClient;
import nl.tue.id.oocsi.server.OOCSIServer;

public class ClientSignoffTest {

	CountDownLatch cdl;
	OOCSIServer server;

	@Test
	public void testMultiClientSignOff() throws InterruptedException, IOException {

		cdl = new CountDownLatch(100);

		server = new OOCSIServer(4444, 102, false);

		assertNotEquals(null, server);

		// start clients that simply sign on and then off after some time
		for (int i = 0; i < 100; i++) {
			new Thread(new Runnable() {

				@Override
				public void run() {
					try {
						String clientName = "clienttest__" + "__" + System.currentTimeMillis();
						OOCSIClient client = new OOCSIClient(clientName);
						if (client.connect("localhost", 4444)) {
							try {
								Thread.sleep(2000);
							} catch (InterruptedException e) {
							}

							client.kill();
						}
					} catch (Exception e) {
					}
					cdl.countDown();
				}
			}).start();

			Thread.sleep(20);
		}

		cdl.await();
		assertNotEquals(null, server);
		assertEquals(server.getClientList(), "");
	}

}
