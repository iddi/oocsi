# oocsi

The OOCSI mission is to create a simple systems-interaction fabric for use by designers.

There are two basic components that form an OOCSI network: the client and the server. While the server can be started from the command line (see here), the client interface need to be embedded in other code and allows this code to access the OOCSI network, to send and receive data in a simple way.

## Server

The server comes as a pre-compiled .jar file: [OOCSI_server.jar](https://github.com/iddi/oocsi/releases/download/server_version_1.4/OOCSI_server.jar) (~250kB)

It can be run simply by double-clicking on the JAR file in most operating systems. There are, however, command line parameters that are explained in the following:   

Switch on logging to a file with:

	java -jar OOCSI_server.jar -logging

Allow for a certain maximum number of clients to be connected (here, 55):

	java -jar OOCSI_server.jar -clients 55

Run the server on a specific port (instead of 4444 which is the default):

	java -jar OOCSI_server.jar -port 4545

Of course, all parameters can be used at the same time, like this:

	java -jar OOCSI_server.jar -logging -clients 55 -port 4545 


## Client

OOCSI is a in essence a client-server message-bus infrastructure. With the server running, a client can connect an exchange
messages with other clients via the server. While there is a dedicated [Processing plug-in for OOCSI] (https://iddi.github.io/oocsi-processing), 
a client interface for direct use in Java or JVM program is provided as well (see below). 


### Embedding the client

The Java OOCSI client can be embedded in any kind of Java program (or anything running on a JVM). The only dependency is the 
[oocsi.jar](https://github.com/iddi/oocsi-processing/blob/master/dist/oocsi/library/oocsi.jar) (20kB) library. Put this library
into the classpath and follow the steps below. 

### Connecting to the OOCSI network

As the first step, create an OOCSI client by providing a unique name to the OOCSIClient constructor:

	OOCSIClient sender = new OOCSIClient("sender");
	
Connect to a running OOCSI server on the same machine and the default port 4444.
	
	sender.connect("localhost", 4444);

Connect to a running OOCSI server on a different machine and a custom port 4545.
	
	sender.connect("123.123.123.123", 4545);


### Sending messages

Use the OOCSIMessage class to create and send messages on the OOCSI network:  

	new OOCSIMessage(sender, "mychannel").data("mykey", "myvalue").send();
	
In this example a message is created to be sent with the OOCSIClient "sender" and destined for the channel "mychannel".
A key-value pair of data ("mykey" : "myvalue") is added to the message by calling the data() method
and finally, the message is sent to the OOCSI network.


### Receiving messages and handling events

For receiving messages, another OOCSI client is initialized and started: 

	OOCSIClient recipient = new OOCSIClient("myrecipient");
	recipient.connect("localhost", 4444);
	
Again, take care that the handle is unique and that the client connects to the right server.

This new client, recipient, subscribes for the channel "mychannel" and provides an EventHandler instance. This newly 
instantiated object asynchronously receives all events sent on the respective channel and will call the internal
method "receive". In this example, the event's timestamp will be printed in the console output.

	recipient.subscribe("mychannel", new EventHandler() {
		public void receive(OOCSIEvent event) {
			System.out.println(event.getTimestamp());
		}
	});


### Getting data from events

An OOCSIEvent has built-in infrastructure-level data fields such as _sender_, _timestamp_, and _channel_. In addition, the _recipient_ field is provided for some client implementations.
Each of these fields can be access with a dedicated getter method:

	OOCSIEvent event = ...
	
	// sender and receiver
	String sender = event.getSender();
	String channel = event.getChannel();
	String channel = event.getRecipient();
	
	// time
	Date timestamp = event.getTimestamp();
	long unixTime = event.getTime();
	
Apart from that, OOCSIEvents have a data payload that is freely definable and realized as a key-value store (Map<String, Object>). Such key-value pairs can be accessed with helper mthods
that will convert the data type of hte value accordingly: 
	 
	OOCSIEvent event = ...
	String stringValue = event.getString("mykey");
	Object objectValue = event.getObject("mykey");
	
Events do not guarantee that specific keys and values are contained. For these cases, default values can be used in the retrieval of event data. These default values (with the correct data type) are 
added to the retrieval call as a second parameter, and they will be assigned if (1) the key could not be found, or (2) if the value could not converted to the specified data type.  	

	// retrieval with an additional default value
	OOCSIEvent event = ...
	String stringValue = event.getString("mykey", "default");
	long longValue = event.getLong("mykey", 0);
	int intValue = event.getInt("mykey", 0);
	boolean booleanValue = event.getInt("mykey", false);

As an alternative to using default values, one can also check whether the key is contained in the event:

	OOCSIEvent event = ...
	if(event.has("mykey")) {
		// retrieve value
	}
	
Finally, events can provide a list of contained keys, which can be used to dump all contained data or to systematically retrieve all data.

	OOCSIEvent event = ...
	String[] keys = event.keys();
	



### Full example

The full example given below registers two clients, of which one subscribes for message on the channel "mychannel" and the
other will send a single message on this channel. The subscribing client receives the message and prints out the value
associated with the key "mykey" and the timestamp of the event on the console.


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
