import static org.junit.Assert.assertEquals;
import nl.tue.id.oocsi.server.OOCSIServer;

import org.junit.Test;

public class ServerCommandLineParsing {

	@Test
	public void testCommandLineParsing() {

		OOCSIServer.parseCommandlineArgs("-port 4445".split(" "));
		assertEquals(OOCSIServer.port, 4445);

		OOCSIServer.parseCommandlineArgs("-clients 24".split(" "));
		assertEquals(OOCSIServer.maxClients, 24);

		assertEquals(OOCSIServer.isLogging, false);
		OOCSIServer.parseCommandlineArgs("-logging".split(" "));
		assertEquals(OOCSIServer.isLogging, true);

		OOCSIServer.parseCommandlineArgs("-port 4446 -clients 26".split(" "));
		assertEquals(OOCSIServer.port, 4446);
		assertEquals(OOCSIServer.maxClients, 26);

		OOCSIServer.parseCommandlineArgs("-clients 27 -port 4447".split(" "));
		assertEquals(OOCSIServer.port, 4447);
		assertEquals(OOCSIServer.maxClients, 27);

		OOCSIServer.isLogging = false;
		OOCSIServer.parseCommandlineArgs("-port 4446 -logging -clients 26"
				.split(" "));
		assertEquals(OOCSIServer.port, 4446);
		assertEquals(OOCSIServer.maxClients, 26);
		assertEquals(OOCSIServer.isLogging, true);

		OOCSIServer.isLogging = false;
		OOCSIServer.parseCommandlineArgs("-clients 27 -port 4447 -logging"
				.split(" "));
		assertEquals(OOCSIServer.port, 4447);
		assertEquals(OOCSIServer.maxClients, 27);
		assertEquals(OOCSIServer.isLogging, true);
	}

}
