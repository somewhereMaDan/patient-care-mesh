package com.pm.billing_service.grpc;

import billing.BillingResponse;
import billing.BillingServiceGrpc.BillingServiceImplBase;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


// When billing-service starts, the @GrpcService annotation (from grpc-spring-boot-starter) 
// automatically registers BillingGrpcService with the gRPC server running on port 9001

@GrpcService
public class BillingGrpcService extends BillingServiceImplBase {
  private static final Logger log = LoggerFactory.getLogger(BillingGrpcService.class);

  @Override
  public void createBillingAccount(billing.BillingRequest billingRequest,
      StreamObserver<billing.BillingResponse> responseObserver) {
    // log.info("createBillingAccount request received {}", billingRequest.toString());
    System.out.println("createBillingAccount request received: " + billingRequest.toString());

    // dummy response
    BillingResponse resopnse = BillingResponse.newBuilder()
        .setAccountId("1234")
        .setStatus("ACTIVE")
        .build();

    responseObserver.onNext(resopnse); // sending the response from GRPC service to back to the client (in this
                                       // scenario -> patient service)
    responseObserver.onCompleted(); // to end the cycle/stream, because we can return as many response as we want
  }
}


// billingRequest — the framework deserializes the binary payload that came over the wire from patient-service back into a BillingRequest protobuf object. 
// All the fields (patientId, name, email) are already set because patient-service called .setPatientId() etc. before sending.

// responseObserver — this is a gRPC-managed callback object the framework hands you. It's how you send data back to the caller. 
// Since gRPC supports streaming (you could call onNext() multiple times), you signal you're done with onCompleted(). 
// When you call responseObserver.onNext(response), the framework serializes your BillingResponse and sends it back over the same HTTP/2 stream to patient-service, 
// where the blocking stub unblocks and returns it.