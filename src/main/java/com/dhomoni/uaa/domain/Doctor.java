package com.dhomoni.uaa.domain;

import java.io.Serializable;
import java.util.Set;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.constraints.Pattern;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.dhomoni.uaa.config.Constants;
import com.vividsolutions.jts.geom.Point;

import lombok.Data;

@Entity
@Table(name = "doctor")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Data
public class Doctor implements Serializable {
    
	private static final long serialVersionUID = 1L;
	
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column( columnDefinition = "uuid")
    private UUID id;
    
    @Pattern(regexp = Constants.PHONE_REGEX)
    private String phone;

    @Column(name = "type")
    private Integer type;
    
    @Column(name = "department")
    private Integer department;
    
    @Column(name = "licence_number")
    private String licenceNumber;

    @Column(name = "national_id")
    private String nationalId;
    
    @Column(name = "passport_no")
    private String passportNo;
    
    @Column(name = "designation")
    private String designation;

    @Column(name = "description")
    private String description;
    
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "doctor_id")
    private Set<Degree> degrees;
    
    @Column(name = "address")
    private String address;
    
    @Column(name = "GEOM", columnDefinition = "GEOMETRY(Point,4326)")
    private Point location;

    @Lob
    @Column(name = "image")
    private byte[] image;

    @Column(name = "image_content_type")
    private String imageContentType;

    @OneToOne
    private User user;
}
