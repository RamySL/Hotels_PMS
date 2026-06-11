package com.pms.hotel.client;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Email et téléphone facultatifs : un client qui réserve au comptoir ne les donne pas. */
public record ClientRequest(
		@NotBlank @Size(max = 100) String firstName,
		@NotBlank @Size(max = 100) String lastName,
		@Email @Size(max = 255) String email,
		@Size(max = 30) @Pattern(regexp = "[0-9 .+()-]*",
				message = "ne doit contenir que des chiffres, des espaces et + . ( ) -") String phone) {
}
