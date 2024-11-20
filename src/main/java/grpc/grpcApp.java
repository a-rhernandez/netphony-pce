package grpc;

import es.tid.pce.server.DomainPCEServer;
import io.grpc.Server;
import io.grpc.ServerBuilder;

public class grpcApp {
	
	public static void main( String[] args ) throws Exception
    {
      // Create a new server to listen on port 10060
		int port=10060;
		DomainPCEServer  pceserver = new DomainPCEServer();
		
		if(args.length > 0)
			pceserver.configure(args[0]);
		else 
			pceserver.configure(null);
		
		Server server = ServerBuilder.forPort(port)
        .addService(pceserver)
        .build();
		
		// Start the server
	    server.start();

	    // Server threads are running in the background.
	    System.out.println("Server started started, listening on "+port);
				
	    //Run the logic of the PCE server
		pceserver.run();
      
      // Don't exit the main thread. Wait until server is terminated.
		server.awaitTermination();
    }
}
