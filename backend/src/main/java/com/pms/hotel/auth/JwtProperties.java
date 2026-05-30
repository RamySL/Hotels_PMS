package com.pms.hotel.auth;

import java.time.Duration;
import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Paramètres des jetons JWT. Le secret est validé dès le démarrage : une clé absente ou
 * trop faible doit empêcher l'application de démarrer plutôt que permettre de forger des
 * jetons.
 */
@ConfigurationProperties("pms.jwt")
public record JwtProperties(String secret, Duration expiration) {

	/** HS256 exige une clé d'au moins 256 bits. */
	private static final int MIN_SECRET_BYTES = 32;

	public JwtProperties {
		if (secret == null || secret.isBlank()) {
			throw new IllegalArgumentException("pms.jwt.secret est obligatoire (variable JWT_SECRET)");
		}
		if (decode(secret).length < MIN_SECRET_BYTES) {
			throw new IllegalArgumentException(
					"pms.jwt.secret doit faire au moins 256 bits : générer avec openssl rand -base64 32");
		}
		if (expiration == null || expiration.isNegative() || expiration.isZero()) {
			throw new IllegalArgumentException("pms.jwt.expiration doit être une durée positive");
		}
	}

	public SecretKey secretKey() {
		return new SecretKeySpec(decode(secret), "HmacSHA256");
	}

	@Override
	public String toString() {
		return "JwtProperties[secret=****, expiration=" + expiration + "]";
	}

	private static byte[] decode(String secret) {
		try {
			return Base64.getDecoder().decode(secret);
		}
		catch (IllegalArgumentException ex) {
			throw new IllegalArgumentException("pms.jwt.secret doit être encodé en base64", ex);
		}
	}

}
