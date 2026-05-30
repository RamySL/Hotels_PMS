package com.pms.hotel.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class JwtPropertiesTest {

	private static final Duration EIGHT_HOURS = Duration.ofHours(8);

	@Test
	void acceptsBase64SecretOfAtLeast256Bits() {
		JwtProperties properties = new JwtProperties(randomSecret(32), EIGHT_HOURS);

		assertThat(properties.secretKey().getEncoded()).hasSize(32);
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = "   ")
	void rejectsMissingSecret(String secret) {
		assertThatIllegalArgumentException().isThrownBy(() -> new JwtProperties(secret, EIGHT_HOURS))
			.withMessageContaining("JWT_SECRET");
	}

	@Test
	void rejectsSecretThatIsNotBase64() {
		assertThatIllegalArgumentException().isThrownBy(() -> new JwtProperties("pas du base64 !", EIGHT_HOURS))
			.withMessageContaining("base64");
	}

	@Test
	void rejectsSecretShorterThan256Bits() {
		assertThatIllegalArgumentException().isThrownBy(() -> new JwtProperties(randomSecret(31), EIGHT_HOURS))
			.withMessageContaining("256 bits");
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { "PT0S", "-PT1H" })
	void rejectsNonPositiveExpiration(Duration expiration) {
		assertThatIllegalArgumentException().isThrownBy(() -> new JwtProperties(randomSecret(32), expiration))
			.withMessageContaining("expiration");
	}

	@Test
	void doesNotExposeSecretInToString() {
		String secret = randomSecret(32);

		assertThat(new JwtProperties(secret, EIGHT_HOURS)).asString().doesNotContain(secret);
	}

	private static String randomSecret(int bytes) {
		byte[] key = new byte[bytes];
		new SecureRandom().nextBytes(key);
		return Base64.getEncoder().encodeToString(key);
	}

}
