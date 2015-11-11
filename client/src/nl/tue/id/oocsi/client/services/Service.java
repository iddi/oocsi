package nl.tue.id.oocsi.client.services;

import java.util.LinkedList;
import java.util.List;

import nl.tue.id.oocsi.client.OOCSIClient;

public class Service {

	public String name;
	public String handle;
	public String uuid;
	public String category;
	public List<ServiceMethod> methods = new LinkedList<ServiceMethod>();

	public ServiceMethod newServiceMethod() {
		ServiceMethod serviceMethod = new ServiceMethod(name);
		methods.add(serviceMethod);
		return serviceMethod;
	}

	public static class ServiceMethod {
		public String serviceName;
		public String name;
		public String handle;
		public String uuid;

		public List<ServiceField<?>> input = new LinkedList<Service.ServiceField<?>>();
		public List<ServiceField<?>> output = new LinkedList<Service.ServiceField<?>>();

		/**
		 * hidden constructor, so users will build the method via the service
		 * 
		 * @param serviceName
		 */
		private ServiceMethod(String serviceName) {
			this.serviceName = serviceName;
		}

		/**
		 * register a responder for this method
		 * 
		 * @param oocsi
		 * @param responder
		 */
		public void registerResponder(OOCSIClient oocsi, Responder responder) {
			responder.setOocsi(oocsi);
			responder.setCallName(getName());

			oocsi.register(getName(), responder);
		}

		/**
		 * build a call to a responder on OOCSI for this method
		 * 
		 * @param oocsi
		 * @param timeoutMS
		 * @param maxResponses
		 * @return
		 */
		public OOCSICall buildCall(OOCSIClient oocsi, int timeoutMS, int maxResponses) {
			OOCSICall call = new OOCSICall(oocsi, getName(), timeoutMS, maxResponses);
			for (ServiceField<?> field : input) {
				call.data(field.name, field.defaultValue);
			}

			return call;
		}

		/**
		 * get method name respective the service
		 * 
		 * @return
		 */
		public String getName() {
			return serviceName + '.' + name;
		}
	}

	public static class ServiceField<T> {

		public String name;
		public T defaultValue;
		public T value;

		public ServiceField(String name) {
			this.name = name;
		}

		public ServiceField(String name, T defaultValue) {
			this.name = name;
			this.defaultValue = defaultValue;
		}

		public void set(T value) {
			this.value = value;
		}

		public String getType() {
			return this.value.getClass().getName();
		}
	}

}
