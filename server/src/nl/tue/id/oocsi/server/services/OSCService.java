package nl.tue.id.oocsi.server.services;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Date;

import nl.tue.id.oocsi.server.OOCSIServer;
import nl.tue.id.oocsi.server.model.Channel;
import nl.tue.id.oocsi.server.model.Server;
import nl.tue.id.oocsi.server.protocol.Message;

import com.illposed.osc.AddressSelector;
import com.illposed.osc.OSCListener;
import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortIn;
import com.illposed.osc.OSCPortOut;

/**
 * OSC service component
 * 
 * @author matsfunk
 * 
 */
public class OSCService extends AbstractService {

	public static final String OSC = "OSC";

	private int oscInPort;
	private int oscOutPort;
	private OSCPortOut opOut;

	public OSCService(final Server server, int port) {
		super(server);

		// configure OSC server
		oscInPort = port + 1;
		oscOutPort = port + 2;
	}

	@Override
	public void start() {
		try {

			// OSC input channel
			// ------------------------------------
			OSCPortIn opIn = new OSCPortIn(oscInPort);
			OSCListener oscListener = new OSCListener() {
				public void acceptMessage(Date timestamp, OSCMessage message) {
					if (timestamp.getTime() < System.currentTimeMillis() + 1000
							&& timestamp.getTime() > System.currentTimeMillis() - 1000) {

						String address = message.getAddress();
						String key = null;
						int index = address.lastIndexOf('/');
						if (index > -1) {
							if (index < address.length() - 1) {
								key = address.substring(index);
							}
							address = address.substring(0, index);
						}

						Channel c = server.getChannel(address);
						Message m = new Message("osc://", address, timestamp);
						m.addData(key != null ? key : "data", message.getArguments());
						c.send(m);
					}
				}
			};
			opIn.addListener(new AddressSelector() {
				public boolean matches(String address) {
					int index = address.lastIndexOf('/');
					if (index > -1) {
						address = address.substring(0, index);
					}

					return server.getChannel(address) != null;
				}
			}, oscListener);

			// OSC output channel
			// ------------------------------------
			opOut = new OSCPortOut(new InetSocketAddress(oscOutPort).getAddress());
			Channel oscOutPortChannel = new Channel(OSC, presence) {

				@Override
				public void send(Message message) {
					try {
						String recipient = message.recipient;
						if (recipient.startsWith("osc://")) {
							recipient = recipient.replaceFirst("osc:/", "");
							opOut.send(new OSCMessage(recipient, message.data.values()));
						}
					} catch (IOException e) {
						// do nothing
					}
				}
			};
			server.addChannel(oscOutPortChannel);

		} catch (IOException e) {
			OOCSIServer.log("[OSC server]: Could not listen on ports: " + oscInPort + ", " + (oscInPort + 1));
		}

		// log output
		InetAddress addr;
		try {
			addr = InetAddress.getLocalHost();
			String hostname = addr.getHostName();

			OOCSIServer.log("[OSC server]: Started OSC service @ local address '" + hostname + "' on port "
					+ (oscInPort + 1) + " for OSC.");
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void stop() {
		opOut.close();
	}
}
