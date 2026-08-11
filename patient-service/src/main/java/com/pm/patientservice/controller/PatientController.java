package com.pm.patientservice.controller;

import com.pm.patientservice.dto.PatientResponse;
import com.pm.patientservice.service.PatientService;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/patients")
@AllArgsConstructor
public class PatientController {
    private PatientService ps;

    @GetMapping
    public ResponseEntity<List<PatientResponse>> getPatients(){
        List<PatientResponse> res = ps.getPatients();
        return ResponseEntity.ok().body(res);
    }
}
