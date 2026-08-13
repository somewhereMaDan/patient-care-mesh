package com.pm.patientservice.controller;

import com.pm.patientservice.dto.PatientRequest;
import com.pm.patientservice.dto.PatientResponse;
import com.pm.patientservice.dto.Validators.CreatePatientValidationGroup;
import com.pm.patientservice.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@RestController
@RequestMapping("/patients")
@Tag(name = "Patient", description = "API for mananging Patient service")
public class PatientController {
  private PatientService ps;

  @GetMapping
  public ResponseEntity<List<PatientResponse>> get() {
    List<PatientResponse> res = ps.getPatients();
    return ResponseEntity.ok().body(res);
  }

  @PostMapping
  public ResponseEntity<PatientResponse> create(@Validated({Default.class, CreatePatientValidationGroup.class}) @RequestBody PatientRequest req) {
    PatientResponse res = ps.createPatient(req);
    return ResponseEntity.ok().body(res);
  }

  @PutMapping("/{id}")
  @Operation(summary = "updating a patient")
  public ResponseEntity<PatientResponse> update(@PathVariable UUID id, @Validated({Default.class}) @RequestBody PatientRequest req) {
    PatientResponse res = ps.updatePatient(id, req);
    return ResponseEntity.ok().body(res);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<String> delete(@PathVariable UUID id) {
    String res = ps.deletePatient(id);
    return ResponseEntity.ok().body(res);
  }
}
