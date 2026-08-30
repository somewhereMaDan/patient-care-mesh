package com.pm.patientservice.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import com.pm.patientservice.Repository.PatientRepository;
import com.pm.patientservice.dto.PatientRequest;
import com.pm.patientservice.dto.PatientResponse;
import com.pm.patientservice.exception.BadRequestException;
import com.pm.patientservice.exception.NotFoundException;
import com.pm.patientservice.grpc.BillingServiceGrpcClient;
import com.pm.patientservice.mapper.PatientMapper;
import com.pm.patientservice.model.Patient;

import io.grpc.StatusRuntimeException;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class PatientService {
  private final PatientRepository PatientModel;
  private final BillingServiceGrpcClient billingServiceGrpcClient;
  private static final Logger log = LoggerFactory.getLogger(BillingServiceGrpcClient.class);

  public List<PatientResponse> getPatients() {
    List<Patient> res = PatientModel.findAll();
    List<PatientResponse> resDTO = res.stream().
    // map(patient -> PatientMapper.toDTO(patient)).toList();
        map(PatientMapper::toDTO).toList();

    return resDTO;
  }

  @Transactional
  public PatientResponse createPatient(PatientRequest req) {
    if (PatientModel.existsByEmail(req.getEmail())) {
      throw new BadRequestException("patient already exists with thsi email" + req.getEmail());
    }
    Patient patient = PatientModel.save(PatientMapper.toModal(req));

    try {
      billingServiceGrpcClient.createBillingAccount(
          patient.getId(),
          patient.getName(),
          patient.getEmail());
    } catch (StatusRuntimeException e) {
      log.error(
          "Failed to create billing account for patient {}. Status: {}",
          patient.getId(),
          e.getStatus(),
          e);

      // Decide what your business wants here
      throw new BadRequestException("Unable to create billing account");
    }

    // billingServiceGrpcClient.createBillingAccount(patient.getId(),
    // patient.getName(), patient.getEmail());

    return PatientMapper.toDTO(patient);
  }

  public PatientResponse updatePatient(UUID id, PatientRequest req) {
    if (id == null)
      throw new BadRequestException("patient id is not present");
    Patient temp_pr = PatientModel.findById(id)
        .orElseThrow(() -> new NotFoundException("Patient record not found"));

    temp_pr.setName(req.getName());
    temp_pr.setEmail(req.getEmail());
    temp_pr.setAddress(req.getAddress());
    temp_pr.setDateOfBirth(LocalDate.parse(req.getDateOfBirth()));

    Patient p_updated = PatientModel.save(temp_pr);
    return PatientMapper.toDTO(p_updated);
  }

  @Transactional
  public String deletePatient(UUID id) {
    if (id == null)
      throw new BadRequestException("patient id is not present");

    int deleted = PatientModel.deletePatientById(id);

    if (deleted == 0)
      throw new NotFoundException("Patient not found");

    return "Patient deleted successfully";
  }
}
