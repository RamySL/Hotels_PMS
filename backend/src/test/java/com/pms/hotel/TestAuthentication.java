package com.pms.hotel;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Requêtes MockMvc authentifiées par JWT, avec l'autorité que SecurityConfig déduit du claim
 * « role ».
 */
public final class TestAuthentication {

	private TestAuthentication() {
	}

	public static RequestPostProcessor admin() {
		return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
	}

	public static RequestPostProcessor receptionist() {
		return jwt().authorities(new SimpleGrantedAuthority("ROLE_RECEPTIONIST"));
	}

}
