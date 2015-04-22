import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map.Entry;

/**
 * 
 * @author Sam Damen (42636678)
 * Store
 * COMS3200
 * Assignment 1
 * 
 * 
 */


public class Store {
	
	static String[] bank;
	static String[] content;
	
	public static void main(String[] args) throws IOException {
		
		HashMap<Long,Float> stock = new HashMap<Long,Float> ();	
		ServerSocket serverSoc = null;
		Socket clientSocket = null;
		
		Socket bankSocket = null;
		PrintWriter bankOut = null;
		BufferedReader bankIn = null;
		
		Socket contentSocket = null;
		PrintWriter contentOut = null;
		BufferedReader contentIn = null;
		
		int[] commands = commandParse(args);
		
		//Attempt to read in the stock file
		String path = System.getProperty("user.dir");
		File file = new File(path  + "\\src\\" + args[1]);	
		BufferedReader reader = null;
		
		try {
			reader = new BufferedReader(new FileReader(file));
			String temp = null;
			while ( (temp = reader.readLine()) != null) {
				//Split the line by spaces and store in HashMap
				String[] data = temp.split("\\s+");
				stock.put(Long.parseLong(data[0]), Float.parseFloat(data[1]));
			}
			//Close the stream
			reader.close();
			
		} catch (FileNotFoundException e) {
			System.out.print("File Not Found\n");
			System.exit(1);
		} catch (IOException e) {
			System.out.print("IO Exception\n");
			e.printStackTrace();
			System.exit(1);
		}
		
		//Try to register with the NameServer
		try {
			registrationClient(commands);
		} catch (IOException e) {
			System.err.print("Registration with NameServer failed\n");
			serverSoc.close();
			System.exit(1);
		}
		
		//Try to get Content & Bank information
		try {
			lookupServer(commands[1]);
		} catch (IOException e) {
			System.err.print("unable to connect with NameServer\n");
			System.exit(1);
		}
		
			
		//Open Connections to the Bank & Content Servers
		try {
			bankSocket = new Socket(bank[1], Integer.parseInt(bank[2]) );
			bankOut = new PrintWriter(bankSocket.getOutputStream(), true);
			bankIn = new BufferedReader( new InputStreamReader(bankSocket.getInputStream()));
		} catch (Exception e) {
			System.err.print("Unable to connect with Bank\n");
			System.exit(1);
		}
		
		try {
			contentSocket = new Socket(content[1], Integer.parseInt(content[2]) );
			bankOut = new PrintWriter(contentSocket.getOutputStream(), true);
			bankIn = new BufferedReader( new InputStreamReader(contentSocket.getInputStream()));
		} catch (Exception e) {
			System.err.print("Unable to connect with Content\n");
			System.exit(1);
		}		
		
		//Start listening on port		
		try {
			serverSoc = new ServerSocket(commands[0]);
			System.err.print("Store waiting for incomming connections\n");
		} catch (IOException e) {
			System.err.print("Store unable to listen on given port\n");
			serverSoc.close();
			System.exit(1);
		}
		
		//Perform Server functions
		while (true) {
			
			//Wait for a connection from client
			try {
				clientSocket = serverSoc.accept();				
			} catch (IOException e) {
				System.out.println("could not accept client connection");
			}
			//Create Socket streams
			PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
			BufferedReader in = new BufferedReader( new InputStreamReader(clientSocket.getInputStream()));
			
			//Read the request
			int request = Integer.parseInt(in.readLine());
			if (request == 0) {
				int i = 0;
				//Iterate over all entries in the stock map
				for (Entry<Long,Float> entry : stock.entrySet() ) {
					i++;
					out.println(Integer.toString(i) + ". " + Long.toString(entry.getKey()) + " " + Float.toString(entry.getValue() ));
				}
				clientSocket.close();
			} else {
				//Perform purchase process
				
			}
			
			
			//CloseEverything
			contentSocket.close();
			bankSocket.close();
			//clientSocket.close();
		}
			
			
		
	}
	
	//*************************************************************************
	//
	//
	//
	//*************************************************************************
	
	static void registrationClient(int[] ports) throws IOException {
		
		Socket clientSocket = null;
		PrintWriter out = null;
		BufferedReader in = null;
		
		try {
			//Connect to the NameServer
			clientSocket = new Socket("127.0.0.1", ports[1]);
			out = new PrintWriter(clientSocket.getOutputStream(), true);
			in = new BufferedReader( new InputStreamReader(clientSocket.getInputStream()));
		} catch (Exception e) {
			System.err.println("Registration with NameServer failed\n");
			System.exit(1);
		}
		
		//Send registration request
		out.println("regi,Store,127.0.0.1," + ports[0]);
		
		//Wait for response
		if ( in.readLine().equals("ACK") ){
			out.close();
			in.close();
			clientSocket.close();			
		} else {
			System.err.print("Registration with NameServer failed\n");
			System.exit(1);
		}		
	}
	
	
	static void lookupServer(int port) throws IOException {
		
		Socket clientSocket = null;
		PrintWriter out = null;
		BufferedReader in = null;
		
		try {
			//Connect to the NameServer
			clientSocket = new Socket("127.0.0.1", port);
			out = new PrintWriter(clientSocket.getOutputStream(), true);
			in = new BufferedReader( new InputStreamReader(clientSocket.getInputStream()));
		} catch (Exception e) {
			System.err.print("unable to connect with NameServer\n");
			System.exit(1);
		}
		
		//Send lookup request
		out.println("look,Bank");
		
		//Wait for response
		bank = in.readLine().split(",");
		if (! (bank[0].equals("Bank")) ) {
			System.out.print("Bank has not registered\n");
			System.exit(1);
		}
		
		//Send Lookup request
		out.println("look,Content");
		
		//Wait for response
		content = in.readLine().split(",");
		if (! (content[0].equals("Content")) ) {
			System.out.print("Content has not registered\n");
			System.exit(1);
		}
		
		out.close();
		in.close();
		clientSocket.close();
	}
	
	
	
	
	//Perform command line parsing
	private static int[] commandParse(String[] args) {
		
		int[] ports = new int[2];
		
		if(args.length == 3) {
			try {
				ports[0] = Integer.parseInt(args[0]);
				ports[1] = Integer.parseInt(args[2]);
			} catch (NumberFormatException e) {
				System.err.print("Invalid command line arguments for Store\n");
				System.exit(1);
			}			
		} else {
			System.err.print("Invalid command line arguments for Store\n");
			System.exit(1);
		}
		return ports;
	}
	
	

}
