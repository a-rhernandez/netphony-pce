package grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import src.main.proto.GrpcService.commandRequest;
import src.main.proto.GrpcService.commandResponse;

import java.util.logging.Logger;

import src.main.proto.GrpcService.LSPdb_Request;
import src.main.proto.GrpcService.LSPdb_Response;

import src.main.proto.pceServiceGrpc;
import src.main.proto.pceServiceGrpc.pceServiceBlockingStub;
import src.main.proto.pceServiceGrpc.pceServiceStub;

public class grpcClient {
	private static final Logger logger = Logger.getLogger(grpcClient.class.getName());

	public static void main(String[] args) throws Exception {
		ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 9091).usePlaintext().build();

		pceServiceBlockingStub stub = pceServiceGrpc.newBlockingStub(channel);

		// Construct a request [LSPDB Request]
		LSPdb_Request requestlsp = LSPdb_Request.newBuilder().build();

		LSPdb_Response responselsp = stub.getLSPdb(requestlsp);
		System.out.println("\nRESPUESTA RECIBIDA");
		System.out.println(responselsp);
	}
}