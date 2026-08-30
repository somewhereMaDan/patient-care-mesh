package com.pm.patientservice.grpc;

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

    ManagedChannel channel = ManagedChannelBuilder.forAddress(serverAddress, serverPort)
        .usePlaintext()
        .build();

    this.blockingStub = BillingServiceGrpc.newBlockingStub(channel);
  }

  public BillingResponse createBillingAccount(UUID patientId, String name, String email) {
    BillingRequest request = BillingRequest.newBuilder().setPatientId(patientId.toString())
        .setName(name).setEmail(email).build();

    BillingResponse response = blockingStub.createBillingAccount(request);

    log.info("Received response from billing service via GRPC: {}", response);
    return response;
  }

}