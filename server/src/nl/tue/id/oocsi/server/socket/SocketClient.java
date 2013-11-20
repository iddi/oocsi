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
		send("send " + message.recipient + " " + serialize(message.data) + " "
				+ message.timestamp.getTime() + " " + message.sender);
		OOCSIServer.log("sent message to " + socket.getInetAddress() + ":"
				+ socket.getPort());
	}

	/**
	 * internal send (string based)
	 * 
	 * @param outputLine
	 */
	private void send(String outputLine) {
		synchronized (output) {
			output.println(outputLine);
		}
	}

	private String serialize(Map<String, Object> data) {
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

	/**
	 * start the new client in a thread
	 */
	public void start() {
		new Thread(new Runnable() {
			private BufferedReader input;

			public void run() {
				try {
					output = new PrintWriter(socket.getOutputStream(), true);
					input = new BufferedReader(new InputStreamReader(socket
							.getInputStream()));

					String inputLine, outputLine;
					if ((inputLine = input.readLine()) != null) {
						token = inputLine;
						if (protocol.register(SocketClient.this)) {

							// say hi to new client
							synchronized (output) {
								output.println("welcome " + token);
							}

							while ((inputLine = input.readLine()) != null) {
								outputLine = protocol.processInput(
										SocketClient.this, inputLine);
								if (outputLine == null) {
									break;
								} else if (outputLine.length() > 0) {
									synchronized (output) {
										output.println(outputLine);
									}
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
				} finally {

					// close socket connection to client
					output.close();
					try {
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
