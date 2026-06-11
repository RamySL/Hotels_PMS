package com.pms.hotel.client;

import java.util.UUID;

public record ClientResponse(UUID id, String firstName, String lastName, String email, String phone) {

	static ClientResponse from(Client client) {
		return new ClientResponse(client.getId(), client.getFirstName(), client.getLastName(), client.getEmail(),
				client.getPhone());
	}

}
