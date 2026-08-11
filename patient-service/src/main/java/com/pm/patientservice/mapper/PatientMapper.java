package com.pm.patientservice.mapper;

import com.pm.patientservice.dto.PatientRequest;
import com.pm.patientservice.dto.PatientResponse;
import com.pm.patientservice.model.Patient;

import java.time.LocalDate;

public class PatientMapper {
    public static PatientResponse toDTO(Patient patient){
        PatientResponse p = new PatientResponse();
        p.setId(patient.getId().toString());
        p.setName(patient.getName());
        p.setEmail(patient.getEmail());
        p.setAddress(patient.getAddress());
        p.setDob(patient.getDateOfBirth().toString());

        return p;
    }

    public static Patient toModal(PatientRequest pr){
        Patient p = new Patient();
        p.setName(pr.getName());
        p.setEmail(pr.getEmail());
        p.setAddress(pr.getAddress());
        p.setDateOfBirth(LocalDate.parse(pr.getDateOfBirth()));
        p.setRegisteredDate(LocalDate.parse(pr.getRegisteredDate()));

        return p;
    }
}
