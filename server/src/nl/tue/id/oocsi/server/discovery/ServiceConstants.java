package nl.tue.id.oocsi.server.discovery;

/**
 * Constants used by the discovery process, shared by both the server and
 * clients.
 * 
 * 
 * @author jhu
 * 
 */
public class ServiceConstants {
	// the server and client must match up on the following values...

	/**
	 * A multicast datagram packet instead goes to all machines on the local
	 * network that are listening on the same channel address. The "body" of
	 * that query packet must include that it (1) a request for (2) a named
	 * service .A "service name" needs to be introduced.
	 */
	public static final String SERVICE_NAME = "oocsiService";

	/**
	 * The multicase datagram may also include a human user friendly label for
	 * the service instance itself.
	 */
	public static final String INSTANCE_NAME = "oocsiServer";

	/**
	 * "A multicast group is specified by a class D IP address and by a standard
	 * UDP port number. Class D IP addresses are in the range 224.0.0.0 to
	 * 239.255.255.255, inclusive. The address 224.0.0.0 is reserved and should
	 * not be used. See also http://en.wikipedia.org/wiki/Multicast_address.
	 */
	public static final String MULTICAST_ADDRESS_GROUP = "230.0.0.1";

	/**
	 * Multicast port number. This is a rather arbitrary choice, and can easily
	 * be changed as needed.
	 */
	public static final int MULTICAST_PORT = 4443;

	/**
	 * The length given to the DatagramPacket constructor is the length of the
	 * data in the buffer to send. All data in the buffer after that amount of
	 * data is ignored. Usually the buffer would have the same size.
	 * 
	 */
	public static final int DATAGRAM_LENGTH = 1024;
	

	/**
	 * Enable/disable socket timeout with, in milliseconds. With this option set
	 * to a non-zero timeout, a call to receive() for the MulticastSocket will
	 * block for only this amount of time. If the timeout expires, a
	 * java.net.SocketTimeoutException is raised, though the MulticastSocket is
	 * still valid. The timeout must be > 0. A timeout of zero is interpreted as
	 * an infinite timeout.
	 * 
	 * Socket timeout for ServiceAttendant
	 */
	public static final int ATTENDANT_SOCKET_TIMEOUT = 250;

	/**
	 * Socket timeout for ServiceExplorer
	 */
	public static final int EXPLORER_SOCKET_TIMEOUT = 250;

	/**
	 * schedule the ServiceExplorer lookup tasks for repeated fixed-rate
	 * execution
	 */
	public static final int EXPLORER_QUERY_INTERVAL = 500;

	/**
	 * 
	 * connectionTime - An int expressing the relative importance of a short
	 * connection time latency - An int expressing the relative importance of
	 * low latency bandwidth - An int expressing the relative importance of high
	 * bandwidth
	 */
	public static final int SOCKET_PERFORMANCE_CONNECTIONTIME = 0;
	public static final int SOCKET_PERFORMANCE_LATENCY = 1;
	public static final int SOCKET_PERFORMANCE_BANDWIDTH = 0;

	/**
	 * Enable/disable the SO_REUSEADDR socket option. When a TCP connection is
	 * closed the connection may remain in a timeout state for a period of time
	 * after the connection is closed (typically known as the TIME_WAIT state or
	 * 2MSL wait state). For applications using a well known socket address or
	 * port it may not be possible to bind a socket to the required
	 * SocketAddress if there is a connection in the timeout state involving the
	 * socket address or port.
	 */
	public static final boolean SOCKET_REUSEADDR = true;

}
