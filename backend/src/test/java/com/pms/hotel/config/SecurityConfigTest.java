package com.pms.hotel.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pms.hotel.auth.JwtService;
import com.pms.hotel.health.PingController;
import com.pms.hotel.user.Role;

/**
 * Règles de sécurité de l'API, vérifiées sur un contrôleur sonde : aucune route métier
 * n'existe encore en phase 2.
 */
@WebMvcTest(PingController.class)
@Import({ SecurityConfig.class, JwtService.class, SecurityConfigTest.ProbeController.class })
class SecurityConfigTest {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private JwtEncoder jwtEncoder;

	@Test
	void pingIsPublic() {
		assertThat(mvc.get().uri("/api/ping")).hasStatusOk();
	}

	@Test
	void requestWithoutTokenIsRejected() {
		assertThat(mvc.get().uri("/api/test/whoami"))
			.hasStatus(HttpStatus.UNAUTHORIZED)
			.hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
			.hasHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
			.bodyJson()
			.extractingPath("$.detail")
			.isEqualTo("Authentification requise");
	}

	@Test
	void validTokenAuthenticatesUserWithRoleAuthority() {
		UUID userId = UUID.randomUUID();

		MvcTestResult result = mvc.get()
			.uri("/api/test/whoami")
			.header(HttpHeaders.AUTHORIZATION, bearer(jwtService.generateToken(userId, Role.ADMIN)))
			.exchange();

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.name").isEqualTo(userId.toString());
		assertThat(result).bodyJson().extractingPath("$.authorities").asArray().contains("ROLE_ADMIN");
	}

	@Test
	void expiredTokenIsRejected() {
		Instant issuedAt = Instant.now().minus(Duration.ofHours(9));

		assertRejectedAsInvalid(encode(jwtEncoder, JwtService.ISSUER, issuedAt, issuedAt.plus(Duration.ofHours(8))));
	}

	@Test
	void tokenFromAnotherIssuerIsRejected() {
		Instant now = Instant.now();

		assertRejectedAsInvalid(encode(jwtEncoder, "autre-emetteur", now, now.plus(Duration.ofHours(1))));
	}

	@Test
	void tokenSignedWithAnotherKeyIsRejected() {
		byte[] otherKey = new byte[32];
		new SecureRandom().nextBytes(otherKey);
		JwtEncoder forger = NimbusJwtEncoder.withSecretKey(new SecretKeySpec(otherKey, "HmacSHA256"))
			.algorithm(MacAlgorithm.HS256)
			.build();
		Instant now = Instant.now();

		assertRejectedAsInvalid(encode(forger, JwtService.ISSUER, now, now.plus(Duration.ofHours(1))));
	}

	@Test
	void malformedTokenIsRejected() {
		assertRejectedAsInvalid("pas-un-jwt");
	}

	@Test
	void adminOnlyEndpointRejectsReceptionist() {
		assertThat(mvc.get()
			.uri("/api/test/admin")
			.header(HttpHeaders.AUTHORIZATION, bearer(jwtService.generateToken(UUID.randomUUID(), Role.RECEPTIONIST))))
			.hasStatus(HttpStatus.FORBIDDEN)
			.hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
			.bodyJson()
			.extractingPath("$.detail")
			.isEqualTo("Accès refusé");
	}

	@Test
	void adminOnlyEndpointAcceptsAdmin() {
		assertThat(mvc.get()
			.uri("/api/test/admin")
			.header(HttpHeaders.AUTHORIZATION, bearer(jwtService.generateToken(UUID.randomUUID(), Role.ADMIN))))
			.hasStatusOk();
	}

	private void assertRejectedAsInvalid(String token) {
		assertThat(mvc.get().uri("/api/test/whoami").header(HttpHeaders.AUTHORIZATION, bearer(token)))
			.hasStatus(HttpStatus.UNAUTHORIZED)
			.hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
			.hasHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer error=\"invalid_token\"")
			.bodyJson()
			.extractingPath("$.detail")
			.isEqualTo("Jeton invalide ou expiré");
	}

	private static String encode(JwtEncoder encoder, String issuer, Instant issuedAt, Instant expiresAt) {
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer(issuer)
			.subject(UUID.randomUUID().toString())
			.issuedAt(issuedAt)
			.expiresAt(expiresAt)
			.claim(JwtService.ROLE_CLAIM, Role.ADMIN.name())
			.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}

	private static String bearer(String token) {
		return "Bearer " + token;
	}

	/** Exclue du scan des autres tests (classe imbriquée dans une classe de test). */
	@RestController
	public static class ProbeController {

		@GetMapping("/api/test/whoami")
		public Map<String, Object> whoami(Authentication authentication) {
			return Map.of("name", authentication.getName(), "authorities",
					AuthorityUtils.authorityListToSet(authentication.getAuthorities()));
		}

		@GetMapping("/api/test/admin")
		@PreAuthorize("hasRole('ADMIN')")
		public String adminOnly() {
			return "ok";
		}

	}

}
