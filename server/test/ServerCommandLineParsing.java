import static org.junit.Assert.assertEquals;
import nl.tue.id.oocsi.server.OOCSIServer;

import org.junit.Test;

public class ServerCommandLineParsing {

	@Test
	public void testCommandLineParsing() {

		OOCSIServer os = OOCSIServer.getInstance();

		os.port = 0;
		os.parseCommandlineArgs("-port 4445".split(" "));
		assertEquals(os.port, 4445);

		os.setMaxClients(0);
		os.parseCommandlineArgs("-clients 24".split(" "));
		assertEquals(os.getMaxClients(), 24);

		os.isLogging = false;
		assertEquals(os.isLogging, false);
		os.parseCommandlineArgs("-logging".split(" "));
		assertEquals(os.isLogging, true);

		os.port = 0;
		os.setMaxClients(0);
		os.parseCommandlineArgs("-port 4446 -clients 26".split(" "));
		assertEquals(os.port, 4446);
		assertEquals(os.getMaxClients(), 26);

		os.port = 0;
		os.setMaxClients(0);
		os.parseCommandlineArgs("-clients 27 -port 4447".split(" "));
		assertEquals(os.port, 4447);
		assertEquals(os.getMaxClients(), 27);

		os.port = 0;
		os.setMaxClients(0);
		os.isLogging = false;
		os.parseCommandlineArgs("-port 4446 -logging -clients 26".split(" "));
		assertEquals(os.port, 4446);
		assertEquals(os.getMaxClients(), 26);
		assertEquals(os.isLogging, true);

		os.port = 0;
		os.setMaxClients(0);
		os.isLogging = false;
		os.parseCommandlineArgs("-clients 27 -port 4447 -logging".split(" "));
		assertEquals(os.port, 4447);
		assertEquals(os.getMaxClients(), 27);
		assertEquals(os.isLogging, true);
	}

}
