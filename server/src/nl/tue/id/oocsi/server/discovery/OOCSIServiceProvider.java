package nl.tue.id.oocsi.server.discovery;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

import nl.tue.id.oocsi.server.OOCSIServer;

/**
 * @author jhu
 * 
 */
public class OOCSIServiceProvider {

	private static ServiceAttendant attendant;
	private String instanceName = ServiceConstants.INSTANCE_NAME;
	private String serviceName = ServiceConstants.SERVICE_NAME;


	public ServerSocket getServerSocket(String name) throws IOException {
		this.serviceName = name;
		return this.getServerSocket(name, 0);
	}

	public ServerSocket getServerSocket(String name, int port)
			throws IOException {
		this.serviceName = name;

		ServerSocket serverSocket = null;

		try {
			serverSocket = new ServerSocket();

			serverSocket.setPerformancePreferences(
					ServiceConstants.SOCKET_PERFORMANCE_CONNECTIONTIME,
					ServiceConstants.SOCKET_PERFORMANCE_LATENCY,
					ServiceConstants.SOCKET_PERFORMANCE_BANDWIDTH);
			serverSocket.setReuseAddress(ServiceConstants.SOCKET_REUSEADDR);

			serverSocket.bind(new InetSocketAddress(InetAddress.getLocalHost(),
					port));
		} catch (IOException ioe) {
			OOCSIServer.log("Could not bind a server socket to a free port: "
					+ ioe);
			throw ioe;
		}


		String 	uniqueInstanceName = instanceName + "@"
						+ serverSocket.getInetAddress().getHostAddress() + ":"
						+ serverSocket.getLocalPort();

		/*
		 * Create a descriptor for the service you are providing.
		 */
		ServiceDescription descriptor = new ServiceDescription();
		descriptor.setAddress(serverSocket.getInetAddress());
		descriptor.setPort(serverSocket.getLocalPort());
		descriptor.setInstanceName(uniqueInstanceName);
		OOCSIServer.log("Service details: " + descriptor.toString());

		/*
		 * Set up an attendant and give it the descriptor to publish. Also to
		 * attempt graceful handling of ctrl-C, add a shutdown hook which tries
		 * to alert the network that this service is no longer provided. Finally
		 * start the attendant that works in its own thread.
		 */
		attendant = new ServiceAttendant(serviceName);
		attendant.setDescriptor(descriptor);
		attendant.addShutdownHandler();
		attendant.startAttendant();

		return serverSocket;

	}

}
