package com.pm.patientservice.service;

import com.pm.patientservice.Repository.PatientRepository;
import com.pm.patientservice.dto.PatientRequest;
import com.pm.patientservice.dto.PatientResponse;
import com.pm.patientservice.exception.BadRequestException;
import com.pm.patientservice.exception.NotFoundException;
import com.pm.patientservice.mapper.PatientMapper;
import com.pm.patientservice.model.Patient;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

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

    public PatientResponse updatePatient(UUID id, PatientRequest req){
        if(id == null) throw new BadRequestException("patient id is not present");
        Patient temp_pr = PatientModel.findById(id).orElseThrow(() -> new NotFoundException("Patient record not found"));

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
