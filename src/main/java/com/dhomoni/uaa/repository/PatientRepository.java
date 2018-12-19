package com.dhomoni.uaa.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dhomoni.uaa.domain.Patient;
import com.dhomoni.uaa.domain.User;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {
	
	Optional<Patient> findOneByUser(User user);

}
