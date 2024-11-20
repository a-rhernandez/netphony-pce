package grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import src.main.proto.GrpcService.commandRequest;
import src.main.proto.GrpcService.commandResponse;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import src.main.proto.pceServiceGrpc;
import src.main.proto.pceServiceGrpc.pceServiceBlockingStub;
import src.main.proto.pceServiceGrpc.pceServiceStub;

public class grpcClient_Command {
	//private static final Logger logger = Logger.getLogger(grpcClient.class.getName());

	public static void main(String[] args) throws Exception {
		ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 9091).usePlaintext().build();

		pceServiceBlockingStub stub = pceServiceGrpc.newBlockingStub(channel);

		// Construct a request [Command Request]
		String n = "initiate lsp largo2 10.95.90.56 1.1.1.1 1.1.1.2 m69644288 nn1.1.1.3 m69640192 nn1.1.1.2";
		commandRequest request = commandRequest.newBuilder().setCommand(n).build();
		System.out.println(request.getCommand());
		commandResponse response = stub.update(request);
		
		System.out.println("\nRESPUESTA RECIBIDA");
		System.out.println(response);
		System.out.println(response.getErrorMessage());
	}
}

/*
SERVICE REQUEST
// Construct a request [Service Request]
String n = "initiate lsp largo2 10.95.86.214 1.1.1.1 1.1.1.2 m69644288 nn1.1.1.3 m69640192 nn1.1.1.2";
commandRequest request = commandRequest.newBuilder().setCommand(n).build();

// Make an Asynchronous call. Listen to responses w/ StreamObserver
stub.update(request, new StreamObserver <commandResponse>() {
	  
	  public void onNext(commandResponse response) {
		  System.out.println("respuesta del server: "+response);
		  }
	  public void onError(Throwable t) {
		  System.out.println("error: "+t.getMessage());
		  }
	  public void onCompleted() {
		  // Typically you'll shutdown the channel somewhere else.
		  // But for the purpose of the lab, we are only making a single
		  // request. We'll shutdown as soon as this request is done.
		  System.out.println("command completed");
		  }
	  });
*/