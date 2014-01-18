package nl.tue.id.oocsi.server.discovery;

/**
 * @author jhu
 *
 */
public class ServiceConstants { 
	// the server and client must match up on the following values...
	public static final String SERVICE_NAME = "oocsiService";
	public static final String INSTANCE_NAME = "oocsiServer";
	public static final String MULTICAST_ADDRESS_GROUP = "230.0.0.1";
	public static final int MULTICAST_PORT = 4443;
	public static final int DATAGRAM_LENGTH = 1024;

	// the rest of these values can be changed/tuned as needed...
	public static final int RESPONDER_SOCKET_TIMEOUT = 250;
	public static final int EXPLORER_SOCKET_TIMEOUT = 250;
	public static final int EXPLORER_QUERY_INTERVAL = 500;

}
