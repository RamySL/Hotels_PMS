package com.pms.hotel.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.pms.hotel.user.Role;

class JwtServiceTest {

	private final JwtProperties properties = new JwtProperties(randomSecret(), Duration.ofHours(8));

	private final JwtService jwtService = new JwtService(
			NimbusJwtEncoder.withSecretKey(properties.secretKey()).algorithm(MacAlgorithm.HS256).build(), properties);

	@Test
	void tokenCarriesUserIdRoleAndIssuer() {
		UUID userId = UUID.randomUUID();

		Jwt jwt = decoderFor(properties.secretKey()).decode(jwtService.generateToken(userId, Role.ADMIN));

		assertThat(jwt.getSubject()).isEqualTo(userId.toString());
		assertThat(jwt.getClaimAsString(JwtService.ROLE_CLAIM)).isEqualTo("ADMIN");
		assertThat(jwt.getClaimAsString(JwtClaimNames.ISS)).isEqualTo(JwtService.ISSUER);
		assertThat(jwt.getHeaders()).containsEntry("alg", "HS256");
	}

	@Test
	void tokenExpiresAfterConfiguredDuration() {
		Jwt jwt = decoderFor(properties.secretKey())
			.decode(jwtService.generateToken(UUID.randomUUID(), Role.RECEPTIONIST));

		assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofHours(8));
	}

	@Test
	void tokenSignedWithAnotherKeyIsRejected() {
		String token = jwtService.generateToken(UUID.randomUUID(), Role.ADMIN);
		JwtDecoder decoderWithAnotherKey = decoderFor(
				new JwtProperties(randomSecret(), Duration.ofHours(8)).secretKey());

		assertThatExceptionOfType(BadJwtException.class).isThrownBy(() -> decoderWithAnotherKey.decode(token));
	}

	private static JwtDecoder decoderFor(SecretKey key) {
		return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
	}

	private static String randomSecret() {
		byte[] key = new byte[32];
		new SecureRandom().nextBytes(key);
		return Base64.getEncoder().encodeToString(key);
	}

}
