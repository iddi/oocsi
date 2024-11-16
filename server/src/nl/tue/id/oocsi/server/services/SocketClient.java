package nl.tue.id.oocsi.server.services;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import nl.tue.id.oocsi.server.OOCSIServer;
import nl.tue.id.oocsi.server.model.Client;
import nl.tue.id.oocsi.server.protocol.Message;

/**
 * socket implementation for OOCSI client
 * 
 * @author matsfunk
 * 
 */
public class SocketClient extends Client {

	private static final ObjectMapper JSON_OBJECT_MAPPER = new ObjectMapper();;
	private final SocketService protocol;
	private final Socket socket;

	private ClientType type = ClientType.OOCSI;
	private PrintWriter output;
	private Semaphore pingQueue = new Semaphore(10);

	/**
	 * create a new client for the socket protocol
	 * 
	 * @param protocol
	 * @param socket
	 */
	public SocketClient(SocketService protocol, Socket socket) {
		super("", protocol.presence);
		this.protocol = protocol;
		this.socket = socket;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.tue.id.oocsi.server.model.Channel#accept(java.lang.String)
	 */
	@Override
	public boolean accept(String channelToken) {
		return true;
	};

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.tue.id.oocsi.server.model.Client#send(nl.tue.id.oocsi.server.protocol .Message)
	 */
	@Override
	public synchronized void send(Message message) {
		if (type == ClientType.OOCSI) {
			send("send " + message.getRecipient() + " " + serializeJava(message.data) + " "
			        + message.getTimestamp().getTime() + " " + message.getSender());
		} else if (type == ClientType.PD) {
			send(message.getRecipient() + " timestamp=" + message.getTimestamp().getTime() + " sender="
			        + message.getSender() + " " + serializePD(message.data));
		} else if (type == ClientType.JSON) {
			send(serializeJSON(message.data, message.getRecipient(), message.getTimestamp().getTime(),
			        message.getSender()));
		}

		// log this if recipient is this client exactly
		if (message.getRecipient().equals(getName())) {
			OOCSIServer.logEvent(message.getSender(), "", message.getRecipient(), message.data, message.getTimestamp());
		}
	}

	/**
	 * internal send (string based)
	 * 
	 * @param outputLine
	 */
	private synchronized void send(String outputLine) {
		if (output != null) {
			synchronized (output) {
				if (output != null) {
					if (type == ClientType.OOCSI) {
						output.println(outputLine);
					} else if (type == ClientType.PD) {
						output.println(outputLine + ";");
					} else if (type == ClientType.JSON) {
						output.println(outputLine);
					}
					output.flush();
				}
			}
		}
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
	 * serialize data for PD clients
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

	/**
	 * send a ping message to client
	 * 
	 */
	public void ping() {
		// ensure that we don't have too many permits in queue
		while (pingQueue.availablePermits() > 10) {
			pingQueue.acquireUninterruptibly();
		}

		// don't send a ping if this might block the output stream
		if (pingQueue.tryAcquire()) {
			send("ping");
		} else {
			OOCSIServer.log("Ping failed for " + getName());
		}
	}

	/**
	 * send a ping message to client
	 * 
	 */
	public void pong() {
		pingQueue.release();
	}

	/**
	 * start the new client in a thread
	 * 
	 */
	public void start() {
		new Thread(new Runnable() {
			private BufferedReader input;
			private OutputStream outputStream;

			public void run() {
				try {
					input = new BufferedReader(new InputStreamReader(socket.getInputStream()));

					String inputLine, outputLine;
					if ((inputLine = input.readLine()) != null) {

						// update last action
						touch();

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

						// check for PD/raw-only socket client
						if (inputLine.contains(";")) {
							token = inputLine.replace(";", "");
							type = ClientType.PD;
							output = new PrintWriter(socket.getOutputStream(), true);
						} else if (inputLine.contains("(JSON)")) {
							token = inputLine.replace("(JSON)", "").trim();
							type = ClientType.JSON;
							output = new PrintWriter(socket.getOutputStream(), true);
						} else {
							token = inputLine;
							type = ClientType.OOCSI;
							output = new PrintWriter(socket.getOutputStream(), true);
						}

						if (protocol.register(SocketClient.this)) {

							// say hi to new client
							if (type == ClientType.JSON) {
								send("{'message' : \"welcome " + getName() + "\"}");
							} else {
								send("welcome " + getName());
							}

							// log connection creation
							if (!isPrivate()) {
								OOCSIServer.logConnection(getName(), "OOCSI", "client connected", new Date());
							}

							while ((inputLine = input.readLine()) != null) {

								// update last action
								touch();

								// clean input from PD clients
								if (type == ClientType.PD) {
									inputLine = inputLine.replace("'", ",").replace(";", "");
								}

								// process input and write output if necessary
								outputLine = protocol.processInput(SocketClient.this, inputLine);
								if (outputLine == null) {
									break;
								} else if (outputLine.length() > 0) {
									send(outputLine);
								}
							}
						} else {
							// say goodbye to new client
							synchronized (output) {
								if (SocketClient.this.isPrivate()) {
									output.println("error (password wrong for name: " + getName() + ")");
								} else if (getName().contains(" ")) {
									output.println("error (name cannot contain spaces: " + getName() + ")");
								} else {
									output.println("error (name already registered: " + getName() + ")");
								}
							}
						}
					}

				} catch (IOException e) {
					// this is kinda normal behavior when a client quits
					// e.printStackTrace();
				} catch (Exception e) {
					// real problem
					e.printStackTrace();
				} finally {

					// first close input
					try {
						input.close();
					} catch (IOException e) {
						e.printStackTrace();
					}

					try {
						// first close print writer
						if (output != null) {
							output.flush();
							output.close();
						}

						// close stream and socket
						if (outputStream != null) {
							outputStream.flush();
							outputStream.close();
						}
					} catch (IOException e) {
						e.printStackTrace();
					}

					// close socket connection to client
					SocketClient.this.disconnect();
				}
			}
		}).start();
	}

	/**
	 * disconnect this client from the server
	 * 
	 */
	public void disconnect() {

		// check whether we are done already
		if (output == null) {
			return;
		}

		// close sockets and writers
		try {
			if (output != null) {
				output.close();
			}

			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			output = null;
		}

		// log connection close
		if (!isPrivate()) {
			OOCSIServer.logConnection(getName(), "OOCSI", "client disconnected", new Date());
		}

		// remove this client
		protocol.unregister(SocketClient.this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.tue.id.oocsi.server.model.Client#isConnected()
	 */
	public boolean isConnected() {
		return socket.isConnected() && !socket.isClosed();
	}

	/**
	 * return the IP address of connected client
	 * 
	 * @return
	 */
	public String getIPAddress() {
		return socket != null && socket.isBound() ? socket.getInetAddress().getHostAddress() : null;
	}
}
