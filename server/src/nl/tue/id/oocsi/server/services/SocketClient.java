package nl.tue.id.oocsi.server.services;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import nl.tue.id.oocsi.server.OOCSIServer;
import nl.tue.id.oocsi.server.model.Client;
import nl.tue.id.oocsi.server.protocol.Base64Coder;
import nl.tue.id.oocsi.server.protocol.Message;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * socket implementation for OOCSI client
 * 
 * @author matsfunk
 * 
 */
public class SocketClient extends Client {

	private static final Gson JSON_SERIALIZER = new Gson();

	private SocketService protocol;

	private Socket socket = null;
	private PrintWriter output;

	private ClientType type = ClientType.OOCSI;

	/**
	 * create a new client for the socket protocol
	 * 
	 * @param protocol
	 * @param socket
	 */
	public SocketClient(SocketService protocol, Socket socket) {
		super("");
		this.protocol = protocol;
		this.socket = socket;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.tue.id.oocsi.server.model.Client#send(nl.tue.id.oocsi.server.protocol .Message)
	 */
	@Override
	public void send(Message message) {
		if (type == ClientType.OOCSI) {
			send("send " + message.recipient + " " + serializeOOCSI(message.data) + " " + message.timestamp.getTime()
					+ " " + message.sender);

			// this is ok after serialization
			message.addData("method", "OOCSI");
		} else if (type == ClientType.PD) {
			send(message.recipient + " " + serializePD(message.data) + " " + "timestamp=" + message.timestamp.getTime()
					+ " sender=" + message.sender);

			// this is ok after serialization
			message.addData("method", "PD");
		} else if (type == ClientType.JSON) {
			send(serializeJSON(message.data, message.recipient, message.timestamp.getTime(), message.sender));

			// this is ok after serialization
			message.addData("method", "JSON");
		}
		OOCSIServer.logEvent(message.sender, message.recipient, message.data, message.timestamp);
	}

	/**
	 * internal send (string based)
	 * 
	 * @param outputLine
	 */
	private void send(String outputLine) {
		if (output != null) {
			synchronized (output) {
				if (type == ClientType.OOCSI) {
					output.println(outputLine);
				} else if (type == ClientType.PD) {
					output.println(outputLine + ";");
				} else if (type == ClientType.JSON) {
					output.println(outputLine);
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
	private String serializeOOCSI(Map<String, Object> data) {

		// replace JSON objects
		for (Entry<String, Object> e : data.entrySet()) {
			if (e.getValue() instanceof JsonElement) {
				JsonElement je = (JsonElement) e.getValue();
				if (je.isJsonPrimitive()) {
					if (je.getAsJsonPrimitive().isNumber()) {
						e.setValue(je.getAsJsonPrimitive().getAsNumber());
						continue;
					} else if (je.getAsJsonPrimitive().isString()) {
						e.setValue(je.getAsJsonPrimitive().getAsString());
						continue;
					} else if (je.getAsJsonPrimitive().isBoolean()) {
						e.setValue(je.getAsJsonPrimitive().getAsBoolean());
						continue;
					} else {
						e.setValue(new Gson().toJson(je));
					}
				}
			}
		}

		// map to serialized java object
		ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
		try {
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(data);
			byte[] rawData = baos.toByteArray();
			return new String(Base64Coder.encode(rawData));
		} catch (IOException e) {
			try {
				ObjectOutputStream oos = new ObjectOutputStream(baos);
				oos.writeObject(new HashMap<String, Object>());
				byte[] rawData = baos.toByteArray();
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

		// map to json
		JsonObject je = (JsonObject) JSON_SERIALIZER.toJsonTree(data);

		// add OOCSI properties
		je.addProperty("recipient", recipient);
		je.addProperty("timestamp", timestamp);
		je.addProperty("sender", sender);

		// serialize
		return je.toString();
	}

	/**
	 * send a ping message to client
	 * 
	 */
	public void ping() {
		send("ping");
	}

	/**
	 * start the new client in a thread
	 * 
	 */
	public void start() {
		new Thread(new Runnable() {
			private BufferedReader input;
			private Socket outputSocket;
			private OutputStream outputStream;

			public void run() {
				try {
					input = new BufferedReader(new InputStreamReader(socket.getInputStream()));

					String inputLine, outputLine;
					if ((inputLine = input.readLine()) != null) {

						// update last action
						lastAction = System.currentTimeMillis();

						// check for PD/raw-only socket client
						if (inputLine.contains(";")) {
							token = inputLine.replace(";", "");
							type = ClientType.PD;
							try {
								// try special return port 4445 (for Pd/MaxMSP)
								outputSocket = new Socket();
								outputSocket.connect(new InetSocketAddress(socket.getInetAddress(), 4445), 5000);
								outputStream = outputSocket.getOutputStream();
								output = new PrintWriter(outputStream, true);
							} catch (Exception e) {
								// if not responding in 5 seconds, use open connection for return
								output = new PrintWriter(socket.getOutputStream(), true);
							}
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
								send("{'message' : \"welcome " + token + "\"}");
							} else {
								send("welcome " + token);
							}

							// log connection creation
							OOCSIServer.logConnection(token, "OOCSI", "client connected", new Date());

							while ((inputLine = input.readLine()) != null) {

								// update last action
								lastAction = System.currentTimeMillis();

								// clean input from PD clients
								if (type == ClientType.PD) {
									inputLine = inputLine.replace(";", "");
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
								output.println("error (name already registered: " + token + ")");
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

		// close sockets and writers
		try {
			if (output != null) {
				output.close();
			}
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// log connection close
		OOCSIServer.logConnection(token, "OOCSI", "client disconnected", new Date());

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
}
