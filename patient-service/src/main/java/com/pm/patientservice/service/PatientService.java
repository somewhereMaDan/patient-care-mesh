package com.pm.patientservice.service;

import com.pm.patientservice.Repository.PatientRepository;
import com.pm.patientservice.dto.PatientRequest;
import com.pm.patientservice.dto.PatientResponse;
import com.pm.patientservice.mapper.PatientMapper;
import com.pm.patientservice.model.Patient;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class PatientService {
    private PatientRepository PatientModel;

    public List<PatientResponse> getPatients(){
        List<Patient> res = PatientModel.findAll();
        List<PatientResponse> resDTO = res.stream().
//                map(patient -> PatientMapper.toDTO(patient)).toList();
                map(PatientMapper::toDTO).toList();

        return resDTO;
    }

    public PatientResponse createPatient(PatientRequest req){
        Patient res = PatientModel.save(PatientMapper.toModal(req));

        return PatientMapper.toDTO(res);
    }
}
