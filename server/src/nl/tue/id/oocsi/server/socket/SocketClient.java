package nl.tue.id.oocsi.server.socket;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;

import nl.tue.id.oocsi.server.OOCSIServer;
import nl.tue.id.oocsi.server.model.Client;
import nl.tue.id.oocsi.server.protocol.Message;
import nl.tue.id.oocsi.server.protocol.Protocol;

/**
 * socket implementation for OOCSI client
 * 
 * @author mfunk
 * 
 */
public class SocketClient extends Client {

	private Protocol protocol;

	private Socket socket = null;
	private PrintWriter output;

	private ClientType type = ClientType.OOCSI;

	/**
	 * create a new client for the socket protocol
	 * 
	 * @param protocol
	 * @param socket
	 */
	public SocketClient(Protocol protocol, Socket socket) {
		super("");
		this.protocol = protocol;
		this.socket = socket;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * nl.tue.id.oocsi.server.model.Client#send(nl.tue.id.oocsi.server.protocol
	 * .Message)
	 */
	@Override
	public void send(Message message) {
		if (type == ClientType.OOCSI) {
			send("send " + message.recipient + " "
					+ serializeOOCSI(message.data) + " "
					+ message.timestamp.getTime() + " " + message.sender);
			OOCSIServer.log("sent message to " + socket.getInetAddress() + ":"
					+ socket.getPort());
		} else if (type == ClientType.PD) {
			send(message.recipient + " " + serializePD(message.data) + " "
					+ "timestamp=" + message.timestamp.getTime() + " sender=" + message.sender);
			OOCSIServer.log("sent message to " + socket.getInetAddress()
					+ ":4445");
		}
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
				}
			}
		}
	}

	private String serializeOOCSI(Map<String, Object> data) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
		try {
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(data);
			byte[] rawData = baos.toByteArray();
			return new String(Base64Coder.encode(rawData));
		} catch (IOException e) {
			return "";
		}
	}

	private String serializePD(Map<String, Object> data) {
		StringBuilder sb = new StringBuilder();
		for (String key : data.keySet()) {
			sb.append(key + "=" + data.get(key) + " ");
		}
		return sb.toString();
	}

	/**
	 * start the new client in a thread
	 */
	public void start() {
		new Thread(new Runnable() {
			private BufferedReader input;

			public void run() {
				try {
					input = new BufferedReader(new InputStreamReader(socket
							.getInputStream()));

					String inputLine, outputLine;
					if ((inputLine = input.readLine()) != null) {

						// check for PD/raw-only socket client
						if (inputLine.contains(";")) {
							token = inputLine.replace(";", "");
							type = ClientType.PD;
							output = new PrintWriter(new Socket(socket
									.getInetAddress(), 4445).getOutputStream(),
									true);
						} else {
							token = inputLine;
							type = ClientType.OOCSI;
							output = new PrintWriter(socket.getOutputStream(),
									true);
						}

						if (protocol.register(SocketClient.this)) {

							// say hi to new client
							send("welcome " + token);

							while ((inputLine = input.readLine()) != null) {

								// clean input
								if (type == ClientType.OOCSI) {
									// inputLine = inputLine;
								} else {
									inputLine = inputLine.replace(";", "");
								}

								// process input and write output if necessary
								outputLine = protocol.processInput(
										SocketClient.this, inputLine);
								if (outputLine == null) {
									break;
								} else if (outputLine.length() > 0) {
									send(outputLine);
								}
							}
						} else {
							// say goodbye to new client
							synchronized (output) {
								output.println("error (name already registered: "
										+ token + ")");
							}
						}
					}

				} catch (IOException e) {
					// this is kinda normal behavior when a client quits
					// e.printStackTrace();
				} catch (IllegalArgumentException e) {
					// someone used probably send instead of sendraw with some
					// non-encoded data
					// e.printStackTrace();
				} finally {
					// close socket connection to client
					try {
						output.close();
						input.close();
						socket.close();
					} catch (IOException e) {
						e.printStackTrace();
					}

					// remove this client
					protocol.unregister(SocketClient.this);
				}
			}
		}).start();
	}
}
