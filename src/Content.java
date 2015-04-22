/**
 * 
 * @author Sam Damen (42636678)
 * Content
 * COMS3200
 * Assignment 1
 * 
 *  Register Message format = "regi,name,ip,port\n"
 *  Store return format     = "item_id item_name"
 *  content not found       = "NACK\n"
 *  
 *  
 * 
 */


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


public class Content {
	
	public static void main(String[] args) throws IOException {
		
		HashMap<Long,String> content = new HashMap<Long,String> ();
		ServerSocket serverSoc = null;		
		
		int[] ports = commandParse(args);
		
		
		//Attempt to read in the content file
		String path = System.getProperty("user.dir");
		File file = new File(path  + "\\src\\" + args[1]);	
		BufferedReader reader = null;
		
		try {
			reader = new BufferedReader(new FileReader(file));
			String temp = null;
			while ( (temp = reader.readLine()) != null) {
				//Split the line by spaces and store in HashMap
				String[] data = temp.split("\\s+");
				content.put(Long.parseLong(data[0]), data[1]);
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
				
		//Start listening on port		
		try {
			serverSoc = new ServerSocket(ports[0]);
			System.err.print("Content waiting for incomming connections\n");
		} catch (IOException e) {
			System.err.print("Content unable to listen on given port\n");
			serverSoc.close();
			System.exit(1);
		}
		
		//Try to register with the NameServer
		try {
			registrationClient(ports);
		} catch (IOException e) {
			System.err.print("Content registration with NameServer failed\n");
			serverSoc.close();
			System.exit(1);
		}
		
		System.out.print("REGISTERED\n");
		
		//Perform Server functions
		Socket connSocket = null;
		while(true) {
			//Wait for a connection from Store
			try {
				connSocket = serverSoc.accept();				
			} catch (IOException e) {
				System.out.println("Failed to make connection with store");
			}
			//Create Socket streams
			PrintWriter out = new PrintWriter(connSocket.getOutputStream(), true);
			BufferedReader in = new BufferedReader( new InputStreamReader(connSocket.getInputStream()));

			//Handle requests from Store
			String temp = null;
			while( (temp = in.readLine()) != null) {
				Long id = Long.parseLong(temp);
				if ( content.containsKey(id) ) {
					out.println(id + " " + content.get(id));
				} else {
					out.println("NACK");
				}
			}
			//Close connection
			out.close();
			in.close();
			connSocket.close();
		}		
		
	}	
	
	//***************************************************************************
	//
	//
	//
	//***************************************************************************
	
	
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
			System.err.println("Content Registration with NameServer failed\n");
			System.exit(1);
		}
		
		//Send registration request
		out.println("regi,Content,127.0.0.1," + ports[0]);
		
		//Wait for response
		
		//System.out.print(in.readLine());
		
		if ( in.readLine().equals("ACK") ){
			
			out.close();
			in.close();
			clientSocket.close();			
		} else {
			System.err.print("Content rEgistration with NameServer failed\n");
			System.exit(1);
		}		
	}
	
	
	//Perform command line parsing
	private static int[] commandParse(String[] args) {
		
		int[] ports = new int[2];
		
		if(args.length == 3) {
			try {
				ports[0] = Integer.parseInt(args[0]);
				ports[1] = Integer.parseInt(args[2]);
			} catch (NumberFormatException e) {
				System.err.print("Invalid command line arguments for Content\n");
				System.exit(1);
			}			
		} else {
			System.err.print("Invalid command line arguments for Content\n");
			System.exit(1);
		}
		return ports;
	}
	

}
