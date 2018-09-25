# OOCSI

The OOCSI mission is to create a simple systems-interaction fabric for use by designers.

Two basic components form an OOCSI network: the client and the server. While the server can be started from the command line (next item), the client interface needs to be embedded in another code to allow access to the OOCSI server for sending and receiving data.

## OOCSI Server

The server comes as a pre-compiled .jar file (~250kB) that runs out-of-the-box on Windows and Mac OS X (requires Java). You can find the newest version on the releases page: [OOCSI Server Releases ](https://github.com/iddi/oocsi/releases).

The OOCSI server can be run directly by double-clicking the .jar file on most operating systems. There are, however optional, command line parameters available:

### Port

Run the server on a specific port (instead of the default port 4444):

	java -jar OOCSI_server.jar -port 4545

### Logging

Switch on logging to the console:

	java -jar OOCSI_server.jar -logging

Switch on logging to a file "logfile.txt":

	java -jar OOCSI_server.jar -logging > logfile.txt

### Clients

Allow a maximum number of clients to be connected. In the following example it is restricted to 55 :

	java -jar OOCSI_server.jar -clients 55

It is possible to specify users who are protected by a password. User and password are separated by a double-colon and a semicolon separates different users. You can use alphanumerical characters, including: '-', '_', '.', '$', and '%' for a password of any length. 
An example with two users: Alice with password "Pa$$word%%" and Bob with password "bob-pas$worD":

	java -jar OOCSI_server.jar -users Alice:Pa$$word%%;Bob:bob-pas$worD

As expected, all parameters can be used at the same time, like this:

	java -jar OOCSI_server.jar -logging -clients 55 -port 4545 -users Alice:Pa$$word%%;Bob:bob-pas$worD


## OOCSI Client

OOCSI is, in essence, a client-server message-bus infrastructure. With the server running, a client can connect and exchange messages with other clients via the server. While there is a dedicated [Processing plug-in for OOCSI](https://iddi.github.io/oocsi-processing), a client interface for Java or JVM program is also provided (see next item below). 


### Embedding the client

The Java OOCSI client can be embedded in any Java program (or anything running on a JVM). The only dependency is the [oocsi.jar](https://github.com/iddi/oocsi-processing/blob/master/dist/oocsi/library/oocsi.jar) (20kB) library. Add the library's file location to the classpath and follow the next steps. 

### Connecting to the OOCSI network

First, create an OOCSI client by providing a unique name to the OOCSIClient constructor:

	OOCSIClient sender = new OOCSIClient("sender");
	
Connecting to an OOCSI server on the same machine and default port 4444.
	
	sender.connect("localhost", 4444);

Connecting to an OOCSI server on a different machine and custom port 4545.
	
	sender.connect("123.123.123.123", 4545);


### Sending messages

Use the OOCSIMessage class to create and send messages on the OOCSI network:

	new OOCSIMessage(sender, "mychannel").data("mykey", "myvalue").send();
	
In this example, a message is created and to be sent with the OOCSIClient "sender" and destined for the channel "mychannel". A key-value pair ("mykey" : "myvalue") is added to the message by calling the data() method and finally, the message is sent.


### Receiving messages and handling events

For receiving messages, another OOCSI client is initialized and started: 

	OOCSIClient recipient = new OOCSIClient("myrecipient");
	recipient.connect("localhost", 4444);
	
Again, make sure that the client name is unique and connecting to the correct server.

In the next example, the client "recipient" subscribes to the channel "mychannel" and provides an EventHandler instance. This newly instantiated object asynchronously receives all events sent in the respective channel and will call the internal method "receive". In this example, the event timestamp will be printed in the console window.

	recipient.subscribe("mychannel", new EventHandler() {
		public void receive(OOCSIEvent event) {
			System.out.println(event.getTimestamp());
		}
	});


### Getting data from events

An OOCSIEvent has built-in infrastructure-level data fields such as _sender_, _timestamp_, and _channel_. Also, the _recipient_ field is provided for some client implementations. Each of these fields can be accessed with dedicated getter methods:

	OOCSIEvent event = ...
	
	String sender = event.getSender();
	String channel = event.getChannel();
	String channel = event.getRecipient();
	
	Date timestamp = event.getTimestamp();
	long unixTime = event.getTime();
	
Apart from that, OOCSIEvents have a data payload that is freely definable and provided as a key-value store (Map<String, Object>). Such key-value pairs can be accessed with helper methods
that will convert the data type of the value accordingly: 
	 
	OOCSIEvent event = ...
	
	String stringValue = event.getString("mykey");
	Object objectValue = event.getObject("mykey");
	
Events do not guarantee that specific keys and values are included. For these cases, default values can be used in the retrieval of event data. These default values (with the correct data type) are added to the retrieval call as a second parameter, and they will be assigned if (1) the key could not be found, or if (2) the value could not converted to the specified data type.

	
	OOCSIEvent event = ...
	
	// retrieval with an additional default value
	String stringValue = event.getString("mykey", "default");
	long longValue = event.getLong("mykey", 0);
	int intValue = event.getInt("mykey", 0);
	boolean booleanValue = event.getInt("mykey", false);

As an alternative to using default values, one can also check whether the key is included in the event:

	OOCSIEvent event = ...
	
	if(event.has("mykey")) {
		// retrieve value
	}
	
Finally, events can provide a list of included keys, which can be used to dump all data or to retrieve all data systematically. 

	OOCSIEvent event = ...
	
	String[] keys = event.keys();
	



### Full example

The full example below registers two clients, one subscribes for messages in the channel "mychannel" and the other one will send a single message to this channel. The subscribing client receives the messages and prints out the value associated with the key "mykey" and the timestamp of the event in the console.


	OOCSIClient recipient = new OOCSIClient("myrecipient");
	
	recipient.connect("localhost", 4444);
	
	recipient.subscribe("mychannel", new EventHandler() {
		public void receive(OOCSIEvent event) {
			System.out.println(event.getString("mykey") + " >> " + event.getTimestamp());
		}
	});

	OOCSIClient sender = new OOCSIClient("sender");
	
	sender.connect("localhost", 4444);
	
	new OOCSIMessage(sender, "mychannel").data("mykey", "myvalue").send();
