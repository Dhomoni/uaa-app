package com.dhomoni.uaa.domain;

import java.io.Serializable;
import java.time.Instant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.Pattern;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.dhomoni.uaa.config.Constants;
import com.dhomoni.uaa.domain.enumeration.BloodGroup;
import com.dhomoni.uaa.domain.enumeration.Sex;
import com.vividsolutions.jts.geom.Point;

import lombok.Data;

@Entity
@Table(name = "patient")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Data
public class Patient implements Serializable {

	private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

	@Pattern(regexp = Constants.PHONE_REGEX)
	private String phone;

	@Enumerated(EnumType.STRING)
	@Column(name = "sex")
	private Sex sex;

	@Column(name = "birth_timestamp")
	private Instant birthTimestamp;

	@Enumerated(EnumType.STRING)
	@Column(name = "blood_group")
	private BloodGroup bloodGroup;

	@Column(name = "weight_in_kg")
	private Double weightInKG;

	@Column(name = "height_in_inch")
	private Double heightInInch;

	@Lob
	@Column(name = "image")
	private byte[] image;

	@Column(name = "image_content_type")
	private String imageContentType;

	@Column(name = "address")
	private String address;

	@Column(name = "GEOM", columnDefinition = "GEOMETRY")
	private Point location;

	@OneToOne
	private User user;
}
