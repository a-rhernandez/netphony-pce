package es.tid.pce.server;

import java.io.IOException;

import io.grpc.Server;
import io.grpc.ServerBuilder;

public class PCEServer {

	public static void main(String[] args) throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		int port = 10060;
		DomainPCEServer pceserver = new DomainPCEServer();
		if (args.length > 0)
			pceserver.configure(args[0]);
		else
			pceserver.configure(null);

		Server server = ServerBuilder.forPort(10060).addService(pceserver).build();

		// Start the server
		server.start();

		// Server threads are running in the background.
		System.out.println("Server started started, listening on " + port);

		// Run the logic of the PCE server
		pceserver.run();

		// Don't exit the main thread. Wait until server is terminated.
		server.awaitTermination();
	}

}
