package grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import src.main.proto.pceServiceGrpc;
import src.main.proto.GrpcService.commandRequest;
import src.main.proto.GrpcService.commandResponse;
import src.main.proto.pceServiceGrpc.pceServiceBlockingStub;

public class ActiveSessions {
	public static void main(String[] args) throws Exception {
		ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 9091).usePlaintext().build();

		pceServiceBlockingStub stub = pceServiceGrpc.newBlockingStub(channel);

		
		
		commandRequest request = commandRequest.newBuilder().build();
		System.out.println(request.getCommand());
		commandResponse response = stub.update(request);
		
		System.out.println("\nRESPUESTA RECIBIDA");
		System.out.println(response);
		System.out.println(response.getErrorMessage());
	}
}
