package com.dhomoni.uaa.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dhomoni.uaa.config.Constants;
import com.dhomoni.uaa.domain.Authority;
import com.dhomoni.uaa.domain.Doctor;
import com.dhomoni.uaa.domain.Patient;
import com.dhomoni.uaa.domain.User;
import com.dhomoni.uaa.repository.AuthorityRepository;
import com.dhomoni.uaa.repository.DoctorRepository;
import com.dhomoni.uaa.repository.PatientRepository;
import com.dhomoni.uaa.repository.UserRepository;
import com.dhomoni.uaa.repository.search.UserSearchRepository;
import com.dhomoni.uaa.security.AuthoritiesConstants;
import com.dhomoni.uaa.security.SecurityUtils;
import com.dhomoni.uaa.service.dto.UserDTO;
import com.dhomoni.uaa.service.dto.DoctorDTO;
import com.dhomoni.uaa.service.util.RandomUtil;
import com.dhomoni.uaa.web.rest.errors.DoctorDataNotFoundException;
import com.dhomoni.uaa.web.rest.errors.EmailAlreadyUsedException;
import com.dhomoni.uaa.web.rest.errors.InvalidLicenceNumberException;
import com.dhomoni.uaa.web.rest.errors.InvalidPasswordException;
import com.dhomoni.uaa.web.rest.errors.LicenceNumberAlreadyUsedException;
import com.dhomoni.uaa.web.rest.errors.LoginAlreadyUsedException;

/**
 * Service class for managing users.
 */
@Service
@Transactional
public class UserService {

	private static final String CHANGED_INFORMATION_FOR_USER = "Changed Information for User: {}";

	private final Logger log = LoggerFactory.getLogger(UserService.class);

	private final UserRepository userRepository;

	private final DoctorRepository doctorRepository;

	private final PatientRepository patientRepository;

	private final PasswordEncoder passwordEncoder;

	private final UserSearchRepository userSearchRepository;

	private final AuthorityRepository authorityRepository;

	private final CacheManager cacheManager;

	public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, UserSearchRepository userSearchRepository, 
    		DoctorRepository doctorRepository, PatientRepository patientRepository,
    		AuthorityRepository authorityRepository, CacheManager cacheManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userSearchRepository = userSearchRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.authorityRepository = authorityRepository;
        this.cacheManager = cacheManager;
    }

	public Optional<User> activateRegistration(String key) {
		log.debug("Activating user for activation key {}", key);
		return userRepository.findOneByActivationKey(key).map(user -> {
			// activate given user for the registration key.
			user.setActivated(true);
			user.setActivationKey(null);
			userSearchRepository.save(user);
			this.clearUserCaches(user);
			log.debug("Activated user: {}", user);
			return user;
		});
	}

	public Optional<User> completePasswordReset(String newPassword, String key) {
		log.debug("Reset user password for reset key {}", key);
		return userRepository.findOneByResetKey(key)
				.filter(user -> user.getResetDate().isAfter(Instant.now().minusSeconds(86400))).map(user -> {
					user.setPassword(passwordEncoder.encode(newPassword));
					user.setResetKey(null);
					user.setResetDate(null);
					this.clearUserCaches(user);
					return user;
				});
	}

	public Optional<User> requestPasswordReset(String mail) {
		return userRepository.findOneByEmailIgnoreCase(mail).filter(User::getActivated).map(user -> {
			user.setResetKey(RandomUtil.generateResetKey());
			user.setResetDate(Instant.now());
			this.clearUserCaches(user);
			return user;
		});
	}

	public User registerUser(UserDTO userDTO, String password) {
        userRepository.findOneByLogin(userDTO.getLogin().toLowerCase()).ifPresent(existingUser -> {
            boolean removed = removeNonActivatedUser(existingUser);
            if (!removed) {
                throw new LoginAlreadyUsedException();
            }
        });
        userRepository.findOneByEmailIgnoreCase(userDTO.getEmail()).ifPresent(existingUser -> {
            boolean removed = removeNonActivatedUser(existingUser);
            if (!removed) {
                throw new EmailAlreadyUsedException();
            }
        });
        if(userDTO.hasDoctorAuthority()) {
        	DoctorDTO doctorDTO = userDTO.getDoctorDTO().orElseThrow(DoctorDataNotFoundException::new);
        	if(!doctorDTO.isLiceceNumberValid()) {
        		throw new InvalidLicenceNumberException();
        	}
        	doctorRepository.findOneByLicenceNumber(doctorDTO.getLicenceNumber()).ifPresent(existingDoctor -> {
                boolean removed = removeNonActivatedUser(existingDoctor.getUser());
                if (!removed) {
                	throw new LicenceNumberAlreadyUsedException();
                }
            });
        }
        User newUser = new User();
        String encryptedPassword = passwordEncoder.encode(password);
        newUser.setLogin(userDTO.getLogin().toLowerCase());
        // new user gets initially a generated password
        newUser.setPassword(encryptedPassword);
        newUser.setFirstName(userDTO.getFirstName());
        newUser.setLastName(userDTO.getLastName());
        newUser.setEmail(userDTO.getEmail().toLowerCase());
        newUser.setImageUrl(userDTO.getImageUrl());
        newUser.setLangKey(userDTO.getLangKey());
        // new user is not active
        newUser.setActivated(false);
        // new user gets registration key
        newUser.setActivationKey(RandomUtil.generateActivationKey());
        Set<Authority> authorities = new HashSet<>();
        // admin user can not be created through registration
        userDTO.getAuthorities().stream()
        	.map(auth -> AuthoritiesConstants.ADMIN.equals(auth)
        				? authorityRepository.findById(AuthoritiesConstants.USER) 
        				: authorityRepository.findById(auth))
        	.forEach(a -> a.ifPresent(authorities::add));
        newUser.setAuthorities(authorities);
        userRepository.save(newUser);
        userSearchRepository.save(newUser);
        this.clearUserCaches(newUser);
        log.debug("Created Information for User: {}", newUser);
        if(userDTO.hasDoctorAuthority()) {
        	createDoctor(userDTO, newUser);
        } else if(userDTO.hasPatientAuthority()) {
        	createPatient(userDTO, newUser);
        }
        return newUser;
    }

	private void createDoctor(UserDTO userDTO, User newUser) {
		// Create and save the Doctor entity
		Doctor newDoctor = new Doctor();
		newDoctor.setPhone(userDTO.getPhone());
		newDoctor.setImage(userDTO.getImage());
		newDoctor.setImageContentType(userDTO.getImageContentType());
		newDoctor.setAddress(userDTO.getAddress());
		newDoctor.setLocation(userDTO.getLocation());
		userDTO.getDoctorDTO().ifPresent(doctorDTO -> {
			newDoctor.setType(doctorDTO.getType());
			newDoctor.setDepartment(doctorDTO.getDepartment());
			newDoctor.setLicenceNumber(doctorDTO.getLicenceNumber());
			newDoctor.setNationalId(doctorDTO.getNationalId());
			newDoctor.setPassportNo(doctorDTO.getPassportNo());
			newDoctor.setDesignation(doctorDTO.getDesignation());
			newDoctor.setDescription(doctorDTO.getDescription());
			newDoctor.setProfessionalDegrees(doctorDTO.getProfessionalDegrees());
		});
		newDoctor.setUser(newUser);
		doctorRepository.save(newDoctor);
		log.debug("Created Information for Doctor: {}", newDoctor);
	}

	private void createPatient(UserDTO userDTO, User newUser) {
		// Create and save the Patient entity
		Patient newPatient = new Patient();
		newPatient.setImage(userDTO.getImage());
		newPatient.setImageContentType(userDTO.getImageContentType());
		newPatient.setAddress(userDTO.getAddress());
		newPatient.setLocation(userDTO.getLocation());
		newPatient.setPhone(userDTO.getPhone());
		userDTO.getPatientDTO().ifPresent(patientDTO -> {
			newPatient.setBloodGroup(patientDTO.getBloodGroup());
			newPatient.setSex(patientDTO.getSex());
			newPatient.setBirthTimestamp(patientDTO.getBirthTimestamp());
			newPatient.setWeightInKG(patientDTO.getWeightInKG());
			newPatient.setHeightInInch(patientDTO.getHeightInInch());
		});
		newPatient.setUser(newUser);
		patientRepository.save(newPatient);
		log.debug("Created Information for Patient: {}", newPatient);
	}

	private boolean removeNonActivatedUser(User existingUser) {
		if (existingUser.getActivated()) {
			return false;
		}
		userRepository.delete(existingUser);
		userRepository.flush();
		this.clearUserCaches(existingUser);
		return true;
	}

//	private boolean removeNonActivatedDoctor(Doctor existingDoctor) {
//		if (existingDoctor.getUser().getActivated()) {
//			return false;
//		}
//		doctorRepository.delete(existingDoctor);
//		userRepository.delete(existingDoctor.getUser());
//		doctorRepository.flush();
//		userRepository.flush();
//		this.clearUserCaches(existingDoctor.getUser());
//		return true;
//	}
	
	public User createUser(UserDTO userDTO) {
		User user = new User();
		user.setLogin(userDTO.getLogin().toLowerCase());
		user.setFirstName(userDTO.getFirstName());
		user.setLastName(userDTO.getLastName());
		user.setEmail(userDTO.getEmail().toLowerCase());
		user.setImageUrl(userDTO.getImageUrl());
		if (userDTO.getLangKey() == null) {
			user.setLangKey(Constants.DEFAULT_LANGUAGE); // default language
		} else {
			user.setLangKey(userDTO.getLangKey());
		}
		String encryptedPassword = passwordEncoder.encode(RandomUtil.generatePassword());
		user.setPassword(encryptedPassword);
		user.setResetKey(RandomUtil.generateResetKey());
		user.setResetDate(Instant.now());
		user.setActivated(true);
		if (userDTO.getAuthorities() != null) {
			Set<Authority> authorities = userDTO.getAuthorities().stream().map(authorityRepository::findById)
					.filter(Optional::isPresent).map(Optional::get).collect(Collectors.toSet());
			user.setAuthorities(authorities);
		}
		userRepository.save(user);
		userSearchRepository.save(user);
		this.clearUserCaches(user);
		log.debug("Created Information for User: {}", user);
		return user;
	}
	
	/**
	 * Update all information for the current user.
	 * @param userDTO user to update
	 * @return updated user
	 */
	@Transactional
	public Optional<UserDTO> updateCurrentUser(UserDTO userDTO) {	
		return SecurityUtils.getCurrentUserLogin()
				.flatMap(userRepository::findOneWithAuthoritiesByLogin).map(user -> {
			this.clearUserCaches(user);
			user.setFirstName(userDTO.getFirstName());
			user.setLastName(userDTO.getLastName());
			user.setEmail(userDTO.getEmail().toLowerCase());
			user.setImageUrl(userDTO.getImageUrl());
			user.setLangKey(userDTO.getLangKey());
			userSearchRepository.save(user);
			this.clearUserCaches(user);
			log.debug(CHANGED_INFORMATION_FOR_USER, user);
			if(user.isDoctor()) {
				updateCurrentDoctor(userDTO, user);
			} else if(user.isPatient()) {
				updateCurrentPatient(userDTO, user);
			}
			return user;
		}).map(UserDTO::new);
	}
	
	private void updateCurrentDoctor(UserDTO userDTO, User user) {
		// Update doctor
		Doctor doctor = doctorRepository.findOneByUser(user).orElseGet(Doctor::new);
		doctor.setPhone(userDTO.getPhone());
		doctor.setImage(userDTO.getImage());
		doctor.setImageContentType(userDTO.getImageContentType());
		doctor.setAddress(userDTO.getAddress());
		doctor.setLocation(userDTO.getLocation());
		userDTO.getDoctorDTO().ifPresent(doctorDTO -> {
			doctor.setType(doctorDTO.getType());
			doctor.setDepartment(doctorDTO.getDepartment());
			doctor.setLicenceNumber(doctorDTO.getLicenceNumber());
			doctor.setNationalId(doctorDTO.getNationalId());
			doctor.setPassportNo(doctorDTO.getPassportNo());
			doctor.setDesignation(doctorDTO.getDesignation());
			doctor.setDescription(doctorDTO.getDescription());
			doctor.setProfessionalDegrees(doctorDTO.getProfessionalDegrees());
		});
		doctor.setUser(user);
		doctorRepository.save(doctor);
		log.debug("Created Information for Doctor: {}", doctor);
	}

	private void updateCurrentPatient(UserDTO userDTO, User user) {
		// Update patient
		Patient patient = patientRepository.findOneByUser(user).orElseGet(Patient::new);
		patient.setPhone(userDTO.getPhone());
		patient.setImage(userDTO.getImage());
		patient.setImageContentType(userDTO.getImageContentType());
		patient.setAddress(userDTO.getAddress());
		patient.setLocation(userDTO.getLocation());
		userDTO.getPatientDTO().ifPresent(patientDTO -> {
			patient.setBloodGroup(patientDTO.getBloodGroup());
			patient.setSex(patientDTO.getSex());
			patient.setBirthTimestamp(patientDTO.getBirthTimestamp());
			patient.setWeightInKG(patientDTO.getWeightInKG());
			patient.setHeightInInch(patientDTO.getHeightInInch());
		});
		patient.setUser(user);
		patientRepository.save(patient);
		log.debug("Created Information for Patient: {}", patient);
	}

	/**
	 * Update all information for a specific user, and return the modified user.
	 *
	 * @param userDTO user to update
	 * @return updated user
	 */
	public Optional<UserDTO> updateUser(UserDTO userDTO) {
		return userRepository.findById(userDTO.getId())
				.map(user -> {
					this.clearUserCaches(user);
					user.setLogin(userDTO.getLogin().toLowerCase());
					user.setFirstName(userDTO.getFirstName());
					user.setLastName(userDTO.getLastName());
					user.setEmail(userDTO.getEmail().toLowerCase());
					user.setImageUrl(userDTO.getImageUrl());
					user.setActivated(userDTO.isActivated());
					user.setLangKey(userDTO.getLangKey());
					Set<Authority> managedAuthorities = user.getAuthorities();
					managedAuthorities.clear();
					userDTO.getAuthorities().stream().map(authorityRepository::findById).filter(Optional::isPresent)
							.map(Optional::get).forEach(managedAuthorities::add);
					userSearchRepository.save(user);
					this.clearUserCaches(user);
					log.debug(CHANGED_INFORMATION_FOR_USER, user);
					return user;
				}).map(UserDTO::new);
	}

	public void deleteUser(String login) {
		userRepository.findOneByLogin(login).ifPresent(user -> {
			userRepository.delete(user);
			userSearchRepository.delete(user);
			this.clearUserCaches(user);
			log.debug("Deleted User: {}", user);
		});
	}

	public void changePassword(String currentClearTextPassword, String newPassword) {
		SecurityUtils.getCurrentUserLogin().flatMap(userRepository::findOneByLogin).ifPresent(user -> {
			String currentEncryptedPassword = user.getPassword();
			if (!passwordEncoder.matches(currentClearTextPassword, currentEncryptedPassword)) {
				throw new InvalidPasswordException();
			}
			String encryptedPassword = passwordEncoder.encode(newPassword);
			user.setPassword(encryptedPassword);
			this.clearUserCaches(user);
			log.debug("Changed password for User: {}", user);
		});
	}

	@Transactional(readOnly = true)
	public Page<UserDTO> getAllManagedUsers(Pageable pageable) {
		return userRepository.findAllByLoginNot(pageable, Constants.ANONYMOUS_USER).map(UserDTO::new);
	}

	@Transactional(readOnly = true)
	public Optional<User> getUserWithAuthoritiesByLogin(String login) {
		return userRepository.findOneWithAuthoritiesByLogin(login);
	}

	@Transactional(readOnly = true)
	public Optional<User> getUserWithAuthorities(Long id) {
		return userRepository.findOneWithAuthoritiesById(id);
	}

	@Transactional(readOnly = true)
	public Optional<User> getUserWithAuthorities() {
		return SecurityUtils.getCurrentUserLogin()
				.flatMap(userRepository::findOneWithAuthoritiesByLogin);
	}
	
	@Transactional(readOnly = true)
	public Optional<Doctor> getDoctorWithAuthoritiesAndDegrees() {
		return SecurityUtils.getCurrentUserLogin()
				.flatMap(userRepository::findOneWithAuthoritiesByLogin)
				.flatMap(user -> doctorRepository.findOneWithDegreesByUser(user).map(doctor -> {
					doctor.setUser(user);
					return doctor;
				}));
	}
	
	@Transactional(readOnly = true)
	public Optional<Patient> getPatientWithAuthorities() {
		return SecurityUtils.getCurrentUserLogin()
				.flatMap(userRepository::findOneWithAuthoritiesByLogin)
				.flatMap(user -> patientRepository.findOneByUser(user).map(patient -> {
					patient.setUser(user);
					return patient;
				}));	
	}

	/**
	 * Not activated users should be automatically deleted after 3 days.
	 * <p>
	 * This is scheduled to get fired everyday, at 01:00 (am).
	 */
	@Scheduled(cron = "0 0 1 * * ?")
	public void removeNotActivatedUsers() {
		userRepository.findAllByActivatedIsFalseAndCreatedDateBefore(Instant.now().minus(3, ChronoUnit.DAYS))
				.forEach(user -> {
					log.debug("Deleting not activated user {}", user.getLogin());
					userRepository.delete(user);
					userSearchRepository.delete(user);
					this.clearUserCaches(user);
				});
	}

	/**
	 * @return a list of all the authorities
	 */
	public List<String> getAuthorities() {
		return authorityRepository.findAll().stream().map(Authority::getName).collect(Collectors.toList());
	}

	private void clearUserCaches(User user) {
		Objects.requireNonNull(cacheManager.getCache(UserRepository.USERS_BY_LOGIN_CACHE)).evict(user.getLogin());
		Objects.requireNonNull(cacheManager.getCache(UserRepository.USERS_BY_EMAIL_CACHE)).evict(user.getEmail());
	}
}
