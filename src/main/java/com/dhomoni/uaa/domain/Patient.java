package com.dhomoni.uaa.domain;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.constraints.Pattern;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.dhomoni.uaa.config.Constants;
import com.vividsolutions.jts.geom.Point;

import lombok.Data;

@Entity
@Table(name = "patient")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Data
public class Patient implements Serializable {

	private static final long serialVersionUID = 1L;

	public enum Sex {
		MALE, FEMALE
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column( columnDefinition = "uuid")
	private UUID id;

	@Pattern(regexp = Constants.PHONE_REGEX)
	private String phone;

	@Column(name = "sex")
	private Sex sex;

	@Column(name = "birth_timestamp")
	private Instant birthTimestamp;

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

	public enum BloodGroup {
		A_POSITIVE("A+"), A_NEGATIVE("A-"), B_POSITIVE("B+"), B_NEGATIVE("B-"), AB_POSITIVE("AB+"), AB_NEGATIVE("AB-"),
		O_NEGATIVE("O-"), O_POSITIVE("O+");

		private final String label;

		private BloodGroup(String label) {
			this.label = label;
		}

		@Override
		public String toString() {
			return label;
		}
	}
}
