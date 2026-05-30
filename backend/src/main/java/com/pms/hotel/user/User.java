package com.pms.hotel.user;

import java.util.Locale;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	private String email;

	private String passwordHash;

	private String firstName;

	private String lastName;

	@Enumerated(EnumType.STRING)
	private Role role;

	private boolean active;

	protected User() {
		// requis par JPA
	}

	public User(String email, String passwordHash, String firstName, String lastName, Role role) {
		this.email = normalizeEmail(email);
		this.passwordHash = passwordHash;
		this.firstName = firstName;
		this.lastName = lastName;
		this.role = role;
		this.active = true;
	}

	/**
	 * Les emails sont stockés en minuscules (contrainte ck_users_email_lowercase) : toute
	 * recherche par email doit passer par cette normalisation.
	 */
	public static String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

	public UUID getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public String getFirstName() {
		return firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public Role getRole() {
		return role;
	}

	public boolean isActive() {
		return active;
	}

}
