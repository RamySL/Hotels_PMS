package com.pms.hotel.auth;

import java.time.Duration;
import java.util.UUID;

import com.pms.hotel.user.Role;
import com.pms.hotel.user.User;

/**
 * Réponse du login : le jeton d'accès, sa durée de validité en secondes, et le profil dont
 * le front a besoin dès la connexion.
 */
public record LoginResponse(String accessToken, String tokenType, long expiresIn, UserProfile user) {

	static LoginResponse bearer(String accessToken, Duration validity, User user) {
		return new LoginResponse(accessToken, "Bearer", validity.toSeconds(), UserProfile.from(user));
	}

	public record UserProfile(UUID id, String email, String firstName, String lastName, Role role) {

		static UserProfile from(User user) {
			return new UserProfile(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
					user.getRole());
		}

	}

}
