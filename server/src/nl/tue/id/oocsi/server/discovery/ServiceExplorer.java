package nl.tue.id.oocsi.server.discovery;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import nl.tue.id.oocsi.server.OOCSIServer;

/**
 * @author jhu
 *
 */
public final class ServiceExplorer implements Runnable {

	private class QueryTimerTask extends TimerTask {
		public void run() {
			DatagramPacket packet = getQueryPacket();
			if (packet != null) {
				queuedPacket = packet;
			}
		}
	}
	protected static InetAddress multicastAddressGroup;

	protected static int multicastPort;

	static {
		try {
			multicastAddressGroup = InetAddress.getByName(ServiceConstants.MULTICAST_ADDRESS_GROUP);
			multicastPort = ServiceConstants.MULTICAST_PORT;
		}
		catch (UnknownHostException uhe) {
			System.err.println("Unexpected exception: "+uhe);
			uhe.printStackTrace();
			System.exit(1);
		}
	}
	protected String serviceName;
	protected boolean shouldRun = true;
	protected MulticastSocket socket;
	protected DatagramPacket queuedPacket;
	protected DatagramPacket receivedPacket;
	protected Vector<ServiceExplorerListener> listeners;
	protected Thread myThread;
	
	
	protected Timer myTimer;
	
	public ServiceExplorer() {
		
		try {
			socket = new MulticastSocket(multicastPort);
			socket.joinGroup(multicastAddressGroup);
			socket.setSoTimeout(ServiceConstants.EXPLORER_SOCKET_TIMEOUT);
			
		}
		catch (IOException ioe) {
			OOCSIServer.log("Unexpected exception: "+ioe);
			//ioe.printStackTrace();
			System.exit(1);
		}

		
		listeners = new Vector<ServiceExplorerListener>();
	}
	
	public void addServiceExplorerListener(ServiceExplorerListener l) {
		if (! listeners.contains(l)) {
			listeners.add(l);
		}
	}

	protected String getEncodedServiceName() {
		try {
			return URLEncoder.encode(getServiceName(),"UTF-8");
		}
		catch (UnsupportedEncodingException uee) {
			return null;
		}
	}

	protected DatagramPacket getQueryPacket() {
		StringBuffer buf = new StringBuffer();
		buf.append("SERVICE QUERY "+getEncodedServiceName());
		
		byte[] bytes = buf.toString().getBytes();
		DatagramPacket packet = new DatagramPacket(bytes,bytes.length);
		packet.setAddress(multicastAddressGroup);
		packet.setPort(multicastPort);
		
		return packet;
	}
	
	protected ServiceDescription getReplyDescriptor() {
		String dataStr = new String(receivedPacket.getData());
		int pos = dataStr.indexOf((char)0);
		if (pos>-1) {
			dataStr = dataStr.substring(0,pos);
		}
		
		StringTokenizer tokens = new StringTokenizer(dataStr.substring(15+getEncodedServiceName().length()));
		if (tokens.countTokens()==3) {
			return ServiceDescription.parse(tokens.nextToken(),
					tokens.nextToken(), tokens.nextToken());
		}
		else {
			return null;
		}
	}

	public String getServiceName() {
		return serviceName;
	}

	protected boolean isReplyPacket(){ 
		if (receivedPacket==null) {
			return false;
		}
		
		String dataStr = new String(receivedPacket.getData());
		int pos = dataStr.indexOf((char)0);
		if (pos>-1) {
			dataStr = dataStr.substring(0,pos);
		}
		
		/* REQUIRED TOKEN TO START */
		if (dataStr.startsWith("SERVICE REPLY "+getEncodedServiceName())) {
			return true;
		}

		return false;
	}
	
	protected void notifyReply(ServiceDescription descriptor) {
		for (ServiceExplorerListener l : listeners) {
			l.serviceReply(descriptor);
		}
	}
	
	public void removeServiceBrowserListener(ServiceExplorerListener l) {
		listeners.remove(l);
	}

	public void run() {


		while (shouldRun) {
			

			/* listen (briefly) for a reply packet */
			try {
				byte[] buf = new byte[ServiceConstants.DATAGRAM_LENGTH];
				receivedPacket = new DatagramPacket(buf, buf.length);
				socket.receive(receivedPacket); // note timeout in effect
				
				
				if (isReplyPacket()) {
					
					ServiceDescription descriptor;

					/* notes on behavior of descriptors.indexOf(...)
					 * ServiceDescriptor objects check for 'equals()'
					 * based only on the instanceName field. An update
					 * to a descriptor implies we should replace an
					 * entry if we already have one. (Instead of bothing
					 * with the details to determine new vs. update, just
					 * quickly replace any current descriptor.)
					 */

					descriptor = getReplyDescriptor();
					if (descriptor!=null) {
						notifyReply(descriptor);
						receivedPacket = null;
					}
				
				}
				
			}
			catch (SocketTimeoutException ste) {
				/* ignored; this exception is by design to
				 * break the blocking from socket.receive */
			}
			catch (IOException ioe) {
				OOCSIServer.log("Unexpected exception: "+ioe);
				//ioe.printStackTrace();
				/* resume operation */
			}
			
			sendQueuedPacket();
			
		}
	}
	
	protected void sendQueuedPacket() {
		if (queuedPacket==null) { return; }
		try {
			socket.send(queuedPacket);
			queuedPacket = null;
		}
		catch (IOException ioe) {
			OOCSIServer.log("Unexpected exception: "+ioe);
			//ioe.printStackTrace();
			/* resume operation */
		}
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public void startListener() {
		if (myThread == null) {
			shouldRun = true;
			myThread = new Thread(this,"ServiceExplorer");
			myThread.start();
		}
	}

	public void startLookup() {
		if (myTimer==null) {
			myTimer = new Timer("QueryTimer");
			myTimer.scheduleAtFixedRate(new QueryTimerTask(),0L,ServiceConstants.EXPLORER_QUERY_INTERVAL);
		}
	}
	
	public void startSingleLookup() {
		if (myTimer==null) {
			myTimer = new Timer("QueryTimer");
			myTimer.schedule(new QueryTimerTask(), 0L);
			myTimer=null;
		}
	}
	
	public void stopListener() {
		if (myThread != null) {
			shouldRun = false;
			myThread.interrupt();
			myThread = null;
		}
	}

	
	public void stopLookup() {
		if (myTimer != null) {
			myTimer.cancel();
			myTimer=null;
		}
	}
}
