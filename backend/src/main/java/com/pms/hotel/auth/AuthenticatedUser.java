package com.pms.hotel.auth;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;

import com.pms.hotel.user.User;

/**
 * Adapte l'entité User au contrat de Spring Security, sans que l'entité en dépende. Le login
 * récupère ainsi l'utilisateur authentifié sans seconde requête.
 */
record AuthenticatedUser(User user) implements UserDetails {

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return AuthorityUtils.createAuthorityList("ROLE_" + user.getRole().name());
	}

	@Override
	public String getPassword() {
		return user.getPasswordHash();
	}

	@Override
	public String getUsername() {
		return user.getEmail();
	}

	@Override
	public boolean isEnabled() {
		return user.isActive();
	}

}
