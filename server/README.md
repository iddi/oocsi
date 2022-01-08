# OOCSI server

The OOCSI server is the hub in an OOCSI network. It is a central component that receives and dispatches messages. The server component is a Java application that can be configured for different use-cases. This page is about the mini OOCSI server. For other variants of the OOCSI server, check out the [OOCSI server](https://github.com/iddi/oocsi/wiki/OOCSI-server) page on the wiki.

The mini server is a single `.jar` file that can be run on most operating systems as long as there is a Java Runtime Environment (JRE) installed. The instructions for installing Java on Windows, macOS or Linux differ, but an OpenJDK installer from [Adoptium](https://adoptium.net/) should work in most cases.


### Download

Once Java is installed on your system, you can download and run the OOCSI server. The server comes as a pre-compiled .jar file: [OOCSI_server.jar](https://github.com/iddi/oocsi/releases/download/server_version_1.7/OOCSI_server.jar) (2 MB).


### Running / Configuration

It can be run simply by double-clicking on the JAR file in most operating systems. There are, however, command line parameters that are explained in the following:   

Switch on logging to a file with:

	java -jar OOCSI_server.jar -logging

Allow for a certain maximum number of clients to be connected (here, 55):

	java -jar OOCSI_server.jar -clients 55

Run the server on a specific port (4545 instead of 4444 which is the default):

	java -jar OOCSI_server.jar -port 4545

Of course, all parameters can be used at the same time, like this:

	java -jar OOCSI_server.jar -logging -clients 55 -port 4545 

Since the OOCSI server is a console application it does not have an application window or tray presence that you could use to stop it. If you need to stop the server, use the Task Manager on Windows, Activity Monitor on macOS, or similar on Linux. If you start the server on the command line, a simple CTRL-C should stop it, too.
