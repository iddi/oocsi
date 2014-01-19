package nl.tue.id.oocsi.server.discovery;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.UnknownHostException;

import nl.tue.id.oocsi.server.OOCSIServer;

/**
 * @author jhu
 * 
 */
public class ServiceDescription implements Comparable<ServiceDescription> {

	public static ServiceDescription parse(String encodedInstanceName,
			String addressAsString, String portAsString) {

		ServiceDescription descriptor = new ServiceDescription();
		try {
			String name = URLDecoder.decode(encodedInstanceName, "UTF-8");
			if (name == null || name.length() == 0) {
				/* warning: check API docs for exact behavior of 'decode' */
				return null;
			}
			descriptor.setInstanceName(name);
		} catch (UnsupportedEncodingException uee) {
			OOCSIServer.log("Unexpected exception: " + uee);
			// uee.printStackTrace();
			return null;
		}

		try {
			InetAddress addr = InetAddress.getByName(addressAsString);
			descriptor.setAddress(addr);
		} catch (UnknownHostException uhe) {
			OOCSIServer.log("Unexpected exception: " + uhe);
			// uhe.printStackTrace();
			return null;
		}

		try {
			int p = Integer.parseInt(portAsString);
			descriptor.setPort(p);
		} catch (NumberFormatException nfe) {
			OOCSIServer.log("Unexpected exception: " + nfe);
			// nfe.printStackTrace();
			return null;
		}

		return descriptor;
	}
	
	private String instanceName;
	private int port;

	private InetAddress address;

	public ServiceDescription() {
	}

	public int compareTo(ServiceDescription sd) throws ClassCastException {
		if (sd == null) {
			throw new NullPointerException();
		}
		if (sd == this) {
			return 0;
		}

		return getInstanceName().compareTo(sd.getInstanceName());
	}

	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof ServiceDescription)) {
			return false;
		}
		ServiceDescription descriptor = (ServiceDescription) o;
		return descriptor.getInstanceName().equals(getInstanceName());
	}

	public InetAddress getAddress() {
		return address;
	}

	protected String getAddressAsString() {
		return getAddress().getHostAddress();
	}

	protected String getEncodedInstanceName() {
		try {
			return URLEncoder.encode(getInstanceName(), "UTF-8");
		} catch (UnsupportedEncodingException uee) {
			return null;
		}
	}

	public String getInstanceName() {
		return instanceName;
	}

	public int getPort() {
		return port;
	}

	protected String getPortAsString() {
		return "" + getPort();
	}

	public int hashCode() {
		return getInstanceName().hashCode();
	}

	public void setAddress(InetAddress serviceAddress) {
		this.address = serviceAddress;
	}

	public void setInstanceName(String serviceDescription) {
		this.instanceName = serviceDescription;
	}

	public void setPort(int servicePort) {
		this.port = servicePort;
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append(getEncodedInstanceName());
		buf.append(" ");
		buf.append(getAddressAsString());
		buf.append(" ");
		buf.append(getPortAsString());
		return buf.toString();
	}
}
