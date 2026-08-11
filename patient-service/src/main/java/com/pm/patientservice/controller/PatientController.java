package com.pm.patientservice.controller;

import com.pm.patientservice.dto.PatientRequest;
import com.pm.patientservice.dto.PatientResponse;
import com.pm.patientservice.service.PatientService;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/patients")
@AllArgsConstructor
public class PatientController {
    private PatientService ps;

    @GetMapping
    public ResponseEntity<List<PatientResponse>> get(){
        List<PatientResponse> res = ps.getPatients();
        return ResponseEntity.ok().body(res);
    }

    @PostMapping
    public ResponseEntity<PatientResponse> create(@RequestBody PatientRequest req){
        PatientResponse res = ps.createPatient(req);
        return ResponseEntity.ok().body(res);
    }
}
