/**
 * 
 * @author Sam Damen (42636678)
 * Store
 * COMS3200
 * Assignment 1
 * 
 * NON-BLOCKING
 */


public class Store {
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		commandParse(args);

	}
	
	
	
	
	
	
	//Perform command line parsing
	private static int commandParse(String[] args) {
		
		int port = 0;
		
		if(args.length == 3) {
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
	
	

}
