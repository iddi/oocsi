package nl.tue.id.oocsi.server.discovery;

import java.util.Vector;

import nl.tue.id.oocsi.server.OOCSIServer;

/**
 * @author jhu
 * 
 */
public class OOCSIServiceExplorer implements ServiceExplorerListener {

	ServiceExplorer explorer;
	Vector<ServiceDescription> descriptors;

	private String serviceName = ServiceConstants.SERVICE_NAME;
	
	public Vector<ServiceDescription> lookupServiceDescriptors(){
		descriptors = new Vector<ServiceDescription>();
		explorer = new ServiceExplorer();
		explorer.addServiceExplorerListener(this);
		explorer.setServiceName(serviceName);
		explorer.startListener();
		explorer.startLookup();
		OOCSIServer.log("Explorer started. Will search for 2 secs.");
		try {
			Thread.sleep(2000);
		} catch (InterruptedException ie) {
			// ignore
		}
		explorer.stopLookup();
		explorer.stopListener();
		
		return descriptors;
		
	}
	
	public Vector<ServiceDescription> lookupServiceDescriptors(String name){
		this.serviceName = name;
		return lookupServiceDescriptors();
	}


	public String[] list() {
		lookupServiceDescriptors();

		String[] services = new String[descriptors.size()];

		for (int i = 0; i < descriptors.size(); i++) {
			services[i] = descriptors.get(i).getAddress().getHostAddress()
					+ ":" + descriptors.get(i).getPort();
		}

		return services;
	}

	public String[] list(String name) {
		this.serviceName = name;
		return list();
	}

	public void serviceReply(ServiceDescription descriptor) {
		int pos = descriptors.indexOf(descriptor);
		if (pos > -1) {
			descriptors.removeElementAt(pos);
		}
		descriptors.add(descriptor);
	}

}
