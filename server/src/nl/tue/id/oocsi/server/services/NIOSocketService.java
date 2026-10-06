package nl.tue.id.oocsi.server.services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import nl.tue.id.oocsi.server.OOCSIServer;
import nl.tue.id.oocsi.server.model.Channel;
import nl.tue.id.oocsi.server.model.Client;
import nl.tue.id.oocsi.server.model.Server;
import nl.tue.id.oocsi.server.protocol.Message;

public class NIOSocketService extends AbstractService {

	private static final int MAX_PRE_AUTH_BUFFER = 256;
	private static final int MAX_BUFFER_SIZE = 65536;

	private final int port;
	private final String[] registeredUsers;

	// current list of connected NIO clients
	private final Map<SocketChannel, NIOSocketClient> nioClients = new ConcurrentHashMap<>();
	private final Map<SocketChannel, StringBuilder> nioClientInputBuffer = new ConcurrentHashMap<>();
	private final Queue<SocketChannel> pendingWriteInterest = new ConcurrentLinkedQueue<>();
	private final AtomicBoolean wakeupPending = new AtomicBoolean(false);
	private final ByteBuffer readBuffer = ByteBuffer.allocateDirect(1024);
	private volatile boolean serverSocketActive = true;
	private volatile Thread selectorThread;
	private ServerSocketChannel serverSocketChannel;
	private Selector selector;

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
			ServerSocket serverSocket = serverSocketChannel.socket();
			serverSocket.setPerformancePreferences(0, 2, 1);
			serverSocket.setReuseAddress(true);
			serverSocket.bind(new InetSocketAddress(port));

			selector = Selector.open();
			serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

			selectorThread = Thread.currentThread();

			// accept and read loop
			while (serverSocketActive && selector.isOpen()) {
				// process pending write interests
				SocketChannel ch;
				while ((ch = pendingWriteInterest.poll()) != null) {
					SelectionKey k = ch.keyFor(selector);
					if (k != null && k.isValid()) {
						try {
							k.interestOpsOr(SelectionKey.OP_WRITE);
						} catch (CancelledKeyException ignored) {
						}
					}
				}

				try {
					wakeupPending.set(false);
					if (selector.select(20) == 0) {
						continue;
					}
				} catch (ClosedSelectorException e) {
					break;
				}

				Iterator<SelectionKey> it = selector.selectedKeys().iterator();
				while (it.hasNext()) {
					SelectionKey selectionKey = it.next();
					it.remove();

					if (!selectionKey.isValid()) {
						continue;
					}

					try {
						// accept operation
						if (selectionKey.isAcceptable()) {
							SocketChannel socketChannel = serverSocketChannel.accept();
							if (socketChannel != null) {
								socketChannel.configureBlocking(false);
								socketChannel.register(selector, SelectionKey.OP_READ);
								socketChannel.socket().setPerformancePreferences(0, 2, 1);
								socketChannel.socket().setTcpNoDelay(true);
							}
						} else {
							// read operation
							if (selectionKey.isValid() && selectionKey.isReadable()) {
								handleReadOp(selectionKey);
							}

							// perform write operation
							if (selectionKey.isValid() && selectionKey.isWritable()) {
								handleWriteOp(selectionKey);
							}
						}
					} catch (CancelledKeyException | ClosedChannelException e) {
						// channel closed or cancelled during processing
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		} catch (Exception e) {
			if (serverSocketActive) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * read from NIO socket client and process the data
	 * 
	 * @param selectionKey
	 */
	private void handleReadOp(SelectionKey selectionKey) {
		SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
		int read = 0;
		try {
			readBuffer.clear();
			read = socketChannel.read(readBuffer);
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
			readBuffer.flip();
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

		String inputLine = StandardCharsets.UTF_8.decode(readBuffer).toString();
		NIOSocketClient client = nioClients.get(socketChannel);
		if (client == null) {
			// do the client init based on read
			StringBuilder sb = nioClientInputBuffer.computeIfAbsent(socketChannel, s -> new StringBuilder())
			        .append(inputLine);
			if (sb.length() > MAX_PRE_AUTH_BUFFER) {
				nioClientInputBuffer.remove(socketChannel);
				try {
					socketChannel.close();
				} catch (IOException ignored) {
				}
				return;
			}
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

			// remove starting or trailing slashes
			inputLine = inputLine.replaceAll("^/|/$", "");

			// check input line for exceptional values that cannot be handled safely
			// do some filtering for SSH clients connecting, invalid client names, and other abuse
			String clientHandle = Channel.parseChannelName(inputLine.replace(";", "").replace("(JSON)", "").trim());
			if (inputLine.length() == 0 || inputLine.length() > 200 || !inputLine.matches("\\p{ASCII}+$")
			        || inputLine.contains("OpenSSH") || inputLine.contains("libssh")
			        || inputLine.matches(".*\\s.*") || !Server.isValidClientName(clientHandle)) {
				nioClientInputBuffer.remove(socketChannel);
				try {
					socketChannel.close();
				} catch (IOException ignored) {
				}
				return;
			}

			// if there are one or more hashes in the inputLine, we need to generate a client name
			for (int i = 0; i < 20 && inputLine.contains("#"); i++) {
				String tempHandle = replaceHashesWithDigits(inputLine);
				if (server.getClient(tempHandle) == null) {
					inputLine = tempHandle;
					break;
				}
			}

			// if ok, register NIOSocketClient
			NIOSocketClient newClient = new NIOSocketClient(inputLine, presence, selectionKey);

			// register on internal protocol
			if (register(newClient)) {
				// register for NIO only on successful registration
				nioClients.put(socketChannel, newClient);

				// say hi
				newClient.sayHi();

				// log connection creation
				if (!newClient.isPrivate()) {
					OOCSIServer.logConnection(newClient.getName(), "OOCSI", "client connected", new Date());
				}

				// process any remaining complete lines already buffered during handshake
				int lineStart = 0;
				int nextNl = sb.indexOf("\n", lineStart);
				while (nextNl > -1) {
					String pendingLine = sb.substring(lineStart, nextNl + 1).trim();
					lineStart = nextNl + 1;
					newClient.processNIOInput(pendingLine);
					if (!newClient.isConnected()) {
						try {
							socketChannel.close();
						} catch (IOException ignored) {
						}
						return;
					}
					nextNl = sb.indexOf("\n", lineStart);
				}
				if (lineStart > 0) {
					sb.delete(0, lineStart);
					if (sb.capacity() > 8192 && sb.length() < 1024) {
						sb.trimToSize();
					}
				}
			} else {
				String errMsg;
				if (newClient.getName().contains(" ")) {
					errMsg = "error (name cannot contain spaces: " + newClient.getName() + ")";
				} else if (newClient.isPrivate()) {
					errMsg = "error (password wrong for name: " + newClient.getName() + ")";
				} else {
					errMsg = "error (name already registered: " + newClient.getName() + ")";
				}
				try {
					socketChannel.write(ByteBuffer.wrap((errMsg + "\n").getBytes(StandardCharsets.UTF_8)));
				} catch (IOException ignored) {
				}
				server.removeClient(newClient);
				nioClients.remove(socketChannel);
				nioClientInputBuffer.remove(socketChannel);
				try {
					socketChannel.close();
				} catch (IOException ignored) {
				}
			}
		} else {
			// do the client process based on read
			StringBuilder sb = nioClientInputBuffer.computeIfAbsent(socketChannel, s -> new StringBuilder())
			        .append(inputLine);

			if (sb.length() > MAX_BUFFER_SIZE) {
				nioClientInputBuffer.remove(socketChannel);
				nioClients.remove(socketChannel);
				server.removeClient(client);
				try {
					socketChannel.close();
				} catch (IOException ignored) {
				}
				return;
			}

			// find newlines using line cursor
			int lineStart = 0;
			int nlIndex = sb.indexOf("\n", lineStart);
			while (nlIndex > -1) {
				// if found, extract line without copying buffer each time
				inputLine = sb.substring(lineStart, nlIndex + 1).trim();
				lineStart = nlIndex + 1;

				// send data to client
				client.processNIOInput(inputLine);

				// check if client should be terminated
				if (!client.isConnected()) {
					try {
						socketChannel.write(ByteBuffer.wrap("bye\n".getBytes(StandardCharsets.UTF_8)));
						nioClients.remove(socketChannel);
						nioClientInputBuffer.remove(socketChannel);
						socketChannel.close();
					} catch (IOException e) {
						// e.printStackTrace();
					}
					return;
				}

				// find next newline
				nlIndex = sb.indexOf("\n", lineStart);
			}

			if (lineStart > 0) {
				sb.delete(0, lineStart);
				// Cap and reset backing capacity if it grew large
				if (sb.capacity() > 8192) {
					if (sb.length() == 0) {
						nioClientInputBuffer.put(socketChannel, new StringBuilder());
					} else {
						nioClientInputBuffer.put(socketChannel, new StringBuilder(sb));
					}
				}
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
			NIOSocketClient client = nioClients.get(socketChannel);
			if (client != null) {
				if (client.isConnected()) {
					Queue<ByteBuffer> pendingData = client.pendingData;
					while (!pendingData.isEmpty() && client.isConnected()) {
						ByteBuffer buf = pendingData.peek();
						if (buf == null) {
							pendingData.poll();
							continue;
						}
						socketChannel.write(buf);
						if (buf.hasRemaining()) {
							// TCP send buffer is full; yield and retain OP_WRITE interest
							return;
						}
						pendingData.poll();
					}
					// Queue drained: clear OP_WRITE interest
					if (selectionKey.isValid()) {
						selectionKey.interestOpsAnd(~SelectionKey.OP_WRITE);
					}
				} else {
					// check if client should be terminated
					socketChannel.write(ByteBuffer.wrap("bye\n".getBytes(StandardCharsets.UTF_8)));
					nioClients.remove(socketChannel);
					nioClientInputBuffer.remove(socketChannel);
					socketChannel.close();
				}
			}
		} catch (ClosedChannelException e) {
			// it's ok, don't raise alert
		} catch (IOException e) {
			// it's ok, don't raise alert
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String replaceHashesWithDigits(String input) {
		StringBuilder result = new StringBuilder(input.length());
		for (int i = 0; i < input.length(); i++) {
			char c = input.charAt(i);
			if (c == '#') {
				result.append(ThreadLocalRandom.current().nextInt(10));
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

		if (selector != null) {
			selector.wakeup();
			try {
				selector.close();
			} catch (IOException ignored) {
			}
		}

		// close server socket
		if (serverSocketChannel != null) {
			try {
				serverSocketChannel.close();
				if (serverSocketChannel.socket() != null) {
					serverSocketChannel.socket().close();
				}
			} catch (IOException ignored) {
			}
		}
	}

	/**
	 * Java NIO Socket Client
	 *
	 */
	class NIOSocketClient extends Client {
		private static final ObjectMapper JSON_OBJECT_MAPPER = JsonMapper.builder()
		        .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
		        .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true).build();

		private static final String LEGACY_UPGRADE_NOTICE = serializeOOCSIOutputStatic();

		private static String serializeOOCSIOutputStatic() {
			final ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
			try {
				final ObjectOutputStream oos = new ObjectOutputStream(baos);
				Map<String, Object> oocsiData = new HashMap<String, Object>();
				oocsiData.put("error", "Your OOCSI client version is too old, please update.");
				oos.writeObject(oocsiData);
				final byte[] rawData = baos.toByteArray();
				return new String(Base64.getEncoder().encode(rawData));
			} catch (IOException e) {
				return "";
			}
		}

		private final ClientType type;
		private final SelectionKey selectionKey;

		private volatile boolean isConnected = true;
		private Queue<ByteBuffer> pendingData = new ConcurrentLinkedQueue<ByteBuffer>();

		public NIOSocketClient(String token, ChangeListener presence, SelectionKey selectionKey) {
			super(token.replace(";", "").replace("(JSON)", "").trim(), presence);

			this.selectionKey = selectionKey;

			// select client type
			if (token.contains(";")) {
				this.type = ClientType.PD;
			} else if (token.contains("(JSON)")) {
				this.type = ClientType.JSON;
			} else {
				this.type = ClientType.OOCSI;
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
				send(serializeJSON(message));
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

			ByteBuffer b = ByteBuffer.wrap((string + "\n").getBytes(StandardCharsets.UTF_8));
			if (b != null) {
				pendingData.offer(b);
			}

			// signal send interest
			if (selectionKey.isValid() && selectionKey.channel() instanceof SocketChannel) {
				if (Thread.currentThread() == selectorThread) {
					try {
						selectionKey.interestOpsOr(SelectionKey.OP_WRITE);
					} catch (CancelledKeyException ignored) {
					}
				} else {
					pendingWriteInterest.offer((SocketChannel) selectionKey.channel());
					if (selector != null) {
						if (wakeupPending.compareAndSet(false, true)) {
							selector.wakeup();
						}
					}
				}
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
			return LEGACY_UPGRADE_NOTICE;
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
			data.entrySet().stream().sorted((a, b) -> a.getKey().compareToIgnoreCase(b.getKey())).forEach(e -> {
				String key = e.getKey();
				Object value = e.getValue();
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
			});
			return sb.toString();
		}

		/**
		 * serialize data for JSON clients
		 * 
		 * @param message
		 * @return
		 */
		private String serializeJSON(Message message) {
			return message.getSocketJsonForm(() -> {
				ObjectNode je = JSON_OBJECT_MAPPER.valueToTree(message.data);

				// add OOCSI properties
				je.put("recipient", message.getRecipient());
				je.put("timestamp", message.getTimestamp().getTime());
				je.put("sender", message.getSender());

				// serialize
				try {
					return JSON_OBJECT_MAPPER.writeValueAsString(je);
				} catch (JsonProcessingException e) {
					// fall back to normal toString
					return je.toString();
				}
			});
		}
	}

}
