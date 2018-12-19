package com.dhomoni.uaa.service.dto;

import java.time.Instant;
import java.util.UUID;

import com.dhomoni.uaa.domain.Patient.BloodGroup;
import com.dhomoni.uaa.domain.Patient.Sex;

import lombok.Data;

@Data
public class PatientDTO {
	
	private UUID id;
	
    private Instant birthTimestamp;
    
    private BloodGroup bloodGroup;
    
    private Sex sex;
    
    private Double weightInKG;
    
    private Double heightInInch;
}
