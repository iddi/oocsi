package nl.tue.id.oocsi.server;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

import nl.tue.id.oocsi.server.model.Channel;
import nl.tue.id.oocsi.server.model.Client;
import nl.tue.id.oocsi.server.model.Server;
import nl.tue.id.oocsi.server.protocol.Message;
import nl.tue.id.oocsi.server.services.AbstractService;
import nl.tue.id.oocsi.server.services.OSCService;
import nl.tue.id.oocsi.server.services.PresenceTracker;
import nl.tue.id.oocsi.server.services.SocketService;

/**
 * main server component for running OOCSI
 * 
 * @author matsfunk
 * 
 */
public class OOCSIServer extends Server {

	// constants
	public static final String VERSION = "1.23";

	// defaults for different services
	private int maxClients = 100;
	public int port = 4444;
	public boolean isLogging = false;
	public String[] users = null;

	// default channels
	public static final String SERVER = "SERVER";
	public static final String OOCSI_EVENTS = "OOCSI_events";
	public static final String OOCSI_CONNECTIONS = "OOCSI_connections";
	public static final String OOCSI_CHANNELS = "OOCSI_channels";
	public static final String OOCSI_CLIENTS = "OOCSI_clients";
	public static final String OOCSI_METRICS = "OOCSI_metrics";

	// metrics
	private static int messageCount = 0;
	private static int messageTotal = 0;
	private static final long serverStart = System.currentTimeMillis();

	// singleton server instance
	private volatile static OOCSIServer INSTANCE;

	// services
	AbstractService[] services;

	/**
	 * initialize minimal server without any services running
	 * 
	 * @param args
	 * @throws IOException
	 */
	public OOCSIServer() {

		super(new PresenceTracker());

		// thread-safe singleton assignment
		if (INSTANCE == null) {
			synchronized (OOCSIServer.class) {
				if (INSTANCE == null) {
					INSTANCE = this;
				}
			}
		}
	}

	/**
	 * initialize the server and listen for client connects
	 * 
	 * @param args
	 * @throws IOException
	 */
	public OOCSIServer(String[] args) throws IOException {
		this();

		// parse arguments
		parseCommandlineArgs(args);

		// then initialize
		init();
	}

	/**
	 * initialize the server and listen for client connects
	 * 
	 * @param port
	 * @param clients
	 * @param logging
	 * @throws IOException
	 */
	public OOCSIServer(int port, int clients, boolean logging) throws IOException {
		this();

		// assign argument
		this.port = port;
		this.maxClients = clients;
		this.isLogging = logging;

		init();
	}

	/**
	 * initialize the server and listen for client connects
	 * 
	 * @param port
	 * @param clients
	 * @param logging
	 * @param userList
	 * @throws IOException
	 */
	public OOCSIServer(int port, int clients, boolean logging, String[] userList) throws IOException {
		this();

		// assign argument
		this.port = port;
		this.maxClients = clients;
		this.isLogging = logging;
		this.users = userList;

		init();
	}

	/**
	 * return instance of server
	 * 
	 * @return
	 */
	public static OOCSIServer getInstance() {
		return INSTANCE;
	}

	public void destroyInstance() {
		INSTANCE = null;
	}

	/**
	 * initialize the server and listen for client connects
	 * 
	 * @param args
	 * @throws IOException
	 */
	private void init() throws IOException {

		// add OOCSI channels that will deliver meta-data to potentially
		// connected clients
		addChannel(new Channel(OOCSI_CONNECTIONS, PresenceTracker.nullListener));
		addChannel(new Channel(OOCSI_EVENTS, PresenceTracker.nullListener));

		// output status message
		OOCSIServer.log("Started OOCSI server v" + OOCSIServer.VERSION + " for max. " + maxClients + " parallel clients"
		        + (isLogging ? " and activated logging" : "") + ".");

		// start OSC server
		OSCService osc = new OSCService(this, port + 1);

		// start TCP/socket server
		SocketService tcp = new SocketService(this, port, users);

		// start services
		startServices(new AbstractService[] { tcp, osc });

		// start timer for posting channel and client information to the respective channels
		try {
			Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(new StatusTimeTask(), 5, 1,
			        TimeUnit.SECONDS);
		} catch (Exception e) {
			e.printStackTrace();
			OOCSIServer.log("Exception on StatusTimeTask: " + e.getMessage());
		}
	}

	/**
	 * start given services
	 * 
	 * @param services
	 */
	private void startServices(AbstractService[] services) {

		// first stop all running services
		stop();

		// start new services
		for (final AbstractService service : services) {
			new Thread(new Runnable() {
				public void run() {
					service.start();
				}
			}).start();
		}

		// keep record of newly started services
		this.services = services;
	}

	/**
	 * stop all running services
	 * 
	 */
	public void stop() {
		if (services == null) {
			return;
		}

		for (AbstractService service : services) {
			service.stop();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.tue.id.oocsi.server.model.Channel#getChannel(java.lang.String)
	 */
	@Override
	public Channel getChannel(String channelName) {
		Channel c = super.getChannel(channelName);

		// intercept for OSC
		if (c == null && channelName.startsWith("osc://")) {
			c = getChannel(OSCService.OSC);
		}

		return c;
	}

	/**
	 * retrieve the max number of clients on this server
	 * 
	 * @return
	 */
	public int getMaxClients() {
		return this.maxClients;
	}

	/**
	 * set the max number of clients on this server
	 * 
	 * @param maxClients
	 */
	public void setMaxClients(int maxClients) {
		this.maxClients = maxClients;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.tue.id.oocsi.server.model.Server#canAcceptClient(nl.tue.id.oocsi.server.model.Client)
	 */
	@Override
	public boolean canAcceptClient(Client c) {
		return getClients().size() < maxClients;
	}

	/** STATIC METHODS *************************************************************/

	public static void main(String[] args) {

		// check dependencies (Jackson ObjectMapper)
		new ObjectMapper();

		// start socket server
		try {
			new OOCSIServer(args);
		} catch (IOException e) {
			// e.printStackTrace();
		} finally {
			// done
		}
	}

	/**
	 * logging of a general server event (can be switched off with startup parameter '-logging')
	 * 
	 * @param message
	 */
	public static void log(String message) {
		if (INSTANCE.isLogging) {
			INSTANCE.internalLog(message);
		}
	}

	/**
	 * internal logging function that can be overridden by a subclass
	 * 
	 * @param message
	 */
	protected void internalLog(String message) {
		System.out.println(new Date() + " " + message);
	}

	/**
	 * logging of event (can be switched off with startup parameter '-logging')
	 * 
	 * @param sender
	 * @param recipient
	 * @param data
	 * @param timestamp
	 */
	public static void logEvent(String sender, String channel, String recipient, Map<String, Object> data,
	        Date timestamp) {

		if (SERVER.equals(sender) || OOCSI_EVENTS.equals(sender) || OOCSI_EVENTS.equals(recipient)
		        || OOCSI_METRICS.equals(recipient) || OOCSI_CONNECTIONS.equals(recipient)) {
			return;
		}

		// log metrics
		messageCount++;
		messageTotal++;

		if (INSTANCE.isLogging) {
			if (channel.length() == 0) {
				log(OOCSI_EVENTS + " " + sender + " --> " + recipient);
			} else {
				log(OOCSI_EVENTS + " " + sender + " --( " + channel + " )--> " + recipient);
			}

			Channel logChannel = INSTANCE.getChannel(OOCSI_EVENTS);
			if (logChannel != null) {

				// strip secret data items starting with '_'
				LongSummaryStatistics lss = data.entrySet().stream().filter(e -> !e.getKey().startsWith("_"))
				        .collect(Collectors.summarizingLong(e -> e.getValue().toString().length()));

				Map<String, Object> eventStats = new HashMap<>();
				eventStats.put("size", lss.getSum());
				eventStats.put("count", lss.getCount());

				Message message = new Message(SERVER, OOCSI_EVENTS, timestamp, eventStats);
				message.addData("PUB", sender);
				message.addData("CHANNEL", channel);
				message.addData("SUB", recipient);
				logChannel.send(message);
			}
		}
	}

	/**
	 * logging of connection/channel update (can be switched off with startup parameter '-logging')
	 * 
	 * @param message
	 */
	public static void logConnection(String client, String channel, String operation, Date timestamp) {

		if (OOCSI_EVENTS.equals(channel) || OOCSI_CONNECTIONS.equals(channel)) {
			return;
		}

		if (INSTANCE.isLogging) {
			log(OOCSI_CONNECTIONS + " " + client + "->" + channel + " (" + operation + ")");

			Channel logChannel = INSTANCE.getChannel(OOCSI_CONNECTIONS);
			if (logChannel != null) {
				Message message = new Message(SERVER, OOCSI_CONNECTIONS, timestamp);
				message.addData("CLIENT", client);
				message.addData("CHANNEL", channel);
				message.addData("OP", operation);
				logChannel.send(message);
			}
		}
	}

	/**
	 * parses the command line arguments
	 */
	public void parseCommandlineArgs(String[] args) {
		for (int i = 0; i < args.length; i++) {
			String argument = args[i];

			if (argument.equals("-port") && args.length >= i + 2) {
				this.port = Integer.parseInt(args[i + 1]);
			} else if (argument.equals("-clients") && args.length >= i + 2) {
				this.maxClients = Integer.parseInt(args[i + 1]);
			} else if (argument.equals("-logging")) {
				this.isLogging = true;
			} else if (argument.equals("-users") && args.length >= i + 2) {
				String userList = args[i + 1];
				if (userList.matches(
				        "^([a-zA-Z0-9_\\-.]+:[a-zA-Z0-9_\\-.%$]+;)*([a-zA-Z0-9_\\-.]+:[a-zA-Z0-9_\\-.%$]+);*$")) {
					users = userList.split(";");
				}
			}
		}
	}

	class StatusTimeTask implements Runnable {

		public StatusTimeTask() {
			OOCSIServer.log("StatusTimeTask launched");
		}

		@Override
		public void run() {
			try {
				statusTask();
			} catch (Exception e) {
				OOCSIServer.log("Exception in StatusTimeTask: " + e.getMessage());
				e.printStackTrace();
			}
		}

		/**
		 * run the status task
		 * 
		 */
		public void statusTask() {
			long start = System.currentTimeMillis();

			// clean up first
			closeStaleClients();
			closeEmptyChannels();

			long afterCleans = System.currentTimeMillis();

			// keep-alive ping-pong with socket clients
			for (Client client : INSTANCE.getClients()) {
				// only ping if last action is at least 5 seconds ago
				if (client.lastAction() + 5000 < start) {
					client.ping();
				}
			}

			long afterPings = System.currentTimeMillis();

			// check if we have a subscriber for public channel information
			Channel channels = INSTANCE.getChannel(OOCSI_CHANNELS);
			if (channels != null) {
				Message message = new Message(SERVER, OOCSI_CHANNELS);
				message.addData("channels", INSTANCE.getChannelList());
				channels.send(message);
			}

			// check if we have a subscriber for public client information
			Channel clients = INSTANCE.getChannel(OOCSI_CLIENTS);
			if (clients != null) {
				Message message = new Message(SERVER, OOCSI_CLIENTS);
				message.addData("clients", INSTANCE.getClientList());
				clients.send(message);
			}

			// check first-level channels for channel subscribers
			for (Channel channel : INSTANCE.getChannels()) {
				Channel channelSubscription = INSTANCE.getChannel(channel.getName() + "/?");
				if (channelSubscription != null) {
					Message message = new Message(SERVER, channelSubscription.getName());
					message.addData("channels", channel.getChannelList());
					channelSubscription.send(message);
				}
			}

			long afterFirstMetrics = System.currentTimeMillis();

			// check if we have a subscriber for public client information
			Channel metrics = INSTANCE.getChannel(OOCSI_METRICS);
			if (metrics != null) {
				Message message = new Message(SERVER, OOCSI_METRICS);

				// millis since startup
				message.addData("uptime", System.currentTimeMillis() - serverStart);

				// total messages since startup
				message.addData("messagesTotal", messageTotal);

				// messages per second
				message.addData("messages", messageCount);

				// channel count
				message.addData("channels", INSTANCE.subChannels.size());

				// client count
				message.addData("clients", INSTANCE.clients.size());

				// report!
				metrics.send(message);
			}

			// reset message count
			messageCount = 0;

			// log out if status task took too long
			if (System.currentTimeMillis() - start > 100) {
				OOCSIServer.log("Status task took longer than 100ms: " + (System.currentTimeMillis() - start));
				OOCSIServer.log("Also cleans took: " + (afterCleans - start));
				OOCSIServer.log("Also pings took: " + (afterPings - afterCleans));
				OOCSIServer.log("Also 1st metrics took: " + (afterFirstMetrics - afterPings));
				OOCSIServer.log("Also 2nd metrics took: " + (System.currentTimeMillis() - afterFirstMetrics));
			}
		}
	}

}
