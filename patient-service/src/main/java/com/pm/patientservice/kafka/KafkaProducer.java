package com.pm.patientservice.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import com.pm.patientservice.model.Patient;
import patient.events.PatientEvent;

@Service
public class KafkaProducer {
  private final KafkaTemplate<String, byte[]> kafkaTemplate;
  private static final Logger log = LoggerFactory.getLogger(KafkaProducer.class);

  public KafkaProducer(KafkaTemplate<String, byte[]> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }

  public void sendEvent(Patient patient) {
    PatientEvent event = PatientEvent.newBuilder()
        .setPatientId(patient.getId().toString())
        .setName(patient.getName())
        .setEmail(patient.getEmail())
        .setEventType("PATIENT_CREATED")
        .build();
    // uses the Java class generated from the .proto file, So the .proto file is a definition
    // and is responsible for creating/defining what PatientEvent looks like

    /*
     * PatientEvent.newBuilder() → gives a Builder
     * ↓
     * provides actual PatientEvent object
     * ↓
     * toByteArray() ← Protobuf serialization happens HERE (Protobuf converts the
     * object into its binary wire format)
     * ↓
     * byte[]
     * ↓
     * kafkaTemplate.send("patient", data);
     * ↓
     * patient topic
     */
    kafkaTemplate.send("patient", event.toByteArray())
        .whenComplete((result, ex) -> {
          if (ex != null) {
            log.error("Failed to send PatientCreated event", ex);
          } else {
            log.info(
                "PatientCreated event sent to topic={}, partition={}, offset={}",
                result.getRecordMetadata().topic(),
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset());
          }
        });
  }
}
