package com.dhomoni.uaa.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import com.dhomoni.uaa.UaaApp;
import com.dhomoni.uaa.config.Constants;
import com.dhomoni.uaa.domain.Authority;
import com.dhomoni.uaa.domain.Degree;
import com.dhomoni.uaa.domain.Doctor;
import com.dhomoni.uaa.domain.Patient;
import com.dhomoni.uaa.domain.Patient.BloodGroup;
import com.dhomoni.uaa.domain.Patient.Sex;
import com.dhomoni.uaa.domain.User;
import com.dhomoni.uaa.repository.AuthorityRepository;
import com.dhomoni.uaa.repository.DoctorRepository;
import com.dhomoni.uaa.repository.PatientRepository;
import com.dhomoni.uaa.repository.UserRepository;
import com.dhomoni.uaa.security.AuthoritiesConstants;
import com.dhomoni.uaa.service.MailService;
import com.dhomoni.uaa.service.UserService;
import com.dhomoni.uaa.service.dto.DoctorDTO;
import com.dhomoni.uaa.service.dto.PasswordChangeDTO;
import com.dhomoni.uaa.service.dto.PatientDTO;
import com.dhomoni.uaa.service.dto.UserDTO;
import com.dhomoni.uaa.web.rest.errors.ExceptionTranslator;
import com.dhomoni.uaa.web.rest.vm.KeyAndPasswordVM;
import com.dhomoni.uaa.web.rest.vm.ManagedUserVM;

/**
 * Test class for the AccountResource REST controller.
 *
 * @see AccountResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = UaaApp.class)
public class AccountResourceIntTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
	private DoctorRepository doctorRepository;

    @Autowired
	private PatientRepository patientRepository;
    
    @Autowired
    private AuthorityRepository authorityRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private HttpMessageConverter<?>[] httpMessageConverters;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Mock
    private UserService mockUserService;

    @Mock
    private MailService mockMailService;

    private MockMvc restMvc;

    private MockMvc restUserMockMvc;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        doNothing().when(mockMailService).sendActivationEmail(any());
        AccountResource accountResource =
            new AccountResource(userRepository, userService, mockMailService);

        AccountResource accountUserMockResource =
            new AccountResource(userRepository, mockUserService, mockMailService);
        this.restMvc = MockMvcBuilders.standaloneSetup(accountResource)
            .setMessageConverters(httpMessageConverters)
            .setControllerAdvice(exceptionTranslator)
            .build();
        this.restUserMockMvc = MockMvcBuilders.standaloneSetup(accountUserMockResource)
            .setControllerAdvice(exceptionTranslator)
            .build();
    }

    @Test
    public void testNonAuthenticatedUser() throws Exception {
        restUserMockMvc.perform(get("/api/authenticate")
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(""));
    }

    @Test
    public void testAuthenticatedUser() throws Exception {
        restUserMockMvc.perform(get("/api/authenticate")
            .with(request -> {
                request.setRemoteUser("test");
                return request;
            })
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string("test"));
    }

    @Test
    public void testGetExistingAccount() throws Exception {
        Set<Authority> authorities = new HashSet<>();
        Authority authority = new Authority();
        authority.setName(AuthoritiesConstants.ADMIN);
        authorities.add(authority);

        User user = new User();
        user.setLogin("test");
        user.setFirstName("john");
        user.setLastName("doe");
        user.setEmail("john.doe@jhipster.com");
        user.setImageUrl("http://placehold.it/50x50");
        user.setLangKey("en");
        user.setAuthorities(authorities);
        when(mockUserService.getUserWithAuthorities()).thenReturn(Optional.of(user));

        restUserMockMvc.perform(get("/api/account")
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.login").value("test"))
            .andExpect(jsonPath("$.firstName").value("john"))
            .andExpect(jsonPath("$.lastName").value("doe"))
            .andExpect(jsonPath("$.email").value("john.doe@jhipster.com"))
            .andExpect(jsonPath("$.imageUrl").value("http://placehold.it/50x50"))
            .andExpect(jsonPath("$.langKey").value("en"))
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.ADMIN));
    }

    @Test
    @Transactional
    @WithMockUser(username="existing-doctor-account", password="password", roles={"DOCTOR"})
    public void testGetExistingDoctorAccount() throws Exception {
        User user = new User();
        user.setLogin("existing-doctor-account");
        user.setFirstName("john");
        user.setLastName("doe");
        user.setEmail("existing-doctor-account@example.com");
        user.setPassword(RandomStringUtils.random(60));
        user.setImageUrl("http://placehold.it/50x50");
        user.setLangKey("en");
        user.setActivated(true);
        Authority auth = new Authority();
        auth.setName(AuthoritiesConstants.DOCTOR);
        user.setAuthorities(Collections.singleton(auth));
        Doctor doctor = new Doctor();
        doctor.setPhone("8888123234355");
        doctor.setType(1);
        doctor.setDepartment(1);
        doctor.setDescription("desc 1");
        doctor.setDesignation("designation 1");
        doctor.setLicenceNumber("1111122222");
        doctor.setPassportNo("2222211111");
        doctor.setNationalId("2222222222");
        doctor.setAddress("Dhanmondi");
        Path imagePath = new ClassPathResource("static/images/pervez.jpg").getFile().toPath();
        doctor.setImage(Base64.getEncoder().encode(Files.readAllBytes(imagePath)));
        doctor.setImageContentType(Files.probeContentType(imagePath));
        Degree degree = new Degree();
        degree.setName("MBBS");
        degree.setInstitute("Institute");
        degree.setCountry("Bangladesh");
        degree.setEnrollmentYear(1990);
        degree.setPassingYear(1998);
        doctor.setDegrees(Collections.singleton(degree));
        doctor.setUser(user);
        
        when(mockUserService.getDoctorWithAuthoritiesAndDegrees()).thenReturn(Optional.of(doctor));

        restUserMockMvc.perform(get("/api/account")
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.login").value(user.getLogin()))
            .andExpect(jsonPath("$.firstName").value(user.getFirstName()))
            .andExpect(jsonPath("$.lastName").value(user.getLastName()))
            .andExpect(jsonPath("$.email").value(user.getEmail()))
            .andExpect(jsonPath("$.imageUrl").value(user.getImageUrl()))
            .andExpect(jsonPath("$.langKey").value(user.getLangKey()))
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.DOCTOR))
            .andExpect(jsonPath("$.phone").value(doctor.getPhone()))
            .andExpect(jsonPath("$.address").value(doctor.getAddress()))
            .andExpect(jsonPath("$.image").exists())
            .andExpect(jsonPath("$.imageContentType").value(doctor.getImageContentType()))
            .andExpect(jsonPath("$.doctorDTO.type").value(doctor.getType()))
            .andExpect(jsonPath("$.doctorDTO.department").value(doctor.getDepartment()))
            .andExpect(jsonPath("$.doctorDTO.description").value(doctor.getDescription()))
            .andExpect(jsonPath("$.doctorDTO.designation").value(doctor.getDesignation()))
            .andExpect(jsonPath("$.doctorDTO.licenceNumber").value(doctor.getLicenceNumber()))
            .andExpect(jsonPath("$.doctorDTO.passportNo").value(doctor.getPassportNo()))
            .andExpect(jsonPath("$.doctorDTO.nationalId").value(doctor.getNationalId()))
            .andExpect(jsonPath("$.doctorDTO.degrees[0].name").value(degree.getName()))
            .andExpect(jsonPath("$.doctorDTO.degrees[0].institute").value(degree.getInstitute()))
            .andExpect(jsonPath("$.doctorDTO.degrees[0].country").value(degree.getCountry()))
            .andExpect(jsonPath("$.doctorDTO.degrees[0].enrollmentYear").value(degree.getEnrollmentYear()))
            .andExpect(jsonPath("$.doctorDTO.degrees[0].passingYear").value(degree.getPassingYear()));
    }
    
    @Test
    @Transactional
    @WithMockUser(username="existing-patient-account", password="password", roles={"USER"})
    public void testGetExistingPatientAccount() throws Exception {
        Set<Authority> authorities = new HashSet<>();
        Authority authority = new Authority();
        authority.setName(AuthoritiesConstants.USER);
        authorities.add(authority);
        User user = new User();
        user.setLogin("existing-patient-account");
        user.setFirstName("john");
        user.setLastName("doe");
        user.setEmail("existing-patient-account@jhipster.com");
        user.setImageUrl("http://placehold.it/50x50");
        user.setLangKey("en");
        user.setAuthorities(authorities);
        Patient patient = new Patient();
        patient.setPhone("8888123234355");
        String timestamp = "2016-02-16 11:00:02";
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
        Instant birthTimestamp = Instant.from(formatter.parse(timestamp));
        patient.setBirthTimestamp(birthTimestamp);
        patient.setBloodGroup(BloodGroup.A_POSITIVE);
        patient.setHeightInInch(70.0);
        patient.setSex(Sex.MALE);
        patient.setWeightInKG(70.0);
        patient.setAddress("Dhanmondi");
        Path imagePath = new ClassPathResource("static/images/pervez.jpg").getFile().toPath();
        patient.setImage(Base64.getEncoder().encode(Files.readAllBytes(imagePath)));
        patient.setImageContentType(Files.probeContentType(imagePath));
        patient.setUser(user);
        
        when(mockUserService.getPatientWithAuthorities()).thenReturn(Optional.of(patient));

        restUserMockMvc.perform(get("/api/account")
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.login").value(user.getLogin()))
            .andExpect(jsonPath("$.firstName").value(user.getFirstName()))
            .andExpect(jsonPath("$.lastName").value(user.getLastName()))
            .andExpect(jsonPath("$.email").value(user.getEmail()))
            .andExpect(jsonPath("$.imageUrl").value(user.getImageUrl()))
            .andExpect(jsonPath("$.langKey").value(user.getLangKey()))
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.USER))
            .andExpect(jsonPath("$.phone").value(patient.getPhone()))
            .andExpect(jsonPath("$.address").value(patient.getAddress()))
            .andExpect(jsonPath("$.image").exists())
            .andExpect(jsonPath("$.imageContentType").value(patient.getImageContentType()))
            .andExpect(jsonPath("$.patientDTO.birthTimestamp").value(patient.getBirthTimestamp().getEpochSecond()))
            .andExpect(jsonPath("$.patientDTO.bloodGroup").value(patient.getBloodGroup().name()))
            .andExpect(jsonPath("$.patientDTO.heightInInch").value(patient.getHeightInInch()))
            .andExpect(jsonPath("$.patientDTO.sex").value(patient.getSex().toString()))
            .andExpect(jsonPath("$.patientDTO.weightInKG").value(patient.getWeightInKG()));
    }
    
    @Test
    public void testGetUnknownAccount() throws Exception {
        when(mockUserService.getUserWithAuthorities()).thenReturn(Optional.empty());

        restUserMockMvc.perform(get("/api/account")
            .accept(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(status().isInternalServerError());
    }

    @Test
    @Transactional
    public void testRegisterValid() throws Exception {
        ManagedUserVM validUser = new ManagedUserVM();
        validUser.setLogin("test-register-valid");
        validUser.setPassword("password");
        validUser.setFirstName("Alice");
        validUser.setLastName("Test");
        validUser.setEmail("test-register-valid@example.com");
        validUser.setImageUrl("http://placehold.it/50x50");
        validUser.setLangKey(Constants.DEFAULT_LANGUAGE);
        validUser.setAuthorities(Collections.singleton(AuthoritiesConstants.USER));
        assertThat(userRepository.findOneByLogin("test-register-valid").isPresent()).isFalse();

        restMvc.perform(
            post("/api/register")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andExpect(status().isCreated());

        assertThat(userRepository.findOneByLogin("test-register-valid").isPresent()).isTrue();
    }

    // h2 geodb spatial extension already moved towards JTS 1.6.0 version supported by locationtech 
    // whereas hibernate spatial that comes with spring boot jpa starter still using
    // old jTS version 1.4.0 supported by vividsolutions. So for now I am
    // ignoring to test location data against H2/GeoDB.
    // Link: https://hibernate.atlassian.net/browse/HHH-12144
    @Test
    @Transactional
    public void testDoctorRegisterValid() throws Exception {
        ManagedUserVM validDoctor = new ManagedUserVM();
        validDoctor.setLogin("test-doctor-register-valid");
        validDoctor.setPassword("password");
        validDoctor.setFirstName("Pervez");
        validDoctor.setLastName("Sajjad");
        validDoctor.setEmail("test-doctor-register-valid@example.com");
        validDoctor.setImageUrl("http://placehold.it/50x50");
        validDoctor.setLangKey(Constants.DEFAULT_LANGUAGE);
        validDoctor.setAuthorities(Collections.singleton(AuthoritiesConstants.DOCTOR));
        validDoctor.setPhone("8888123234355");
        validDoctor.setAddress("Dhanmondi");
//        GeometryFactory gf=new GeometryFactory();
//        Point point=gf.createPoint(new Coordinate(90.4125, 23.8103));
//        validDoctor.setLocation(point);
        Path imagePath = new ClassPathResource("static/images/pervez.jpg").getFile().toPath();
        validDoctor.setImage(Base64.getEncoder().encode(Files.readAllBytes(imagePath)));
        validDoctor.setImageContentType(Files.probeContentType(imagePath));
        DoctorDTO doctorDTO = new DoctorDTO();        
        doctorDTO.setType(1);
        doctorDTO.setDepartment(1);
        doctorDTO.setDescription("Desc");
        doctorDTO.setDesignation("doctor designation");
        doctorDTO.setLicenceNumber("434243434155");
        doctorDTO.setNationalId("4545646456234");
        doctorDTO.setPassportNo("89787655673423");
        Degree degree = new Degree();
        degree.setName("MBBS");
        degree.setInstitute("Institute");
        degree.setCountry("Bangladesh");
        degree.setEnrollmentYear(1990);
        degree.setPassingYear(1998);
        doctorDTO.setDegrees(Collections.singleton(degree));
        validDoctor.setDoctorDTO(doctorDTO);
        assertThat(userRepository.findOneByLogin("test-doctor-register-valid").isPresent()).isFalse();

        restMvc.perform(
            post("/api/register")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validDoctor)))
            .andExpect(status().isCreated());

        Optional<User> newUser = userRepository.findOneByLogin("test-doctor-register-valid");
        assertThat(newUser.isPresent()).isTrue();
        Optional<Doctor> newDoctor = doctorRepository.findOneByUser(newUser.get());
        assertThat(newDoctor.isPresent()).isTrue();
        assertThat(newDoctor.get().getDegrees().size()).isEqualTo(1);
    }
    
    @Test
    @Transactional
    public void testPatientRegisterValid() throws Exception {
        ManagedUserVM validPatient = new ManagedUserVM();
        validPatient.setLogin("test-patient-register-valid");
        validPatient.setPassword("password");
        validPatient.setFirstName("Arhan");
        validPatient.setLastName("Sajjad");
        validPatient.setEmail("test-patient-register-valid@example.com");
        validPatient.setImageUrl("http://placehold.it/50x50");
        validPatient.setLangKey(Constants.DEFAULT_LANGUAGE);
        validPatient.setAuthorities(Collections.singleton(AuthoritiesConstants.USER));
        validPatient.setPhone("8888123234355");
        validPatient.setAddress("Dhanmondi");
        Path imagePath = new ClassPathResource("static/images/pervez.jpg").getFile().toPath();
        validPatient.setImage(Base64.getEncoder().encode(Files.readAllBytes(imagePath)));
        validPatient.setImageContentType(Files.probeContentType(imagePath));
        PatientDTO patientDTO = new PatientDTO();
        String timestamp = "2016-02-16 11:00:02";
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
        Instant birthTimestamp = Instant.from(formatter.parse(timestamp));
        patientDTO.setBirthTimestamp(birthTimestamp);
        patientDTO.setBloodGroup(BloodGroup.A_POSITIVE);
        patientDTO.setHeightInInch(70.0);
        patientDTO.setSex(Sex.MALE);
        patientDTO.setWeightInKG(70.0);
        validPatient.setPatientDTO(patientDTO);
        assertThat(userRepository.findOneByLogin("test-patient-register-valid").isPresent()).isFalse();

        restMvc.perform(
            post("/api/register")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validPatient)))
            .andExpect(status().isCreated());

        assertThat(userRepository.findOneByLogin("test-patient-register-valid").isPresent()).isTrue();
    }
    
    @Test
    @Transactional
    public void testRegisterInvalidLogin() throws Exception {
        ManagedUserVM invalidUser = new ManagedUserVM();
        invalidUser.setLogin("funky-log!n");// <-- invalid
        invalidUser.setPassword("password");
        invalidUser.setFirstName("Funky");
        invalidUser.setLastName("One");
        invalidUser.setEmail("funky@example.com");
        invalidUser.setActivated(true);
        invalidUser.setImageUrl("http://placehold.it/50x50");
        invalidUser.setLangKey(Constants.DEFAULT_LANGUAGE);
        invalidUser.setAuthorities(Collections.singleton(AuthoritiesConstants.USER));

        restUserMockMvc.perform(
            post("/api/register")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(invalidUser)))
            .andExpect(status().isBadRequest());

        Optional<User> user = userRepository.findOneByEmailIgnoreCase("funky@example.com");
        assertThat(user.isPresent()).isFalse();
    }

    @Test
    @Transactional
    public void testRegisterInvalidEmail() throws Exception {
        ManagedUserVM invalidUser = new ManagedUserVM();
        invalidUser.setLogin("bob");
        invalidUser.setPassword("password");
        invalidUser.setFirstName("Bob");
        invalidUser.setLastName("Green");
        invalidUser.setEmail("invalid");// <-- invalid
        invalidUser.setActivated(true);
        invalidUser.setImageUrl("http://placehold.it/50x50");
        invalidUser.setLangKey(Constants.DEFAULT_LANGUAGE);
        invalidUser.setAuthorities(Collections.singleton(AuthoritiesConstants.USER));

        restUserMockMvc.perform(
            post("/api/register")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(invalidUser)))
            .andExpect(status().isBadRequest());

        Optional<User> user = userRepository.findOneByLogin("bob");
        assertThat(user.isPresent()).isFalse();
    }

    @Test
    @Transactional
    public void testRegisterInvalidPassword() throws Exception {
        ManagedUserVM invalidUser = new ManagedUserVM();
        invalidUser.setLogin("bob");
        invalidUser.setPassword("123");// password with only 3 digits
        invalidUser.setFirstName("Bob");
        invalidUser.setLastName("Green");
        invalidUser.setEmail("bob@example.com");
        invalidUser.setActivated(true);
        invalidUser.setImageUrl("http://placehold.it/50x50");
        invalidUser.setLangKey(Constants.DEFAULT_LANGUAGE);
        invalidUser.setAuthorities(Collections.singleton(AuthoritiesConstants.USER));

        restUserMockMvc.perform(
            post("/api/register")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(invalidUser)))
            .andExpect(status().isBadRequest());

        Optional<User> user = userRepository.findOneByLogin("bob");
        assertThat(user.isPresent()).isFalse();
    }
    
    @Test
    @Transactional
    public void testDoctorRegisterInvalidLicenceNumber() throws Exception {
        ManagedUserVM invalidDoctor = new ManagedUserVM();
        invalidDoctor.setLogin("pervez");
        invalidDoctor.setPassword("password");
        invalidDoctor.setFirstName("Pervez");
        invalidDoctor.setLastName("Sajjad");
        invalidDoctor.setEmail("pervez@example.com");
        invalidDoctor.setImageUrl("http://placehold.it/50x50");
        invalidDoctor.setLangKey(Constants.DEFAULT_LANGUAGE);
        invalidDoctor.setAuthorities(Collections.singleton(AuthoritiesConstants.DOCTOR));
        invalidDoctor.setPhone("11112222345");
        invalidDoctor.setAddress("Dhanmondi");
        Path imagePath = new ClassPathResource("static/images/pervez.jpg").getFile().toPath();
        invalidDoctor.setImage(Base64.getEncoder().encode(Files.readAllBytes(imagePath)));
        invalidDoctor.setImageContentType(Files.probeContentType(imagePath));
        DoctorDTO doctorDTO = new DoctorDTO();        
        doctorDTO.setType(1);
        doctorDTO.setDepartment(1);
        doctorDTO.setDescription("Desc");
        doctorDTO.setDesignation("doctor designation");
        doctorDTO.setLicenceNumber("qwe");  // invalid licence number
        doctorDTO.setNationalId("4545646456234");
        doctorDTO.setPassportNo("89787655673423");
        invalidDoctor.setDoctorDTO(doctorDTO);
        assertThat(userRepository.findOneByLogin("test-doctor-register-valid").isPresent()).isFalse();

        restMvc.perform(
            post("/api/register")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(invalidDoctor)))
            .andExpect(status().isBadRequest());

        assertThat(userRepository.findOneByLogin("pervez").isPresent()).isFalse();
    }

    @Test
    @Transactional
    public void testRegisterNullPassword() throws Exception {
        ManagedUserVM invalidUser = new ManagedUserVM();
        invalidUser.setLogin("bob");
        invalidUser.setPassword(null);// invalid null password
        invalidUser.setFirstName("Bob");
        invalidUser.setLastName("Green");
        invalidUser.setEmail("bob@example.com");
        invalidUser.setActivated(true);
        invalidUser.setImageUrl("http://placehold.it/50x50");
        invalidUser.setLangKey(Constants.DEFAULT_LANGUAGE);
        invalidUser.setAuthorities(Collections.singleton(AuthoritiesConstants.USER));

        restUserMockMvc.perform(
            post("/api/register")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(invalidUser)))
            .andExpect(status().isBadRequest());

        Optional<User> user = userRepository.findOneByLogin("bob");
        assertThat(user.isPresent()).isFalse();
    }
    
    @Test
    @Transactional
    public void testDoctorRegisterNullLicenceNumber() throws Exception {
        ManagedUserVM invalidDoctor = new ManagedUserVM();
        invalidDoctor.setLogin("pervez");
        invalidDoctor.setPassword("password");
        invalidDoctor.setFirstName("Pervez");
        invalidDoctor.setLastName("Sajjad");
        invalidDoctor.setEmail("pervez@example.com");
        invalidDoctor.setImageUrl("http://placehold.it/50x50");
        invalidDoctor.setLangKey(Constants.DEFAULT_LANGUAGE);
        invalidDoctor.setAuthorities(Collections.singleton(AuthoritiesConstants.DOCTOR));
        invalidDoctor.setPhone("11112222345");
        invalidDoctor.setAddress("Dhanmondi");
        Path imagePath = new ClassPathResource("static/images/pervez.jpg").getFile().toPath();
        invalidDoctor.setImage(Base64.getEncoder().encode(Files.readAllBytes(imagePath)));
        invalidDoctor.setImageContentType(Files.probeContentType(imagePath));
        DoctorDTO doctorDTO = new DoctorDTO();        
        doctorDTO.setType(1);
        doctorDTO.setDepartment(1);
        doctorDTO.setDescription("Desc");
        doctorDTO.setDesignation("doctor designation");
        doctorDTO.setLicenceNumber(null);  // null licence number
        doctorDTO.setNationalId("4545646456234");
        doctorDTO.setPassportNo("89787655673423");
        invalidDoctor.setDoctorDTO(doctorDTO);
        assertThat(userRepository.findOneByLogin("test-doctor-register-valid").isPresent()).isFalse();

        restMvc.perform(
            post("/api/register")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(invalidDoctor)))
            .andExpect(status().isBadRequest());

        assertThat(userRepository.findOneByLogin("pervez").isPresent()).isFalse();
    }
    
    @Test
    @Transactional
    public void testDoctorRegisterWithoutData() throws Exception {
        ManagedUserVM invalidDoctor = new ManagedUserVM();
        invalidDoctor.setLogin("pervez_invalid");
        invalidDoctor.setPassword("password");
        invalidDoctor.setFirstName("Pervez");
        invalidDoctor.setLastName("Sajjad");
        invalidDoctor.setEmail("pervez_invalid@example.com");
        invalidDoctor.setImageUrl("http://placehold.it/50x50");
        invalidDoctor.setLangKey(Constants.DEFAULT_LANGUAGE);
        invalidDoctor.setAuthorities(Collections.singleton(AuthoritiesConstants.DOCTOR));
        invalidDoctor.setPhone("11112222345");
        invalidDoctor.setAddress("Dhanmondi");
        Path imagePath = new ClassPathResource("static/images/pervez.jpg").getFile().toPath();
        invalidDoctor.setImage(Base64.getEncoder().encode(Files.readAllBytes(imagePath)));
        invalidDoctor.setImageContentType(Files.probeContentType(imagePath));
        assertThat(userRepository.findOneByLogin("test-doctor-register-valid").isPresent()).isFalse();

        restMvc.perform(
            post("/api/register")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(invalidDoctor)))
            .andExpect(status().isBadRequest());

        assertThat(userRepository.findOneByLogin("pervez_invalid").isPresent()).isFalse();
    }

    @Test
    @Transactional
    public void testRegisterDuplicateLogin() throws Exception {
        // First registration
        ManagedUserVM firstUser = new ManagedUserVM();
        firstUser.setLogin("alice");
        firstUser.setPassword("password");
        firstUser.setFirstName("Alice");
        firstUser.setLastName("Something");
        firstUser.setEmail("alice@example.com");
        firstUser.setImageUrl("http://placehold.it/50x50");
        firstUser.setLangKey(Constants.DEFAULT_LANGUAGE);
        firstUser.setAuthorities(Collections.singleton(AuthoritiesConstants.USER));

        // Duplicate login, different email
        ManagedUserVM secondUser = new ManagedUserVM();
        secondUser.setLogin(firstUser.getLogin());
        secondUser.setPassword(firstUser.getPassword());
        secondUser.setFirstName(firstUser.getFirstName());
        secondUser.setLastName(firstUser.getLastName());
        secondUser.setEmail("alice2@example.com");
        secondUser.setImageUrl(firstUser.getImageUrl());
        secondUser.setLangKey(firstUser.getLangKey());
        secondUser.setCreatedBy(firstUser.getCreatedBy());
        secondUser.setCreatedDate(firstUser.getCreatedDate());
        secondUser.setLastModifiedBy(firstUser.getLastModifiedBy());
        secondUser.setLastModifiedDate(firstUser.getLastModifiedDate());
        secondUser.setAuthorities(new HashSet<>(firstUser.getAuthorities()));

        // First user
        restMvc.perform(
            post("/api/register")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(firstUser)))
            .andExpect(status().isCreated());

        // Second (non activated) user
        restMvc.perform(
            post("/api/register")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(secondUser)))
            .andExpect(status().isCreated());

        Optional<User> testUser = userRepository.findOneByEmailIgnoreCase("alice2@example.com");
        assertThat(testUser.isPresent()).isTrue();
        testUser.get().setActivated(true);
        userRepository.save(testUser.get());

        // Second (already activated) user
        restMvc.perform(
            post("/api/register")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(secondUser)))
            .andExpect(status().is4xxClientError());
    }

    @Test
    @Transactional
    public void testRegisterDuplicateEmail() throws Exception {
        // First user
        ManagedUserVM firstUser = new ManagedUserVM();
        firstUser.setLogin("test-register-duplicate-email");
        firstUser.setPassword("password");
        firstUser.setFirstName("Alice");
        firstUser.setLastName("Test");
        firstUser.setEmail("test-register-duplicate-email@example.com");
        firstUser.setImageUrl("http://placehold.it/50x50");
        firstUser.setLangKey(Constants.DEFAULT_LANGUAGE);
        firstUser.setAuthorities(Collections.singleton(AuthoritiesConstants.USER));

        // Register first user
        restMvc.perform(
            post("/api/register")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(firstUser)))
            .andExpect(status().isCreated());

        Optional<User> testUser1 = userRepository.findOneByLogin("test-register-duplicate-email");
        assertThat(testUser1.isPresent()).isTrue();

        // Duplicate email, different login
        ManagedUserVM secondUser = new ManagedUserVM();
        secondUser.setLogin("test-register-duplicate-email-2");
        secondUser.setPassword(firstUser.getPassword());
        secondUser.setFirstName(firstUser.getFirstName());
        secondUser.setLastName(firstUser.getLastName());
        secondUser.setEmail(firstUser.getEmail());
        secondUser.setImageUrl(firstUser.getImageUrl());
        secondUser.setLangKey(firstUser.getLangKey());
        secondUser.setAuthorities(new HashSet<>(firstUser.getAuthorities()));

        // Register second (non activated) user
        restMvc.perform(
            post("/api/register")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(secondUser)))
            .andExpect(status().isCreated());

        Optional<User> testUser2 = userRepository.findOneByLogin("test-register-duplicate-email");
        assertThat(testUser2.isPresent()).isFalse();

        Optional<User> testUser3 = userRepository.findOneByLogin("test-register-duplicate-email-2");
        assertThat(testUser3.isPresent()).isTrue();

        // Duplicate email - with uppercase email address
        ManagedUserVM userWithUpperCaseEmail = new ManagedUserVM();
        userWithUpperCaseEmail.setId(firstUser.getId());
        userWithUpperCaseEmail.setLogin("test-register-duplicate-email-3");
        userWithUpperCaseEmail.setPassword(firstUser.getPassword());
        userWithUpperCaseEmail.setFirstName(firstUser.getFirstName());
        userWithUpperCaseEmail.setLastName(firstUser.getLastName());
        userWithUpperCaseEmail.setEmail("TEST-register-duplicate-email@example.com");
        userWithUpperCaseEmail.setImageUrl(firstUser.getImageUrl());
        userWithUpperCaseEmail.setLangKey(firstUser.getLangKey());
        userWithUpperCaseEmail.setAuthorities(new HashSet<>(firstUser.getAuthorities()));

        // Register third (not activated) user
        restMvc.perform(
            post("/api/register")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(userWithUpperCaseEmail)))
            .andExpect(status().isCreated());

        Optional<User> testUser4 = userRepository.findOneByLogin("test-register-duplicate-email-3");
        assertThat(testUser4.isPresent()).isTrue();
        assertThat(testUser4.get().getEmail()).isEqualTo("test-register-duplicate-email@example.com");

        testUser4.get().setActivated(true);
        userService.updateUser((new UserDTO(testUser4.get())));

        // Register 4th (already activated) user
        restMvc.perform(
            post("/api/register")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(secondUser)))
            .andExpect(status().is4xxClientError());
    }
    
    @Test
    @Transactional
    public void testDoctorRegisterDuplicateLicenceNumber() throws Exception {
    	// First doctor
        ManagedUserVM firstDoctor = new ManagedUserVM();
        firstDoctor.setLogin("test-duplicate-licence-number");
        firstDoctor.setPassword("password");
        firstDoctor.setFirstName("Pervez");
        firstDoctor.setLastName("Sajjad");
        firstDoctor.setEmail("test-duplicate-licence-number@example.com");
        firstDoctor.setImageUrl("http://placehold.it/50x50");
        firstDoctor.setLangKey(Constants.DEFAULT_LANGUAGE);
        firstDoctor.setAuthorities(Collections.singleton(AuthoritiesConstants.DOCTOR));
        firstDoctor.setPhone("11112222345");
        firstDoctor.setAddress("Dhanmondi");
        Path imagePath = new ClassPathResource("static/images/pervez.jpg").getFile().toPath();
        firstDoctor.setImage(Base64.getEncoder().encode(Files.readAllBytes(imagePath)));
        firstDoctor.setImageContentType(Files.probeContentType(imagePath));
        DoctorDTO doctorDTO = new DoctorDTO();        
        doctorDTO.setType(1);
        doctorDTO.setDepartment(1);
        doctorDTO.setDescription("Desc");
        doctorDTO.setDesignation("doctor designation");
        doctorDTO.setLicenceNumber("434243434155");
        doctorDTO.setNationalId("4545646456234");
        doctorDTO.setPassportNo("89787655673423");
        firstDoctor.setDoctorDTO(doctorDTO);
        assertThat(userRepository.findOneByLogin("test-duplicate-licence-number").isPresent()).isFalse();

        // Register first doctor
        restMvc.perform(
            post("/api/register")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(firstDoctor)))
            .andExpect(status().isCreated());
        
        Optional<User> testUser1 = userRepository.findOneByLogin("test-duplicate-licence-number");
        assertThat(testUser1.isPresent()).isTrue();
        
        // Duplicate license number, different login
        ManagedUserVM secondDoctor = new ManagedUserVM();
        secondDoctor.setLogin("test-duplicate-licence-number-2");
        secondDoctor.setPassword(firstDoctor.getPassword());
        secondDoctor.setFirstName(firstDoctor.getFirstName());
        secondDoctor.setLastName(firstDoctor.getLastName());
        secondDoctor.setEmail("test-duplicate-licence-number-2@example.com");
        secondDoctor.setImageUrl(firstDoctor.getImageUrl());
        secondDoctor.setLangKey(firstDoctor.getLangKey());
        secondDoctor.setCreatedBy(firstDoctor.getCreatedBy());
        secondDoctor.setCreatedDate(firstDoctor.getCreatedDate());
        secondDoctor.setLastModifiedBy(firstDoctor.getLastModifiedBy());
        secondDoctor.setLastModifiedDate(firstDoctor.getLastModifiedDate());
        secondDoctor.setAuthorities(new HashSet<>(firstDoctor.getAuthorities()));
        secondDoctor.setAddress(firstDoctor.getAddress());
        secondDoctor.setImage(firstDoctor.getImage());
        secondDoctor.setImageContentType(firstDoctor.getImageContentType());
        secondDoctor.setDoctorDTO(doctorDTO);
        
        // Register second (non activated) user
        restMvc.perform(
            post("/api/register")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(secondDoctor)))
            .andExpect(status().isCreated());

        testUser1 = userRepository.findOneByLogin("test-duplicate-licence-number");
        assertThat(testUser1.isPresent()).isFalse();

        Optional<User> testUser2 = userRepository.findOneByLogin("test-duplicate-licence-number-2");
        assertThat(testUser2.isPresent()).isTrue();

        testUser2.get().setActivated(true);
        userService.updateUser((new UserDTO(testUser2.get())));
        
        // Register 3rd (already activated) user
        restMvc.perform(
            post("/api/register")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(firstDoctor)))
            .andExpect(status().is4xxClientError());
    }

    @Test
    @Transactional
    public void testRegisterAdminIsIgnored() throws Exception {
        ManagedUserVM validUser = new ManagedUserVM();
        validUser.setLogin("badguy");
        validUser.setPassword("password");
        validUser.setFirstName("Bad");
        validUser.setLastName("Guy");
        validUser.setEmail("badguy@example.com");
        validUser.setActivated(true);
        validUser.setImageUrl("http://placehold.it/50x50");
        validUser.setLangKey(Constants.DEFAULT_LANGUAGE);
        validUser.setAuthorities(Collections.singleton(AuthoritiesConstants.ADMIN));

        restMvc.perform(
            post("/api/register")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andExpect(status().isCreated());

        Optional<User> userDup = userRepository.findOneByLogin("badguy");
        assertThat(userDup.isPresent()).isTrue();
        assertThat(userDup.get().getAuthorities()).hasSize(1)
            .containsExactly(authorityRepository.findById(AuthoritiesConstants.USER).get());
    }

    @Test
    @Transactional
    public void testActivateAccount() throws Exception {
        final String activationKey = "some activation key";
        User user = new User();
        user.setLogin("activate-account");
        user.setEmail("activate-account@example.com");
        user.setPassword(RandomStringUtils.random(60));
        user.setActivated(false);
        user.setActivationKey(activationKey);

        userRepository.saveAndFlush(user);

        restMvc.perform(get("/api/activate?key={activationKey}", activationKey))
            .andExpect(status().isOk());

        user = userRepository.findOneByLogin(user.getLogin()).orElse(null);
        assertThat(user.getActivated()).isTrue();
    }

    @Test
    @Transactional
    public void testActivateAccountWithWrongKey() throws Exception {
        restMvc.perform(get("/api/activate?key=wrongActivationKey"))
            .andExpect(status().isInternalServerError());
    }

    @Test
    @Transactional
    @WithMockUser("save-account")
    public void testSaveAccount() throws Exception {
        User user = new User();
        user.setLogin("save-account");
        user.setEmail("save-account@example.com");
        user.setPassword(RandomStringUtils.random(60));
        user.setActivated(true);

        userRepository.saveAndFlush(user);

        UserDTO userDTO = new UserDTO();
        userDTO.setLogin("not-used");
        userDTO.setFirstName("firstname");
        userDTO.setLastName("lastname");
        userDTO.setEmail("save-account@example.com");
        userDTO.setActivated(false);
        userDTO.setImageUrl("http://placehold.it/50x50");
        userDTO.setLangKey(Constants.DEFAULT_LANGUAGE);
        userDTO.setAuthorities(Collections.singleton(AuthoritiesConstants.ADMIN));

        restMvc.perform(
            post("/api/account")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(userDTO)))
            .andExpect(status().isOk());

        User updatedUser = userRepository.findOneByLogin(user.getLogin()).orElse(null);
        assertThat(updatedUser.getFirstName()).isEqualTo(userDTO.getFirstName());
        assertThat(updatedUser.getLastName()).isEqualTo(userDTO.getLastName());
        assertThat(updatedUser.getEmail()).isEqualTo(userDTO.getEmail());
        assertThat(updatedUser.getLangKey()).isEqualTo(userDTO.getLangKey());
        assertThat(updatedUser.getPassword()).isEqualTo(user.getPassword());
        assertThat(updatedUser.getImageUrl()).isEqualTo(userDTO.getImageUrl());
        assertThat(updatedUser.getActivated()).isEqualTo(true);
        assertThat(updatedUser.getAuthorities()).isEmpty();
    }
    
    @Test
    @Transactional
    @WithMockUser("save-doctor-account")
    public void testSaveDoctorAccount() throws Exception {
    	
        User user = new User();
        user.setLogin("save-doctor-account");
        user.setEmail("save-doctor-account@example.com");
        user.setPassword(RandomStringUtils.random(60));
        user.setActivated(true);
        Authority auth = new Authority();
        auth.setName(AuthoritiesConstants.DOCTOR);
        user.setAuthorities(Collections.singleton(auth));

        User newUser = userRepository.saveAndFlush(user);
    	
        ManagedUserVM doctor = new ManagedUserVM();
        doctor.setLogin("save-doctor-account");
        doctor.setPassword("password");
        doctor.setFirstName("Pervez");
        doctor.setLastName("Sajjad");
        doctor.setEmail("save-doctor-account@example.com");
        doctor.setImageUrl("http://placehold.it/50x50");
        doctor.setLangKey(Constants.DEFAULT_LANGUAGE);
        doctor.setAuthorities(Collections.singleton(AuthoritiesConstants.USER));   // trying with different roles which should be ignored.
        doctor.setPhone("11112222345");
        doctor.setAddress("Dhanmondi");
        Path imagePath = new ClassPathResource("static/images/pervez.jpg").getFile().toPath();
        doctor.setImage(Base64.getEncoder().encode(Files.readAllBytes(imagePath)));
        doctor.setImageContentType(Files.probeContentType(imagePath));
        DoctorDTO doctorDTO = new DoctorDTO();        
        doctorDTO.setType(1);
        doctorDTO.setDepartment(1);
        doctorDTO.setDescription("Desc");
        doctorDTO.setDesignation("doctor designation");
        doctorDTO.setLicenceNumber("434243434155");
        doctorDTO.setNationalId("4545646456234");
        doctorDTO.setPassportNo("89787655673423");
        Degree degree = new Degree();
        degree.setName("MBBS");
        degree.setInstitute("Institute");
        degree.setCountry("Bangladesh");
        degree.setEnrollmentYear(1990);
        degree.setPassingYear(1998);
        doctorDTO.setDegrees(Collections.singleton(degree));
        doctor.setDoctorDTO(doctorDTO);

        restMvc.perform(
            post("/api/account")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(doctor)))
            .andExpect(status().isOk());

        Doctor updatedDoctor = doctorRepository.findOneByUser(newUser).orElse(null);
        User updatedUser = updatedDoctor.getUser();
        assertThat(updatedUser.getFirstName()).isEqualTo(doctor.getFirstName());
        assertThat(updatedUser.getLastName()).isEqualTo(doctor.getLastName());
        assertThat(updatedUser.getEmail()).isEqualTo(doctor.getEmail());
        assertThat(updatedUser.getLangKey()).isEqualTo(doctor.getLangKey());
        assertThat(updatedUser.getPassword()).isEqualTo(user.getPassword());
        assertThat(updatedUser.getImageUrl()).isEqualTo(doctor.getImageUrl());
        assertThat(updatedUser.getActivated()).isEqualTo(true);
        assertThat(updatedDoctor.getPhone()).isEqualTo(doctor.getPhone());
        assertThat(updatedDoctor.getAddress()).isEqualTo(doctor.getAddress());
        assertThat(updatedDoctor.getImage()).isEqualTo(doctor.getImage());
        assertThat(updatedDoctor.getImageContentType()).isEqualTo(doctor.getImageContentType());
        assertThat(updatedDoctor.getType()).isEqualTo(doctorDTO.getType());
        assertThat(updatedDoctor.getDepartment()).isEqualTo(doctorDTO.getDepartment());
        assertThat(updatedDoctor.getDescription()).isEqualTo(doctorDTO.getDescription());
        assertThat(updatedDoctor.getDesignation()).isEqualTo(doctorDTO.getDesignation());
        assertThat(updatedDoctor.getLicenceNumber()).isEqualTo(doctorDTO.getLicenceNumber());
        assertThat(updatedDoctor.getNationalId()).isEqualTo(doctorDTO.getNationalId());
        assertThat(updatedDoctor.getDegrees().size()).isEqualTo(doctorDTO.getDegrees().size());
        assertThat(updatedDoctor.getPassportNo()).isEqualTo(doctorDTO.getPassportNo());
        assertThat(updatedUser.getAuthorities()).isEqualTo(user.getAuthorities());
    }

    @Test
    @Transactional
    @WithMockUser("save-patient-account")
    public void testSavePatientAccount() throws Exception {
    	
        User user = new User();
        user.setLogin("save-patient-account");
        user.setEmail("save-patient-account@example.com");
        user.setPassword(RandomStringUtils.random(60));
        user.setActivated(true);
        Authority auth = new Authority();
        auth.setName(AuthoritiesConstants.USER);
        user.setAuthorities(Collections.singleton(auth));

        User newUser = userRepository.saveAndFlush(user);
    	
        ManagedUserVM patient = new ManagedUserVM();
        patient.setLogin("save-patient-account");
        patient.setPassword("password");
        patient.setFirstName("Pervez");
        patient.setLastName("Sajjad");
        patient.setEmail("save-patient-account@example.com");
        patient.setImageUrl("http://placehold.it/50x50");
        patient.setLangKey(Constants.DEFAULT_LANGUAGE);
        patient.setAuthorities(Collections.singleton(AuthoritiesConstants.ADMIN));   // trying with different roles which should be ignored.
        patient.setPhone("11112222345");
        patient.setAddress("Dhanmondi");
        Path imagePath = new ClassPathResource("static/images/pervez.jpg").getFile().toPath();
        patient.setImage(Base64.getEncoder().encode(Files.readAllBytes(imagePath)));
        patient.setImageContentType(Files.probeContentType(imagePath));
        PatientDTO patientDTO = new PatientDTO();
        String timestamp = "2016-02-16 11:00:02";
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
        Instant birthTimestamp = Instant.from(formatter.parse(timestamp));
        patientDTO.setBirthTimestamp(birthTimestamp);
        patientDTO.setBloodGroup(BloodGroup.A_NEGATIVE);
        patientDTO.setHeightInInch(70.0);
        patientDTO.setSex(Sex.MALE);
        patientDTO.setWeightInKG(70.0);
        patient.setPatientDTO(patientDTO);

        restMvc.perform(
            post("/api/account")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(patient)))
            .andExpect(status().isOk());

        Patient updatedPatient = patientRepository.findOneByUser(newUser).orElse(null);
        User updatedUser = updatedPatient.getUser();
        assertThat(updatedUser.getFirstName()).isEqualTo(patient.getFirstName());
        assertThat(updatedUser.getLastName()).isEqualTo(patient.getLastName());
        assertThat(updatedUser.getEmail()).isEqualTo(patient.getEmail());
        assertThat(updatedUser.getLangKey()).isEqualTo(patient.getLangKey());
        assertThat(updatedUser.getPassword()).isEqualTo(user.getPassword());
        assertThat(updatedUser.getImageUrl()).isEqualTo(patient.getImageUrl());
        assertThat(updatedUser.getActivated()).isEqualTo(true);
        assertThat(updatedPatient.getPhone()).isEqualTo(patient.getPhone());
        assertThat(updatedPatient.getAddress()).isEqualTo(patient.getAddress());
        assertThat(updatedPatient.getImage()).isEqualTo(patient.getImage());
        assertThat(updatedPatient.getImageContentType()).isEqualTo(patient.getImageContentType());
        assertThat(updatedPatient.getBirthTimestamp()).isEqualTo(patientDTO.getBirthTimestamp());
        assertThat(updatedPatient.getBloodGroup()).isEqualTo(patientDTO.getBloodGroup());
        assertThat(updatedPatient.getHeightInInch()).isEqualTo(patientDTO.getHeightInInch());
        assertThat(updatedPatient.getSex()).isEqualTo(patientDTO.getSex());
        assertThat(updatedPatient.getWeightInKG()).isEqualTo(patientDTO.getWeightInKG());
        assertThat(updatedUser.getAuthorities()).isEqualTo(user.getAuthorities());
    }
    
    @Test
    @Transactional
    @WithMockUser("save-invalid-email")
    public void testSaveInvalidEmail() throws Exception {
        User user = new User();
        user.setLogin("save-invalid-email");
        user.setEmail("save-invalid-email@example.com");
        user.setPassword(RandomStringUtils.random(60));
        user.setActivated(true);

        userRepository.saveAndFlush(user);

        UserDTO userDTO = new UserDTO();
        userDTO.setLogin("not-used");
        userDTO.setFirstName("firstname");
        userDTO.setLastName("lastname");
        userDTO.setEmail("invalid email");
        userDTO.setActivated(false);
        userDTO.setImageUrl("http://placehold.it/50x50");
        userDTO.setLangKey(Constants.DEFAULT_LANGUAGE);
        userDTO.setAuthorities(Collections.singleton(AuthoritiesConstants.ADMIN));

        restMvc.perform(
            post("/api/account")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(userDTO)))
            .andExpect(status().isBadRequest());

        assertThat(userRepository.findOneByEmailIgnoreCase("invalid email")).isNotPresent();
    }

    @Test
    @Transactional
    @WithMockUser("save-existing-email")
    public void testSaveExistingEmail() throws Exception {
        User user = new User();
        user.setLogin("save-existing-email");
        user.setEmail("save-existing-email@example.com");
        user.setPassword(RandomStringUtils.random(60));
        user.setActivated(true);

        userRepository.saveAndFlush(user);

        User anotherUser = new User();
        anotherUser.setLogin("save-existing-email2");
        anotherUser.setEmail("save-existing-email2@example.com");
        anotherUser.setPassword(RandomStringUtils.random(60));
        anotherUser.setActivated(true);

        userRepository.saveAndFlush(anotherUser);

        UserDTO userDTO = new UserDTO();
        userDTO.setLogin("not-used");
        userDTO.setFirstName("firstname");
        userDTO.setLastName("lastname");
        userDTO.setEmail("save-existing-email2@example.com");
        userDTO.setActivated(false);
        userDTO.setImageUrl("http://placehold.it/50x50");
        userDTO.setLangKey(Constants.DEFAULT_LANGUAGE);
        userDTO.setAuthorities(Collections.singleton(AuthoritiesConstants.ADMIN));

        restMvc.perform(
            post("/api/account")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(userDTO)))
            .andExpect(status().isBadRequest());

        User updatedUser = userRepository.findOneByLogin("save-existing-email").orElse(null);
        assertThat(updatedUser.getEmail()).isEqualTo("save-existing-email@example.com");
    }

    @Test
    @Transactional
    @WithMockUser("save-existing-email-and-login")
    public void testSaveExistingEmailAndLogin() throws Exception {
        User user = new User();
        user.setLogin("save-existing-email-and-login");
        user.setEmail("save-existing-email-and-login@example.com");
        user.setPassword(RandomStringUtils.random(60));
        user.setActivated(true);

        userRepository.saveAndFlush(user);

        UserDTO userDTO = new UserDTO();
        userDTO.setLogin("not-used");
        userDTO.setFirstName("firstname");
        userDTO.setLastName("lastname");
        userDTO.setEmail("save-existing-email-and-login@example.com");
        userDTO.setActivated(false);
        userDTO.setImageUrl("http://placehold.it/50x50");
        userDTO.setLangKey(Constants.DEFAULT_LANGUAGE);
        userDTO.setAuthorities(Collections.singleton(AuthoritiesConstants.ADMIN));

        restMvc.perform(
            post("/api/account")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(userDTO)))
            .andExpect(status().isOk());

        User updatedUser = userRepository.findOneByLogin("save-existing-email-and-login").orElse(null);
        assertThat(updatedUser.getEmail()).isEqualTo("save-existing-email-and-login@example.com");
    }

    @Test
    @Transactional
    @WithMockUser("change-password-wrong-existing-password")
    public void testChangePasswordWrongExistingPassword() throws Exception {
        User user = new User();
        String currentPassword = RandomStringUtils.random(60);
        user.setPassword(passwordEncoder.encode(currentPassword));
        user.setLogin("change-password-wrong-existing-password");
        user.setEmail("change-password-wrong-existing-password@example.com");
        userRepository.saveAndFlush(user);

        restMvc.perform(post("/api/account/change-password")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(new PasswordChangeDTO("1"+currentPassword, "new password"))))
            .andExpect(status().isBadRequest());

        User updatedUser = userRepository.findOneByLogin("change-password-wrong-existing-password").orElse(null);
        assertThat(passwordEncoder.matches("new password", updatedUser.getPassword())).isFalse();
        assertThat(passwordEncoder.matches(currentPassword, updatedUser.getPassword())).isTrue();
    }

    @Test
    @Transactional
    @WithMockUser("change-password")
    public void testChangePassword() throws Exception {
        User user = new User();
        String currentPassword = RandomStringUtils.random(60);
        user.setPassword(passwordEncoder.encode(currentPassword));
        user.setLogin("change-password");
        user.setEmail("change-password@example.com");
        userRepository.saveAndFlush(user);

        restMvc.perform(post("/api/account/change-password")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(new PasswordChangeDTO(currentPassword, "new password"))))
            .andExpect(status().isOk());

        User updatedUser = userRepository.findOneByLogin("change-password").orElse(null);
        assertThat(passwordEncoder.matches("new password", updatedUser.getPassword())).isTrue();
    }

    @Test
    @Transactional
    @WithMockUser("change-password-too-small")
    public void testChangePasswordTooSmall() throws Exception {
        User user = new User();
        String currentPassword = RandomStringUtils.random(60);
        user.setPassword(passwordEncoder.encode(currentPassword));
        user.setLogin("change-password-too-small");
        user.setEmail("change-password-too-small@example.com");
        userRepository.saveAndFlush(user);

        restMvc.perform(post("/api/account/change-password")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(new PasswordChangeDTO(currentPassword, "new"))))
            .andExpect(status().isBadRequest());

        User updatedUser = userRepository.findOneByLogin("change-password-too-small").orElse(null);
        assertThat(updatedUser.getPassword()).isEqualTo(user.getPassword());
    }

    @Test
    @Transactional
    @WithMockUser("change-password-too-long")
    public void testChangePasswordTooLong() throws Exception {
        User user = new User();
        String currentPassword = RandomStringUtils.random(60);
        user.setPassword(passwordEncoder.encode(currentPassword));
        user.setLogin("change-password-too-long");
        user.setEmail("change-password-too-long@example.com");
        userRepository.saveAndFlush(user);

        restMvc.perform(post("/api/account/change-password")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(new PasswordChangeDTO(currentPassword, RandomStringUtils.random(101)))))
            .andExpect(status().isBadRequest());

        User updatedUser = userRepository.findOneByLogin("change-password-too-long").orElse(null);
        assertThat(updatedUser.getPassword()).isEqualTo(user.getPassword());
    }

    @Test
    @Transactional
    @WithMockUser("change-password-empty")
    public void testChangePasswordEmpty() throws Exception {
        User user = new User();
        user.setPassword(RandomStringUtils.random(60));
        user.setLogin("change-password-empty");
        user.setEmail("change-password-empty@example.com");
        userRepository.saveAndFlush(user);

        restMvc.perform(post("/api/account/change-password").content(RandomStringUtils.random(0)))
            .andExpect(status().isBadRequest());

        User updatedUser = userRepository.findOneByLogin("change-password-empty").orElse(null);
        assertThat(updatedUser.getPassword()).isEqualTo(user.getPassword());
    }

    @Test
    @Transactional
    public void testRequestPasswordReset() throws Exception {
        User user = new User();
        user.setPassword(RandomStringUtils.random(60));
        user.setActivated(true);
        user.setLogin("password-reset");
        user.setEmail("password-reset@example.com");
        userRepository.saveAndFlush(user);

        restMvc.perform(post("/api/account/reset-password/init")
            .content("password-reset@example.com"))
            .andExpect(status().isOk());
    }

    @Test
    @Transactional
    public void testRequestPasswordResetUpperCaseEmail() throws Exception {
        User user = new User();
        user.setPassword(RandomStringUtils.random(60));
        user.setActivated(true);
        user.setLogin("password-reset");
        user.setEmail("password-reset@example.com");
        userRepository.saveAndFlush(user);

        restMvc.perform(post("/api/account/reset-password/init")
            .content("password-reset@EXAMPLE.COM"))
            .andExpect(status().isOk());
    }

    @Test
    public void testRequestPasswordResetWrongEmail() throws Exception {
        restMvc.perform(
            post("/api/account/reset-password/init")
                .content("password-reset-wrong-email@example.com"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    public void testFinishPasswordReset() throws Exception {
        User user = new User();
        user.setPassword(RandomStringUtils.random(60));
        user.setLogin("finish-password-reset");
        user.setEmail("finish-password-reset@example.com");
        user.setResetDate(Instant.now().plusSeconds(60));
        user.setResetKey("reset key");
        userRepository.saveAndFlush(user);

        KeyAndPasswordVM keyAndPassword = new KeyAndPasswordVM();
        keyAndPassword.setKey(user.getResetKey());
        keyAndPassword.setNewPassword("new password");

        restMvc.perform(
            post("/api/account/reset-password/finish")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(keyAndPassword)))
            .andExpect(status().isOk());

        User updatedUser = userRepository.findOneByLogin(user.getLogin()).orElse(null);
        assertThat(passwordEncoder.matches(keyAndPassword.getNewPassword(), updatedUser.getPassword())).isTrue();
    }

    @Test
    @Transactional
    public void testFinishPasswordResetTooSmall() throws Exception {
        User user = new User();
        user.setPassword(RandomStringUtils.random(60));
        user.setLogin("finish-password-reset-too-small");
        user.setEmail("finish-password-reset-too-small@example.com");
        user.setResetDate(Instant.now().plusSeconds(60));
        user.setResetKey("reset key too small");
        userRepository.saveAndFlush(user);

        KeyAndPasswordVM keyAndPassword = new KeyAndPasswordVM();
        keyAndPassword.setKey(user.getResetKey());
        keyAndPassword.setNewPassword("foo");

        restMvc.perform(
            post("/api/account/reset-password/finish")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(keyAndPassword)))
            .andExpect(status().isBadRequest());

        User updatedUser = userRepository.findOneByLogin(user.getLogin()).orElse(null);
        assertThat(passwordEncoder.matches(keyAndPassword.getNewPassword(), updatedUser.getPassword())).isFalse();
    }

    @Test
    @Transactional
    public void testFinishPasswordResetWrongKey() throws Exception {
        KeyAndPasswordVM keyAndPassword = new KeyAndPasswordVM();
        keyAndPassword.setKey("wrong reset key");
        keyAndPassword.setNewPassword("new password");

        restMvc.perform(
            post("/api/account/reset-password/finish")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(keyAndPassword)))
            .andExpect(status().isInternalServerError());
    }
}
