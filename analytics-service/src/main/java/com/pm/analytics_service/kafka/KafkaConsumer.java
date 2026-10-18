package com.pm.analytics_service.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.google.protobuf.InvalidProtocolBufferException;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import patient.events.PatientEvent;

// Kafka tracks how much data has been written(LOG_END_OFFSET) and how far each consumer group has progressed (CURRENT_OFFSET). 
// The difference between those positions is what helps you identify consumer LAG.

@Service
public class KafkaConsumer {

  private static final Logger log =
      LoggerFactory.getLogger(KafkaConsumer.class);

  @KafkaListener(
      topics = "patient",
      groupId = "analytics-service"
  )
  public void consumeEvent(ConsumerRecord<String, byte[]> record) {
    try {
      // Identify the instance processing this event
      String instance = System.getenv("HOSTNAME");

      // Get Kafka metadata
      int partition = record.partition();
      long offset = record.offset();
      String key = record.key();

      // Deserialize the Protobuf event
      PatientEvent patientEvent =
          PatientEvent.parseFrom(record.value());

      log.info(
          "Instance={}, Topic={}, Partition={}, Offset={}, Key={}, " +
          "PatientId={}, PatientName={}, PatientEmail={}",
          instance,
          record.topic(),
          partition,
          offset,
          key,
          patientEvent.getPatientId(),
          patientEvent.getName(),
          patientEvent.getEmail()
      );

      // Your analytics business logic goes here

    } catch (InvalidProtocolBufferException e) {
      log.error("Error deserializing patient event", e);
    }
  } // Spring Kafka continues and it handles offset commits 
}
