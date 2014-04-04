import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import nl.tue.id.oocsi.client.OOCSIClient;
import nl.tue.id.oocsi.server.socket.SocketServer;

import org.junit.Test;

public class ClientSignoffTest {

	CountDownLatch cdl = new CountDownLatch(100);
	SocketServer server;

	@Test
	public void testMultiClientSignOff() throws InterruptedException {

		// start server
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					server = new SocketServer(4444, 102);
					server.init();
				} catch (IOException e) {
					// e.printStackTrace();
				} finally {
					// done
				}
			}
		}).start();

		// start clients that simply sign on and then off after some time
		for (int i = 0; i < 100; i++) {
			new Thread(new Runnable() {

				@Override
				public void run() {
					String clientName = "clienttest__" + "__" + System.currentTimeMillis();
					OOCSIClient client = new OOCSIClient(clientName);
					if (client.connect("localhost", 4444)) {
						try {
							Thread.sleep(2000);
						} catch (InterruptedException e) {
						}

						client.kill();
					}
					cdl.countDown();
				}
			}).start();
		}

		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
		}

		cdl.await();
		assertEquals(server.getClients(), "");
	}

}
