package com.dhomoni.uaa.service.dto;

import java.time.Instant;

import com.dhomoni.uaa.domain.enumeration.BloodGroup;
import com.dhomoni.uaa.domain.enumeration.Sex;

import lombok.Data;

@Data
public class PatientDTO {
	
	private Long id;
	
    private Instant birthTimestamp;
    
    private BloodGroup bloodGroup;
    
    private Sex sex;
    
    private Double weightInKG;
    
    private Double heightInInch;
}
