package com.pms.hotel.auth;

import java.time.Instant;
import java.util.UUID;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.pms.hotel.user.Role;

/**
 * Émet les jetons d'accès. Leur validation est faite par le resource server de Spring
 * Security (voir SecurityConfig).
 */
@Service
public class JwtService {

	public static final String ISSUER = "pms-hotel";

	public static final String ROLE_CLAIM = "role";

	private final JwtEncoder jwtEncoder;

	private final JwtProperties properties;

	public JwtService(JwtEncoder jwtEncoder, JwtProperties properties) {
		this.jwtEncoder = jwtEncoder;
		this.properties = properties;
	}

	public String generateToken(UUID userId, Role role) {
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer(ISSUER)
			.subject(userId.toString())
			.issuedAt(now)
			.expiresAt(now.plus(properties.expiration()))
			.claim(ROLE_CLAIM, role.name())
			.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}

}
