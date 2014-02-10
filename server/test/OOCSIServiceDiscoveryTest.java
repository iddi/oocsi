import static org.junit.Assert.assertEquals;

import java.net.ServerSocket;
import java.util.Vector;

import nl.tue.id.oocsi.server.discovery.OOCSIServiceExplorer;
import nl.tue.id.oocsi.server.discovery.OOCSIServiceProvider;
import nl.tue.id.oocsi.server.discovery.ServiceDescription;
import nl.tue.id.oocsi.server.discovery.ServiceExplorer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class OOCSIServiceDiscoveryTest {

	ServerSocket serverSocket1 = null;
	ServerSocket serverSocket2 = null;
	ServerSocket serverSocket3 = null;
	OOCSIServiceProvider provider = new OOCSIServiceProvider();

	ServiceExplorer explorer;
	Vector<ServiceDescription> descriptors;

	@Before
	public void setUp() throws Exception {

		serverSocket1 = provider.getServerSocket("oocsiService", 4444);
		serverSocket2 = provider.getServerSocket("anyService", 1234);
		serverSocket3 = provider.getServerSocket("anyService", 1235);

	}

	@After
	public void tearDown() throws Exception {
		serverSocket1.close();
		serverSocket2.close();
		serverSocket3.close();
	}

	@Test
	public void test() {

		OOCSIServiceExplorer explorer = new OOCSIServiceExplorer();

		String[] s = explorer.list();
		assertEquals(s.length, 1);
		System.out.println(s[0]);

		s = explorer.list("anyService");
		assertEquals(s.length, 2);
		System.out.println(s[0]);
		System.out.println(s[1]);

		descriptors = explorer.lookupServiceDescriptors();
		assertEquals(descriptors.size(), 2);
		System.out.println(descriptors.get(0).getInstanceName());
		System.out.println(descriptors.get(1).getInstanceName());

		// fail("Not yet implemented");
	}

}
