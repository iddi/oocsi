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

	private static ServiceResponder responder;
	private String instanceName = ServiceConstants.INSTANCE_NAME;
	private String serviceName = ServiceConstants.SERVICE_NAME;
	
	public ServerSocket getServerSocket(String name) throws IOException {
		this.serviceName = name;
		return this.getServerSocket(name, 0);
	}

	
	public ServerSocket getServerSocket(String name, int port) throws IOException {
		this.serviceName = name;

		ServerSocket serverSocket = null;
		
		try {
			serverSocket = new ServerSocket();
			serverSocket.bind(new InetSocketAddress(InetAddress.getLocalHost(),port));
		}
		catch (IOException ioe) {
			OOCSIServer.log("Could not bind a server socket to a free port: "+ioe);
			throw ioe;
		}

		
		/*
		 * Create a descriptor for the service you are providing.
		 */
		ServiceDescription descriptor = new ServiceDescription();
		descriptor.setAddress(serverSocket.getInetAddress());
		descriptor.setPort(serverSocket.getLocalPort());
		descriptor.setInstanceName(instanceName);
		OOCSIServer.log("Service details: "+descriptor.toString());

		/*
		 * To improve: 'query' at this point to see if the service instance name already 
		 * exists on the local network. If it does,  or retry with a modified service instance
		 * name, possibly by using a suffix such as "(2)" on the end. 
		 */
		
		
		/*
		 * Set up a responder and give it the descriptor to publish. Also to attempt graceful 
		 * handling of ctrl-C, add a shutdown hook which tries to alert the network that 
		 * this service is no longer provided.
		 * Finally start the responder (which works in its own thread).
		 */
		responder = new ServiceResponder(serviceName);
		responder.setDescriptor(descriptor);
		responder.addShutdownHandler();
		responder.startResponder();

        return serverSocket;

	}
}
