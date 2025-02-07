package nl.tue.id.oocsi.server.services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import nl.tue.id.oocsi.server.OOCSIServer;
import nl.tue.id.oocsi.server.model.Client;
import nl.tue.id.oocsi.server.model.Server;
import nl.tue.id.oocsi.server.protocol.Message;

public class NIOSocketService extends AbstractService {

	private final int port;
	private final String[] registeredUsers;

	// current list of connected NIO clients
	private final Map<SocketChannel, NIOSocketClient> nioClients = new ConcurrentHashMap<>();
	private final Map<SocketChannel, StringBuffer> nioClientInputBuffer = new ConcurrentHashMap<>();
	private boolean serverSocketActive = true;
	private ServerSocketChannel serverSocketChannel;

	/**
	 * create a TCP socket service for OOCSI based on Java NIO
	 * 
	 * @param server
	 * @param port
	 */
	public NIOSocketService(Server server, int port, String[] registeredUsers) {
		super(server);

		this.port = port;
		this.registeredUsers = registeredUsers;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.tue.id.oocsi.server.services.AbstractService#register(nl.tue.id.oocsi.server.model.Client)
	 */
	@Override
	public boolean register(Client client) {

		// non-private clients, normal procedure
		if (!client.isPrivate()) {
			return super.register(client);
		}

		// for private clients, first check whether it needs to comply to existing users
		final String name = client.getName();
		if (registeredUsers != null) {
			for (String user : registeredUsers) {
				if (user == null || !user.replaceFirst(":.*", "").equals(name)) {
					continue;
				}

				// if there is a match, replace client
				if (client.validate(user)) {
					return super.register(client);
				} else {
					return false;
				}
			}
		}

		// for non-private clients
		return !client.isPrivate() && super.register(client);
	}

	@Override
	public void start() {
		serverSocketChannel = null;
		try {
			serverSocketChannel = ServerSocketChannel.open();
			serverSocketChannel.configureBlocking(false);
			serverSocketChannel.socket().bind(new InetSocketAddress(port));

			Selector readSelector = Selector.open();
			serverSocketChannel.register(readSelector, SelectionKey.OP_ACCEPT);
			Selector writeSelector = Selector.open();

			// start writer loop in separate thread
			Executors.newSingleThreadExecutor().execute(() -> {
				try {
					while (serverSocketActive) {
						// check if there is anything to write
						if (!writableNIOClients()) {
							try {
								Thread.sleep(10);
							} catch (InterruptedException e) {
							}

							continue;
						}

						writeSelector.selectNow();
						Iterator<SelectionKey> keys = writeSelector.selectedKeys().iterator();
						while (keys.hasNext()) {
							try {
								SelectionKey selectionKey = (SelectionKey) keys.next();
								if (selectionKey.isWritable()) {
									handleWriteOp(selectionKey);
								}
							} catch (CancelledKeyException cke) {
							} catch (Exception e) {
								e.printStackTrace();
							}
							keys.remove();
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			});

			// accept and read loop
			while (serverSocketActive) {
				readSelector.select();
				for (SelectionKey selectionKey : readSelector.selectedKeys()) {
					try {
						if (selectionKey.isAcceptable()) {
							SocketChannel socketChannel = serverSocketChannel.accept();
							if (socketChannel != null) {
								socketChannel.configureBlocking(false);
								socketChannel.register(selectionKey.selector(), SelectionKey.OP_READ);
								socketChannel.register(writeSelector, SelectionKey.OP_WRITE);
							}
						} else if (selectionKey.isReadable()) {
							handleReadOp(selectionKey);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * check whether there is any outgoing data from any client
	 * 
	 * @return
	 */
	private boolean writableNIOClients() {
		return nioClients.values().parallelStream().anyMatch(nc -> !nc.pendingData.isEmpty());
	}

	/**
	 * read from NIO socket client and process the data
	 * 
	 * @param selectionKey
	 */
	private void handleReadOp(SelectionKey selectionKey) {
		SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
		ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
		int read = 0;
		try {
			read = socketChannel.read(byteBuffer);
			if (read == -1) {
				// if connection is closed by the client
				if (nioClients.containsKey(socketChannel)) {
					// remove the client first
					NIOSocketClient client = nioClients.get(socketChannel);
					server.removeClient(client);
					nioClients.remove(socketChannel);
					nioClientInputBuffer.remove(socketChannel);
				}

				// then close channel
				socketChannel.close();
				return;
			}
		} catch (IOException e) {
			// connection reset
			if (nioClients.containsKey(socketChannel)) {
				// remove the client first
				NIOSocketClient client = nioClients.get(socketChannel);
				server.removeClient(client);
				nioClients.remove(socketChannel);
				nioClientInputBuffer.remove(socketChannel);
			}

			try {
				// then close channel
				socketChannel.close();
			} catch (IOException e1) {
			}

			// always return in case of exceptions
			return;
		}

		NIOSocketClient client = nioClients.get(socketChannel);
		if (client == null) {
			// do the client init based on read
			String inputLine = new String(byteBuffer.array(), 0, read);
			StringBuffer sb = nioClientInputBuffer.computeIfAbsent(socketChannel, s -> new StringBuffer())
			        .append(inputLine);
			int nlIndex = sb.indexOf("\n");
			// if no newline found, buffer input till next read
			if (nlIndex == -1) {
				return;
			}
			// if found, remove this part from the buffer, but keeps the rest of the buffer for further use
			else {
				inputLine = sb.substring(0, nlIndex + 1);
				sb.delete(0, nlIndex + 1);
			}

			// remove any whitespace at begin and end
			inputLine = inputLine.trim();

			// check input line for exceptional values that cannot be handled safely
			// do some filtering for SSH clients connecting and other abuse
			if (inputLine.length() > 200) {
				OOCSIServer.log("Killed client connection for [length]: " + inputLine);
				return;
			}
			if (!inputLine.matches("\\p{ASCII}+$")) {
				OOCSIServer.log("Killed client connection for [non-ASCII chars]: " + inputLine);
				return;
			}
			if (inputLine.contains("OpenSSH") || inputLine.contains("libssh")) {
				OOCSIServer.log("Killed client connection for [suspicious client]: " + inputLine);
				return;
			}
			if (inputLine.matches(".*\\s.*")) {
				OOCSIServer.log(
				        "Killed client connection because client name contains whitespace characters: " + inputLine);
				return;
			}

			// check input line for workable deviations from protocol
			// remove starting or trailing slashes
			inputLine = inputLine.replaceAll("^/|/$", "");

			// if there are one or more hashes in the inputLine, we need to generate a client name
			for (int i = 0; i < 20 && inputLine.contains("#"); i++) {
				String tempHandle = replaceHashesWithDigits(inputLine);
				if (server.getClient(tempHandle) == null) {
					inputLine = tempHandle;
					break;
				}
			}

			// if ok, register NIOSocketClient
			NIOSocketClient newClient = new NIOSocketClient(inputLine, presence);
			// register for NIO
			nioClients.put(socketChannel, newClient);

			// register on internal protocol
			if (register(newClient)) {
				// say hi
				newClient.sayHi();

				// log connection creation
				if (!newClient.isPrivate()) {
					OOCSIServer.logConnection(newClient.getName(), "OOCSI", "client connected", new Date());
				}
			} else {
				if (newClient.getName().contains(" ")) {
					newClient.send("error (name cannot contain spaces: " + newClient.getName() + ")");
				} else if (newClient.isPrivate()) {
					newClient.send("error (password wrong for name: " + newClient.getName() + ")");
				} else {
					newClient.send("error (name already registered: " + newClient.getName() + ")");
				}
				server.removeClient(newClient);
			}
		} else {
			// do the client init based on read
			String inputLine = new String(byteBuffer.array(), 0, read);
			StringBuffer sb = nioClientInputBuffer.computeIfAbsent(socketChannel, s -> new StringBuffer())
			        .append(inputLine);

			// find first newline
			int nlIndex = sb.indexOf("\n");
			while (nlIndex > -1) {
				// if found, remove this part from the buffer, but keeps the rest of the buffer for further use
				inputLine = sb.substring(0, nlIndex + 1).trim();
				sb.delete(0, nlIndex + 1);

				// send data to client
				client.processNIOInput(inputLine);

				// check if client should be terminated
				if (!client.isConnected()) {
					try {
						socketChannel.write(ByteBuffer.wrap("bye\n".getBytes()));
						nioClients.remove(socketChannel);
						nioClientInputBuffer.remove(socketChannel);
						socketChannel.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

				// find next newline
				nlIndex = sb.indexOf("\n");
			}
		}
	}

	/**
	 * write pending data to NIO client
	 * 
	 * @param selectionKey
	 */
	private void handleWriteOp(SelectionKey selectionKey) {
		try {
			SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
			socketChannel.socket().setTcpNoDelay(true);
			NIOSocketClient client = nioClients.get(socketChannel);
			if (client != null) {
				if (client.isConnected()) {
					Queue<ByteBuffer> pendingData = client.pendingData;
					while (!pendingData.isEmpty() && client.isConnected()) {
						ByteBuffer buf = pendingData.poll();
						if (buf != null) {
							socketChannel.write(buf);
						}
					}
				} else {
					// check if client should be terminated
					socketChannel.write(ByteBuffer.wrap("bye\n".getBytes()));
					nioClients.remove(socketChannel);
					nioClientInputBuffer.remove(socketChannel);
					socketChannel.close();
				}
			}
		} catch (ClosedChannelException e) {
			// it's ok, don't raise alert
			e.printStackTrace();
		} catch (IOException e) {
			// it's ok, don't raise alert
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String replaceHashesWithDigits(String input) {
		StringBuilder result = new StringBuilder(input.length());
		Random RAND = new Random();
		for (int i = 0; i < input.length(); i++) {
			char c = input.charAt(i);
			if (c == '#') {
				result.append(RAND.nextInt(10));
			} else {
				result.append(c);
			}
		}
		return result.toString();
	}

	@Override
	public void stop() {
		// stop loops
		serverSocketActive = false;

		// close server socket
		if (serverSocketChannel != null) {
			try {
				serverSocketChannel.close();
				serverSocketChannel.socket().close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Java NIO Socket Client
	 *
	 */
	class NIOSocketClient extends Client {
		private final ObjectMapper JSON_OBJECT_MAPPER = new ObjectMapper();

		private ClientType type = ClientType.OOCSI;
		private boolean isConnected = true;

		private Queue<ByteBuffer> pendingData = new ConcurrentLinkedQueue<ByteBuffer>();

		public NIOSocketClient(String token, ChangeListener presence) {
			super(token.replace(";", "").replace("(JSON)", "").trim(), presence);

			// select client type
			if (token.contains(";")) {
				type = ClientType.PD;
			} else if (token.contains("(JSON)")) {
				type = ClientType.JSON;
			} else {
				type = ClientType.OOCSI;
			}
		}

		@Override
		public boolean accept(String channelToken) {
			return isConnected;
		}

		/**
		 * say hi to new client
		 * 
		 */
		public void sayHi() {
			if (type == ClientType.JSON) {
				send("{'message' : \"welcome " + getName() + "\"}");
			} else {
				send("welcome " + getName());
			}
		}

		@Override
		public void disconnect() {
			isConnected = false;
		}

		@Override
		public boolean isConnected() {
			return isConnected;
		}

		@Override
		public void ping() {
			send("ping");
		}

		@Override
		public void pong() {
		}

		/**
		 * receive data in a ByteBuffer, that is, split into lines, then handle the lines separately
		 * 
		 * @param input
		 */
		public void processNIOInput(String message) {

			// update last action
			touch();

			// depending on client type, we need to pre-process input
			final String outputLine;
			if (type == ClientType.PD) {
				// then process input
				outputLine = processInput(this, message.replace("'", ",").replace(";", ""));
			} else {
				// then process input
				outputLine = processInput(this, message);
			}

			// write output if necessary
			if (outputLine == null) {
				this.disconnect();
				server.removeClient(this);
			} else if (outputLine.length() > 0) {
				send(outputLine);
			}
		}

		/**
		 * send message to subscribers
		 * 
		 */
		@Override
		public boolean send(Message message) {

			// update last action
			touch();

			if (type == ClientType.OOCSI) {
				send("send " + message.getRecipient() + " " + serializeJava(message.data) + " "
				        + message.getTimestamp().getTime() + " " + message.getSender());
			} else if (type == ClientType.JSON) {
				send(serializeJSON(message.data, message.getRecipient(), message.getTimestamp().getTime(),
				        message.getSender()));
			} else if (type == ClientType.PD) {
				send(message.getRecipient() + " timestamp=" + message.getTimestamp().getTime() + " sender="
				        + message.getSender() + " " + serializePD(message.data));
			} else {
				return false;
			}

			// log this if recipient is this client exactly
			if (message.getRecipient().equals(getName())) {
				OOCSIServer.logEvent(message.getSender(), "", message.getRecipient(), message.data,
				        message.getTimestamp());
			}

			return true;
		}

		private boolean send(String string) {
			// clean the pending data queue if there are too many elements to sent out
			boolean queueFull = false;
			while (pendingData.size() > 20) {
				queueFull = true;
				pendingData.poll();
			}

			if (type == ClientType.PD) {
				string += ';';
			}

			ByteBuffer b = ByteBuffer.wrap((string + "\n").getBytes(Charset.defaultCharset()).clone());
			if (b != null) {
				pendingData.offer(b);
			}

			// return if the send was successful because the queue is not full
			return !queueFull;
		}

		/**
		 * serialize data for OOCSI clients
		 * 
		 * @param data
		 * @return
		 */
		@Deprecated
		private String serializeJava(Map<String, Object> data) {
			Map<String, Object> oocsiData = new HashMap<String, Object>();
			oocsiData.put("error", "Your OOCSI client version is too old, please update.");
			return serializeOOCSIOutput(oocsiData);
		}

		/**
		 * @param data
		 * @return
		 */
		@Deprecated
		private String serializeOOCSIOutput(Map<String, Object> data) {
			// map to serialized java object
			final ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
			try {
				final ObjectOutputStream oos = new ObjectOutputStream(baos);
				oos.writeObject(data);
				final byte[] rawData = baos.toByteArray();
				return new String(Base64.getEncoder().encode(rawData));
			} catch (IOException e) {
				try {
					final ObjectOutputStream oos = new ObjectOutputStream(baos);
					oos.writeObject(new HashMap<String, Object>());
					final byte[] rawData = baos.toByteArray();
					return new String(Base64.getEncoder().encode(rawData));
				} catch (IOException e1) {
					return "";
				}
			}
		}

		/**
		 * serialize data for PD clients; this serialization needs to be flat, i.e., all key-value pairs are on the
		 * highest level; array serialization prioritizes arrays of numbers; strings in array will not work well
		 * 
		 * @param data
		 * @return
		 */
		private String serializePD(Map<String, Object> data) {
			// map to blank separated list
			StringBuilder sb = new StringBuilder();
			for (String key : data.keySet()) {
				Object value = data.get(key);
				if (value instanceof String) {
					sb.append(key + "=" + (String) value + " ");
				} else if (value instanceof ArrayNode) {
					String joinedArray = StreamSupport.stream(((ArrayNode) value).spliterator(), false)
					        .map(JsonNode::asText).collect(Collectors.joining(","));
					sb.append(key + "=" + joinedArray + " ");
				} else {
					// otherwise, just toString()
					sb.append(key + "=" + value.toString() + " ");
				}
			}
			return sb.toString();
		}

		/**
		 * serialize data for JSON clients
		 * 
		 * @param data
		 * @param recipient
		 * @param timestamp
		 * @param sender
		 * @return
		 */
		private String serializeJSON(Map<String, Object> data, String recipient, long timestamp, String sender) {
			ObjectNode je = JSON_OBJECT_MAPPER.valueToTree(data);

			// add OOCSI properties
			je.put("recipient", recipient);
			je.put("timestamp", timestamp);
			je.put("sender", sender);

			// serialize
			return je.toString();
		}
	}

}
