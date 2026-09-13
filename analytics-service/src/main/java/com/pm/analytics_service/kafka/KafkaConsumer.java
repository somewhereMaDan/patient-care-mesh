package com.pm.analytics_service.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.google.protobuf.InvalidProtocolBufferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import patient.events.PatientEvent;

@Service
public class KafkaConsumer {
  private static final Logger log = LoggerFactory.getLogger(KafkaConsumer.class);

  @KafkaListener(topics = "patient", groupId = "analytics-service")
  public void consumeEvent(byte[] event) {
    try {
      /*
       * Kafka topic
       * ↓
       * byte[]
       * ↓
       * PatientEvent.parseFrom(event)
       * ↓
       * PatientEvent object
       */
      PatientEvent patientEvent = PatientEvent.parseFrom(event);
      // we can perform any business logic here with analytics for patient

      log.info("Received patient event in analytics-service: [PatientId={}, PatientName={}, PatientEmail={}",
          patientEvent.getPatientId(), patientEvent.getName(), patientEvent.getEmail());
    } catch (InvalidProtocolBufferException e) {
      log.error("error deserilizing the event {}", e.getMessage());
    }
  }
}
