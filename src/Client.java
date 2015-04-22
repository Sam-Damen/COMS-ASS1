import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * 
 * @author Sam Damen (42636678)
 * Client
 * COMS3200
 * Assignment 1
 * 
 *  Register Message format = "regi,name,ip,port\n"
 *  
 *  Store requests: 
 *  		"0"    = List items of stick file
 *  		"1-10" = Purchase item 
 *  
 *  Store replies:
 *  		list     = "1. item_id item_price"
 *  		purchase = "item_id $price CONTENT item_content"
 *  		error    = "item_id transaction aborted"
 * 
 */


public class Client {
	
	public static void main(String[] args) throws IOException {

		int[] commands = commandParse(args);
		String[] store = null;
		
		Socket clientSocket = null;
		PrintWriter out = null;
		BufferedReader in = null;
		
		//Try to get Store information
		try {
			store = lookupServer(commands[1]);
		} catch (IOException e) {
			System.err.print("Client unable to connect with NameServer\n");
			System.exit(1);
		}
		
		//Connect to the Store
		try {
			clientSocket = new Socket(store[1], Integer.parseInt(store[2]) );
			out = new PrintWriter(clientSocket.getOutputStream(), true);
			in = new BufferedReader( new InputStreamReader(clientSocket.getInputStream()));
		} catch (Exception e) {
			System.err.print("Client unable to connect with Store\n");
			System.exit(1);
		}
		
		//Send Requests to Store
		out.println(commands[0]);
			
		//wait for and print reply
		String reply;		
		while ( (reply = in.readLine()) != null) {
			System.out.println(reply);			
		}
		
		//Need to surround above in this while loop?
		//check isConnected or isClosed??
		out.close();
		in.close();
		System.exit(0);

	}
	
	
	//********************************************************************
	//
	//
	//
	//********************************************************************
	
	static String[] lookupServer(int port) throws IOException {
		
		Socket clientSocket = null;
		PrintWriter out = null;
		BufferedReader in = null;
		
		try {
			//Connect to the NameServer
			clientSocket = new Socket("127.0.0.1", port);
			out = new PrintWriter(clientSocket.getOutputStream(), true);
			in = new BufferedReader( new InputStreamReader(clientSocket.getInputStream()));
		} catch (Exception e) {
			System.err.print("Client unable to connect with NameServer\n");
			System.exit(1);
		}
		
		//Send lookup request
		out.println("look,Store");
		
		//Wait for response, assume it is correct?
		//TODO Check if contains "," for some error handling
		String[] message = in.readLine().split(",");
		out.close();
		in.close();
		clientSocket.close();
		return message;
	}
	
	
	
	//Perform command line parsing
	private static int[] commandParse(String[] args) {
		
		int[] comm = new int[2];
		
		if(args.length == 2) {
			try {
				comm[0] = Integer.parseInt(args[0]);
				comm[1] = Integer.parseInt(args[1]);
			} catch (NumberFormatException e) {
				System.err.print("Invalid command line arguments\n");
				System.exit(1);
			}			
		} else {
			System.err.print("Invalid command line arguments\n");
			System.exit(1);
		}
		return comm;
	}
	
	

}
