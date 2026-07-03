package com.pms.hotel;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import java.util.UUID;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Requêtes MockMvc authentifiées par JWT, avec l'autorité que SecurityConfig déduit du
 * claim « role ». Le sub est l'id d'un compte de démo du seed V2, qui existe donc en
 * base.
 */
public final class TestAuthentication {

	public static final UUID ADMIN_ID = UUID.fromString("1cfd0fb0-2d26-4835-bec8-55f9317b0b14");

	public static final UUID RECEPTIONIST_ID = UUID.fromString("df376895-62fc-4376-a4bb-4de71365aa54");

	private TestAuthentication() {
	}

	public static RequestPostProcessor admin() {
		return jwt().jwt(token -> token.subject(ADMIN_ID.toString()))
			.authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
	}

	public static RequestPostProcessor receptionist() {
		return jwt().jwt(token -> token.subject(RECEPTIONIST_ID.toString()))
			.authorities(new SimpleGrantedAuthority("ROLE_RECEPTIONIST"));
	}

}
