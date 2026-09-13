package com.pm.patientservice.grpc;

import java.lang.module.ModuleDescriptor.Opens;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import billing.BillingRequest;
import billing.BillingResponse;
import billing.BillingServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

@Service
public class BillingServiceGrpcClient {
  private static final Logger log = LoggerFactory.getLogger(BillingServiceGrpcClient.class);
  private final BillingServiceGrpc.BillingServiceBlockingStub blockingStub;

  // localhost:9001/BillingService/CreatePatientAccount
  // aws.grpc:12312/...
  public BillingServiceGrpcClient(
      @Value("${billing.service.address:localhost}") String serverAddress,
      @Value("${billing.service.grpc.port:9001}") int serverPort) {

    log.info("Connecting to BillingService gRPC server at {}:{}", serverAddress, serverPort);

    ManagedChannel channel = ManagedChannelBuilder.forAddress(serverAddress, serverPort) // it manages the underlying TCP connection(s) to billing-service:9001 for you
        .usePlaintext() // means no TLS (fine inside a Docker network)
        .build();

    // When we compile the .proto file, protobuf generates a Java class BillingServiceGrpc that contains different types of stubs:
    // newBlockingStub(channel) — the calling thread blocks until the remote method returns. Simple to use, but ties up a thread.
    // newFutureStub(channel) — returns a ListenableFuture, non-blocking.
    // newStub(channel) — fully async with StreamObserver.

    this.blockingStub = BillingServiceGrpc.newBlockingStub(channel);
  }

  public BillingResponse createBillingAccount(UUID patientId, String name, String email) {
    BillingRequest request = BillingRequest.newBuilder().setPatientId(patientId.toString())
        .setName(name).setEmail(email).build();

    // The stub is a generated proxy object. When you call blockingStub.createBillingAccount(request), it:
    // Serializes your BillingRequest protobuf to binary
    // Opens an HTTP/2 stream over the ManagedChannel
    // Sends it to billing-service:9001
    // Blocks your thread waiting for the response binary
    // Deserializes it back into a BillingResponse and returns it
        BillingResponse response = blockingStub.createBillingAccount(request);
    //   Thread-1
    //   |
    //   | createBillingAccount()
    //   |
    //   v
    // blockingStub.createBillingAccount(request)
    //   |
    //   | ---- gRPC request ---->
    //   |
    //   |                 Billing Service
    //   |                       |
    //   |                 creates account
    //   |                       |
    //   | <--- gRPC response ---
    //   |
    //   v
    // response
    // While the Billing Service is processing the request, Thread-1 waits.

    // log.info("Received response from billing service via GRPC: {}", response);
    System.out.println("Received response from billing service via GRPC: " + response);
    return response;
  }
}

//     Thread-1:
//     send request
//        ↓
//     WAITING... ⏳
//        ↓
//     WAITING... ⏳
//        ↓
//     Billing Service responds
//        ↓
//     continue execution
//        ↓
//     log.info(...)
//        ↓
//     return response


// Use async stub, moving forward

//     Your Thread
//     │
//     ├── send gRPC request
//     │
//     └── continue doing other work
//              │
//              │
//              │     Billing Service
//              │          │
//              │          │ processing
//              │          ▼
//              │       response
//              │
//              └── callback/future gets response