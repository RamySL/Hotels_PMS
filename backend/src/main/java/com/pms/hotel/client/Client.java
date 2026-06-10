package com.pms.hotel.client;

import java.util.Locale;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "clients")
public class Client {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	private String firstName;

	private String lastName;

	private String email;

	private String phone;

	/** Recalculé à chaque modification, voir ClientSearch. */
	private String searchText;

	protected Client() {
		// requis par JPA
	}

	public Client(String firstName, String lastName, String email, String phone) {
		update(firstName, lastName, email, phone);
	}

	/**
	 * Email et téléphone sont facultatifs : un client qui réserve au comptoir ne les donne pas.
	 * Une valeur vide est enregistrée comme absente.
	 */
	public void update(String firstName, String lastName, String email, String phone) {
		this.firstName = firstName.trim();
		this.lastName = lastName.trim();
		String trimmedEmail = blankToNull(email);
		this.email = trimmedEmail == null ? null : trimmedEmail.toLowerCase(Locale.ROOT);
		this.phone = blankToNull(phone);
		this.searchText = ClientSearch.searchText(this.firstName, this.lastName, this.email, this.phone);
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	public UUID getId() {
		return id;
	}

	public String getFirstName() {
		return firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public String getEmail() {
		return email;
	}

	public String getPhone() {
		return phone;
	}

}
