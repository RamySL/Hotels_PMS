package com.pms.hotel.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.annotation.Transactional;

import com.pms.hotel.TestcontainersConfiguration;
import com.pms.hotel.user.Role;
import com.pms.hotel.user.User;
import com.pms.hotel.user.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class AuthControllerIntegrationTest {

	private static final String PASSWORD = "MotDePasse123!";

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private JwtDecoder jwtDecoder;

	private User receptionist;

	@BeforeEach
	void createUsers() {
		receptionist = userRepository.save(new User("claire.martin@pms.local", passwordEncoder.encode(PASSWORD),
				"Claire", "Martin", Role.RECEPTIONIST));
		// Aucun cas d'usage ne désactive encore un compte : insertion directe en base
		jdbcTemplate.update("""
				INSERT INTO users (email, password_hash, first_name, last_name, role, active)
				VALUES ('ancien.employe@pms.local', ?, 'Paul', 'Durand', 'RECEPTIONIST', FALSE)
				""", passwordEncoder.encode(PASSWORD));
	}

	@Test
	void loginReturnsBearerTokenAndUserProfile() {
		assertThat(login("claire.martin@pms.local", PASSWORD)).hasStatusOk().bodyJson().isLenientlyEqualTo("""
				{
				  "tokenType": "Bearer",
				  "expiresIn": 28800,
				  "user": {
				    "id": "%s",
				    "email": "claire.martin@pms.local",
				    "firstName": "Claire",
				    "lastName": "Martin",
				    "role": "RECEPTIONIST"
				  }
				}
				""".formatted(receptionist.getId()));
	}

	@Test
	void issuedTokenIsAcceptedByTheApiAndIdentifiesTheUser() {
		assertThat(login("claire.martin@pms.local", PASSWORD)).bodyJson()
			.extractingPath("$.accessToken")
			.asString()
			.satisfies(accessToken -> {
				Jwt jwt = jwtDecoder.decode(accessToken);
				assertThat(jwt.getSubject()).isEqualTo(receptionist.getId().toString());
				assertThat(jwt.getClaimAsString(JwtService.ROLE_CLAIM)).isEqualTo("RECEPTIONIST");
			});
	}

	@Test
	void emailIsCaseInsensitive() {
		assertThat(login("Claire.MARTIN@PMS.local", PASSWORD)).hasStatusOk();
	}

	// La 3e colonne ne sert qu'à nommer le cas dans les rapports de test
	@ParameterizedTest(name = "{2}")
	@CsvSource({
			"claire.martin@pms.local,  mauvais-mot-de-passe, mot de passe faux",
			"inconnu@pms.local,        " + PASSWORD + ", email inconnu",
			"ancien.employe@pms.local, " + PASSWORD + ", compte désactivé" })
	void failedLoginDoesNotRevealTheReason(String email, String password, String scenario) {
		assertThat(login(email, password))
			.hasStatus(HttpStatus.UNAUTHORIZED)
			.hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
			.bodyJson()
			.isLenientlyEqualTo("""
					{"status": 401, "detail": "Email ou mot de passe incorrect", "instance": "/api/auth/login"}
					""");
	}

	@ParameterizedTest
	@ValueSource(strings = { "{\"email\": \"pas-un-email\", \"password\": \"x\"}",
			"{\"email\": \"claire.martin@pms.local\"}",
			"{\"email\": \"claire.martin@pms.local\", \"password\": \" \"}", "{\"email\": " })
	void invalidPayloadIsRejected(String body) {
		assertThat(mvc.post().uri("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
			.hasStatus(HttpStatus.BAD_REQUEST)
			.hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
	}

	private MvcTestResult login(String email, String password) {
		return mvc.post()
			.uri("/api/auth/login")
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"email": "%s", "password": "%s"}
					""".formatted(email, password))
			.exchange();
	}

}
