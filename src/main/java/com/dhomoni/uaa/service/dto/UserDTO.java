package com.dhomoni.uaa.service.dto;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.dhomoni.uaa.config.Constants;
import com.dhomoni.uaa.domain.Authority;
import com.dhomoni.uaa.domain.Doctor;
import com.dhomoni.uaa.domain.Patient;
import com.dhomoni.uaa.domain.User;
import com.dhomoni.uaa.security.AuthoritiesConstants;
import com.vividsolutions.jts.geom.Point;

import lombok.Data;

/**
 * A DTO representing a user (doctor + patient), with his authorities.
 */
@Data
public class UserDTO {
    private Long id;

    @NotBlank
    @Pattern(regexp = Constants.LOGIN_REGEX)
    @Size(min = 1, max = 50)
    private String login;

    @Size(max = 50)
    private String firstName;

    @Size(max = 50)
    private String lastName;

    @Email
    @Size(min = 5, max = 254)
    private String email;

    @Size(max = 256)
    private String imageUrl;

    private boolean activated = false;

    @Size(min = 2, max = 6)
    private String langKey;

    private String createdBy;

    private Instant createdDate;

    private String lastModifiedBy;

    private Instant lastModifiedDate;

    private Set<String> authorities;
    
    @Pattern(regexp = Constants.PHONE_REGEX)
    private String phone;
    
    private byte[] image;

    private String imageContentType;
    
    private String address;
    
    private Point location;
    
    DoctorDTO doctorDTO;
    
    PatientDTO patientDTO;

    public UserDTO() {
    	// for jackson
    }
    
    public UserDTO(User user) {
        this.id = user.getId();
        this.login = user.getLogin();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.email = user.getEmail();
        this.activated = user.getActivated();
        this.imageUrl = user.getImageUrl();
        this.langKey = user.getLangKey();
        this.createdBy = user.getCreatedBy();
        this.createdDate = user.getCreatedDate();
        this.lastModifiedBy = user.getLastModifiedBy();
        this.lastModifiedDate = user.getLastModifiedDate();
        this.authorities = user.getAuthorities().stream()
            .map(Authority::getName)
            .collect(Collectors.toSet());
    }
    
    public UserDTO(Doctor doctor) {
    	this(doctor.getUser());
    	this.phone = doctor.getPhone();
    	this.image = doctor.getImage();
    	this.imageContentType = doctor.getImageContentType();
    	this.address = doctor.getAddress();
    	this.location = doctor.getLocation();
    	this.doctorDTO = new DoctorDTO();
    	this.doctorDTO.setId(doctor.getId());
    	this.doctorDTO.setLicenceNumber(doctor.getLicenceNumber());
    	this.doctorDTO.setNationalId(doctor.getNationalId());
    	this.doctorDTO.setPassportNo(doctor.getPassportNo());
    	this.doctorDTO.setDesignation(doctor.getDesignation());
    	this.doctorDTO.setType(doctor.getType());
    	this.doctorDTO.setDepartment(doctor.getDepartment());
    	this.doctorDTO.setDescription(doctor.getDescription());
    	this.doctorDTO.setProfessionalDegrees(doctor.getProfessionalDegrees());
    }

    public UserDTO(Patient patient) {
    	this(patient.getUser());
    	this.phone = patient.getPhone();
    	this.image = patient.getImage();
    	this.imageContentType = patient.getImageContentType();
    	this.address = patient.getAddress();
    	this.location = patient.getLocation();
    	this.patientDTO = new PatientDTO();
    	this.patientDTO.setId(patient.getId());
    	this.patientDTO.setBloodGroup(patient.getBloodGroup());
    	this.patientDTO.setSex(patient.getSex());
    	this.patientDTO.setBirthTimestamp(patient.getBirthTimestamp());
    	this.patientDTO.setWeightInKG(patient.getWeightInKG());
    	this.patientDTO.setHeightInInch(patient.getHeightInInch());
    }
    
    public boolean hasDoctorAuthority() {
    	return authorities!=null 
    			&& authorities.contains(AuthoritiesConstants.DOCTOR);
    }

    public boolean hasPatientAuthority() {
    	return authorities!=null 
    			&& authorities.contains(AuthoritiesConstants.PATIENT);
    }
    
	public Optional<DoctorDTO> getDoctorDTO() {
		return Optional.ofNullable(this.doctorDTO);
	}
	
	public Optional<PatientDTO> getPatientDTO() {
		return Optional.ofNullable(this.patientDTO);
	}
}
