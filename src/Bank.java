/**
 * 
 * @author Sam Damen (42636678)
 * Bank
 * COMS3200
 * Assignment 1
 * 
 *  Register Message format = "regi,name,ip,port\n"
 *  
 *  
 * Even item id             = "1"
 * Odd item id              = "0"
 * 
 * Also print to stdout "item_id" + "OK" or "NOT OK"
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


public class Bank {
	
	//Server variables
	private static Selector selector;
	private static ServerSocketChannel serverSocketChannel = null;
	private static ServerSocket serverSocket = null;
	
	
	public static void main(String[] args) {
		
		//0 = bank, 1 = name server
		int[] ports = commandParse(args);
			
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
			serverSocket.bind(new InetSocketAddress(ports[0]));
			// registers this channel with the given selector, returning a selection key
			serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
			
			//Successfully can listen on port
			System.err.print("Bank waiting for incoming connections\n");
			
			//Try to register with nameserver
			registrationClient(ports);

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
						
							// react to Client's message
						if (readBytes > 0) {
							
							//Check for odd/even item IDs
							String id = message.substring(0, 10); //String index out of range exception
							try {	
								long num = Long.parseLong(id); //Need to account for 32bit limit
								
								if ( (num & 1) == 0 ) {
									reply = "1\n";
									System.out.format("%d OK\n", num);
								} else {
									reply = "0\n";
									System.out.format("%d NOT OK\n", num);
								}
							} catch (NumberFormatException e) {
								reply = "0\n";
								System.out.println("Wrong Format for id");
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
			System.err.format("Bank unable to listen on given port %d\n", ports[0]);
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
//					        Extra Methods
//
//*******************************************************************
	
	//Perform command line parsing
	private static int[] commandParse(String[] args) {
		
		int[] ports = new int[2];
		
		if(args.length == 2) {
			try {
				ports[0] = Integer.parseInt(args[0]);
				ports[1] = Integer.parseInt(args[1]);
			} catch (NumberFormatException e) {
				System.err.print("Invalid command line arguments for Bank\n");
				System.exit(1);
			}			
		} else {
			System.err.print("Invalid command line arguments for Bank\n");
			System.exit(1);
		}
		return ports;
	}
	
	
	//Client Class for Name Server Registration
	static void registrationClient(int[] ports) {
		SocketChannel channel = null;
		try {
			// open socket channel
			channel = SocketChannel.open();
			// set Blocking mode to non-blocking
			channel.configureBlocking(false);
			// set Server info
			InetSocketAddress target = new InetSocketAddress("127.0.0.1", ports[1]);
			// open selector
			Selector selector = Selector.open();
			// connect to Server
			channel.connect(target);
			// registers this channel with the given selector, returning a selection key
			channel.register(selector, SelectionKey.OP_CONNECT);

			while (selector.select() > 0) {
				for (SelectionKey key : selector.selectedKeys()) {
					// test connectivity
					if (key.isConnectable()) {
						SocketChannel sc = (SocketChannel) key.channel();
						// set register status to WRITE
						sc.register(selector, SelectionKey.OP_WRITE);
						sc.finishConnect();
					}
					// test whether this key's channel is ready for reading from Server
					else if (key.isReadable()) {
						// allocate a byte buffer with size 1024
						ByteBuffer buffer = ByteBuffer.allocate(1024);
						SocketChannel sc = (SocketChannel) key.channel();
						int readBytes = 0;
						// try to read bytes from the channel into the buffer
						try {
							int ret = 0;
							try {
								while ((ret = sc.read(buffer)) > 0)
									readBytes += ret;
							} finally {
								buffer.flip();
							}
							// finished reading, check response
							if (readBytes > 0) {
								
								//Check for correct registration to the Name Server
								if ( (Charset.forName("UTF-8").decode(buffer).toString() ).equals("ACK\n") ) {
									//Success, close connection as client
									buffer = null;
									channel.close();
									selector.close();
									break;
								} else {
									System.err.println("Bank registration to NameServer failed\n");
									System.exit(1);
								}
							}
						} finally {
							if (buffer != null)
								buffer.clear();
						}
						// set register status to WRITE
						sc.register(selector, SelectionKey.OP_WRITE);
					}
					// test whether this key's channel is ready for writing to Server
					else if (key.isWritable()) {
						SocketChannel sc = (SocketChannel) key.channel();
						// send to Server
						channel.write(Charset.forName("UTF-8").encode("regi,Bank,127.0.0.1," + ports[0]));
						// set register status to READ
						sc.register(selector, SelectionKey.OP_READ);
					}
				}
				if (selector.isOpen()) {
					selector.selectedKeys().clear();
				} else {
					break;
				}
			}
		} catch (IOException e) {
			System.err.print("Bank registration to NameServer failed\n");
		} finally {
			if (channel != null) {
				try {
					channel.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}	
	
}






