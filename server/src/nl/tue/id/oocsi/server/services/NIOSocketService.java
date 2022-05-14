package nl.tue.id.oocsi.server.services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import nl.tue.id.oocsi.server.OOCSIServer;
import nl.tue.id.oocsi.server.model.Client;
import nl.tue.id.oocsi.server.model.Server;
import nl.tue.id.oocsi.server.protocol.Base64Coder;
import nl.tue.id.oocsi.server.protocol.Message;
import nl.tue.id.oocsi.server.protocol.StreamMessage;

public class NIOSocketService extends AbstractService {

	private final int port;
	private final String[] registeredUsers;

	private final Map<SocketChannel, NIOSocketClient> nioClients = new HashMap<>();
	private boolean serverSocketActive = true;

	/**
	 * create a TCP socket service for OOCSI
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
		ServerSocketChannel serverSocketChannel;
		try {
			serverSocketChannel = ServerSocketChannel.open();
			serverSocketChannel.configureBlocking(false);
			serverSocketChannel.socket().bind(new InetSocketAddress(port));

			Selector selector = Selector.open();
			serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

			while (serverSocketActive) {
				selector.select();
				Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

				while (keys.hasNext()) {
					SelectionKey selectionKey = (SelectionKey) keys.next();
					if (selectionKey.isAcceptable()) {
						SocketChannel socketChannel = serverSocketChannel.accept();
						socketChannel.configureBlocking(false);
						socketChannel.register(selectionKey.selector(), SelectionKey.OP_READ | SelectionKey.OP_WRITE);
					} else if (selectionKey.isReadable()) {
						handleReadOp(selectionKey);
					} else if (selectionKey.isWritable()) {
						handleWriteOp(selectionKey);
					}
					keys.remove();
				}

				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
				}
			}
		} catch (IOException e) {
			// broken
			e.printStackTrace();
		}
	}

	private void handleReadOp(SelectionKey selectionKey) {
		SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
		ByteBuffer byteBuffer = ByteBuffer.allocate(128 * 1024);
		try {
			int read = socketChannel.read(byteBuffer);
			if (read == -1) {
				// if connection is closed by the client
				if (nioClients.containsKey(socketChannel)) {
					// remove the client first
					NIOSocketClient client = nioClients.get(socketChannel);
					server.removeClient(client);
					nioClients.remove(socketChannel);
				}
				// then close channel
				socketChannel.close();
				return;
			}
		} catch (Exception e) {
			// connection reset
			if (nioClients.containsKey(socketChannel)) {
				// remove the client first
				NIOSocketClient client = nioClients.get(socketChannel);
				server.removeClient(client);
				nioClients.remove(socketChannel);
			}
		}

		NIOSocketClient client = nioClients.get(socketChannel);
		if (client == null) {
			// do the client init based on read
			String inputLine = new String(byteBuffer.array()).trim();

			// parse input line
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
			// send data to client
			client.send(byteBuffer);
		}

	}

	private void handleWriteOp(SelectionKey selectionKey) {
		SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
		NIOSocketClient client = nioClients.get(socketChannel);
		try {
			if (client != null) {
				Queue<ByteBuffer> pendingData = client.pendingData;
				while (!pendingData.isEmpty()) {
					ByteBuffer buf = pendingData.poll();
					if (buf != null) {
						socketChannel.write(buf);
					}
				}

				// check if we need to terminate this client
				if (!client.isConnected()) {
					socketChannel.write(ByteBuffer.wrap("bye\n".getBytes()));
					nioClients.remove(socketChannel);
					socketChannel.close();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void stop() {
		serverSocketActive = false;
	}

	class NIOSocketClient extends Client {
		private final ObjectMapper JSON_OBJECT_MAPPER = new ObjectMapper();

		ClientType type = ClientType.OOCSI;
		LinkedList<ByteBuffer> pendingData = new LinkedList<ByteBuffer>();

		private boolean isConnected = true;

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

		public void send(ByteBuffer input) {

			// update last action
			touch();

			String comps[] = new String(input.array()).trim().split("\n");
			for (String message : comps) {
				// process input and write output if necessary
				String outputLine = processInput(this, type == ClientType.PD ? message.replace(";", "") : message);
				if (outputLine == null) {
					server.removeClient(this);
				} else if (outputLine.length() > 0) {
					send(outputLine);
				}
			}
		}

		@Override
		public void send(Message message) {

			// update last action
			touch();

			if (type == ClientType.OOCSI) {
				// OOCSI native clients might support streaming
				if (message instanceof StreamMessage) {
					StreamMessage smessage = (StreamMessage) message;
					send("stream " + smessage.recipient + " " + new String(smessage.getData()));
				} else {
					send("send " + message.recipient + " " + serializeJava(message.data) + " "
					        + message.timestamp.getTime() + " " + message.sender);
				}
			} else if (type == ClientType.PD) {
				send(message.recipient + " " + serializePD(message.data) + " " + "timestamp="
				        + message.timestamp.getTime() + " sender=" + message.sender);
			} else if (type == ClientType.JSON) {
				send(serializeJSON(message.data, message.recipient, message.timestamp.getTime(), message.sender));
			}

			// log this if recipient is this client exactly
			if (message.recipient.equals(getName())) {
				OOCSIServer.logEvent(message.sender, "", message.recipient, message.data, message.timestamp);
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

		private void send(String string) {
			if (type == ClientType.PD) {
				string += ';';
			}

			ByteBuffer b = ByteBuffer.wrap((string + "\n").getBytes(Charset.defaultCharset()));
			pendingData.add(b);
		}

		/**
		 * serialize data for OOCSI clients
		 * 
		 * @param data
		 * @return
		 */
		private String serializeJava(Map<String, Object> data) {
			Map<String, Object> oocsiData = new HashMap<String, Object>();
			oocsiData.put("error", "Your OOCSI client version is too old, please update.");
			return serializeOOCSIOutput(oocsiData);
		}

		/**
		 * @param data
		 * @return
		 */
		private String serializeOOCSIOutput(Map<String, Object> data) {
			// map to serialized java object
			final ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
			try {
				final ObjectOutputStream oos = new ObjectOutputStream(baos);
				oos.writeObject(data);
				final byte[] rawData = baos.toByteArray();
				return new String(Base64Coder.encode(rawData));
			} catch (IOException e) {
				try {
					final ObjectOutputStream oos = new ObjectOutputStream(baos);
					oos.writeObject(new HashMap<String, Object>());
					final byte[] rawData = baos.toByteArray();
					return new String(Base64Coder.encode(rawData));
				} catch (IOException e1) {
					return "";
				}
			}
		}

		/**
		 * serialize data for PD clients
		 * 
		 * @param data
		 * @return
		 */
		private String serializePD(Map<String, Object> data) {
			// map to blank separated list
			StringBuilder sb = new StringBuilder();
			for (String key : data.keySet()) {
				sb.append(key + "=" + data.get(key) + " ");
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
