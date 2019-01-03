package com.dhomoni.uaa.service.dto;

import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.dhomoni.uaa.domain.ProfessionalDegree;
import com.dhomoni.uaa.domain.enumeration.DoctorType;

import lombok.Data;

@Data
public class DoctorDTO {
	
    private static final int LICENCE_NUMBER_MIN_LENGTH = 4;
    private static final int LICENCE_NUMBER_MAX_LENGTH = 20;
	
	private UUID id;
	
    private DoctorType type;
    
    private Integer department;
	
    private String licenceNumber;

    private String nationalId;
    
    private String passportNo;
    
    private String designation;

    private String description;
    
    private Set<ProfessionalDegree> professionalDegrees;
    
    public boolean isLiceceNumberValid() {
		return !StringUtils.isEmpty(this.licenceNumber) 
				&& this.licenceNumber.length() >= LICENCE_NUMBER_MIN_LENGTH
				&& this.licenceNumber.length() <= LICENCE_NUMBER_MAX_LENGTH;
    }
}
