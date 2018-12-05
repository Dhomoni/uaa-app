package com.dhomoni.uaa.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.dhomoni.uaa.domain.Doctor;
import com.dhomoni.uaa.domain.User;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

	Optional<Doctor> findOneByLicenceNumber(String licenceNumber);
	
	Optional<Doctor> findOneByUser(User user);
}
