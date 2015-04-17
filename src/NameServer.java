/**
 * 
 * @author Sam Damen (42636678)
 * NameServer
 * COMS3200
 * Assignment 1
 * 
 *  Register Message format = "regi,name,ip,port\n"
 *  Lookup Message format   = "look,name\n"
 *  
 *  Need to add in footer to messages for non-blocking functionality??
 *  
 *  Successful registration = "ACK"
 *  Unsuccessful registration = "NACK"
 * 
 */

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;


public class NameServer {
	
	//Store of Names and IP/Port of the registered servers
	private static HashMap<String, ArrayList<String>> serverTable = new HashMap<String, ArrayList<String>>();
	
	//Server variables
	private static Selector selector;
	private static ServerSocketChannel serverSocketChannel = null;
	private static ServerSocket serverSocket = null;
	

	public static void main(String[] args) throws IOException {
		
		int port = commandParse(args);
		
		try {
			// open selector
			selector = Selector.open();
			// open socket channel
			serverSocketChannel = ServerSocketChannel.open();
			// set the socket associated with this channel
			serverSocket = serverSocketChannel.socket();
			// set Blocking mode to non-blocking
			serverSocketChannel.configureBlocking(false);
			// bind port
			serverSocket.bind(new InetSocketAddress(port));
			// registers this channel with the given selector, returning a selection key
			serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
			
			//Successfully can listen on port
			System.err.print("Name Server waiting for incoming connections ...\n");

			while (selector.select() > 0) {
				for (SelectionKey key : selector.selectedKeys()) {
					// test whether this key's channel is ready to accept a new socket connection
					if (key.isAcceptable()) {
						// accept the connection
						ServerSocketChannel server = (ServerSocketChannel) key.channel();
						SocketChannel sc = server.accept();
						if (sc == null)
							continue;
						System.out.println("Connection accepted from: " + sc.getRemoteAddress());
						// set blocking mode of the channel
						sc.configureBlocking(false);
						// allocate buffer
						ByteBuffer buffer = ByteBuffer.allocate(1024);
						// set register status to READ
						sc.register(selector, SelectionKey.OP_READ, buffer);
					}
					// test whether this key's channel is ready for reading from Client
					else if (key.isReadable()) {
						// get allocated buffer with size 1024
						ByteBuffer buffer = (ByteBuffer) key.attachment();
						SocketChannel sc = (SocketChannel) key.channel();
						int readBytes = 0;
						String message = null;
						//Used to store the reply for sending to the client
						String reply = null;
						// try to read bytes from the channel into the buffer
						try {
							int ret;
							try {
								while ((ret = sc.read(buffer)) > 0)
									readBytes += ret;
							} catch (Exception e) {
								readBytes = 0;
							} finally {
								buffer.flip();
							}
							// finished reading, form message
							if (readBytes > 0) {
								message = Charset.forName("UTF-8").decode(buffer).toString();
								buffer = null;
							}
						} finally {
							if (buffer != null)
								buffer.clear();
						}
						// react by Client's message
						if (readBytes > 0) {
							System.out.println("Message from Client" + sc.getRemoteAddress() + ": " + message);
							
							//Parse message
							try {
								switch (message.substring(0, 4)) {
								
								case "regi":
									//Register Request
									if( regQuery(message.trim()) ) {
										//send confirm to client
										reply = "ACK\n";
									} else {
										reply = "NACK\n";
										//Close the connection
										sc.close();
									}
									break;
									
								case "look":
									//Lookup Request
									if ( lookQuery(message.trim()) ) {
										String name = message.substring(5, message.length() - 1); //line.length includes \n
										ArrayList<String> list = serverTable.get(name); 
										//Return the requested name and information to client
										reply = name + "," + list.get(0) + "," + list.get(1) + "\n";
				
									} else {
										reply = "Error: Process has not registered with the Name Server\n";
										//TODO also need to close connection if receive look,rubbish ?
									}
									break;
									
								default:
									//Rubbish
									reply = "NACK\n";
									sc.close();
									break;
								}				
								
							} catch (StringIndexOutOfBoundsException e) {
								reply = "NACK\n";
								sc.close();
								break; //not sure??
							}
							
							// set register status to WRITE and send reply
							sc.register(key.selector(), SelectionKey.OP_WRITE, reply);							
						}
					}
					// test whether this key's channel is ready for sending to Client
					else if (key.isWritable()) {
						SocketChannel sc = (SocketChannel) key.channel();
						ByteBuffer buffer = ByteBuffer.allocate(1024);
						buffer.put(((String) key.attachment()).getBytes());
						buffer.flip();
						sc.write(buffer);
						// set register status to READ
						sc.register(key.selector(), SelectionKey.OP_READ, buffer);
					}
				}
				if (selector.isOpen()) {
					selector.selectedKeys().clear();
				} else {
					break;
				}
			}
		} catch (IOException e) {
			System.err.format("Cannot listen on given port number %d\n", port);
			System.exit(1);
		} finally {
			if (serverSocketChannel != null) {
				try {
					serverSocketChannel.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}	
	
	//*******************************************************************
	//
	//				           Extra Methods
	//
	//*******************************************************************
	
	//Check a message for correct format to register
	private static boolean regQuery(String message) {
		String[] data = message.split(",");
		
		//TODO Check if valid Name,IP,Port, correct number of items (3)
		
		ArrayList<String> list = new ArrayList<String>();
		list.add(data[2]);
		list.add(data[3]);
		serverTable.put(data[1], list);		
		
		return true;
	}
	
	//Check if a message is in the Table
	private static boolean lookQuery(String message) {
		String[] data = message.split(",");
		
		//TODO Check if valid name?		
		return serverTable.containsKey(data[1]);	
	}
		
	//Perform command line parsing
	private static int commandParse(String[] args) {
		
		int port = 0;
		
		if(args.length == 1) {
			try {
				port = Integer.parseInt(args[0]);
			} catch (NumberFormatException e) {
				System.err.print("Invalid command line arguments for NameServer\n");
				System.exit(1);
			}			
		} else {
			System.err.print("Invalid command line arguments for NameServer\n");
			System.exit(1);
		}
		return port;
	}
	
	/*	
	//Check if valid IP Address
	private static boolean validIP(String ip) {
	    if (ip == null || ip.isEmpty()) return false;
	    ip = ip.trim();
	    if ((ip.length() < 6) & (ip.length() > 15)) return false;

	    try {
	        Pattern pattern = Pattern.compile("^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");
	        Matcher matcher = pattern.matcher(ip);
	        return matcher.matches();
	    } catch (PatternSyntaxException ex) {
	        return false;
	    }
	}
*/

}
