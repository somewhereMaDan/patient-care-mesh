package com.pm.patientservice.mapper;

import com.pm.patientservice.dto.PatientResponse;
import com.pm.patientservice.model.Patient;

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
}
