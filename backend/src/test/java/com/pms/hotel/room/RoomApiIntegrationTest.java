package com.pms.hotel.room;

import static com.pms.hotel.TestAuthentication.admin;
import static com.pms.hotel.TestAuthentication.receptionist;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.jayway.jsonpath.JsonPath;
import com.pms.hotel.TestDatabase;
import com.pms.hotel.TestcontainersConfiguration;

/**
 * Parcours complet des chambres sur un vrai PostgreSQL. Volontairement
 * sans @Transactional : chaque requête s'exécute dans ses propres transactions, comme en
 * production, ce qui fait apparaître les problèmes de chargement paresseux qu'une
 * transaction de test masquerait.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RoomApiIntegrationTest {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@AfterEach
	void deleteRooms() {
		TestDatabase.deleteBusinessData(jdbcTemplate);
	}

	@Test
	void roomLifecycleThroughTheApi() {
		String typeId = createdId(
				mvc.post().uri("/api/room-types").with(admin()).contentType(MediaType.APPLICATION_JSON).content("""
						{"name": "Double", "basePrice": 89.90, "capacity": 2}
						"""));
		String roomId = createdId(
				mvc.post().uri("/api/rooms").with(admin()).contentType(MediaType.APPLICATION_JSON).content("""
						{"roomTypeId": "%s", "roomNumber": "101", "floor": 1}
						""".formatted(typeId)));

		assertThat(mvc.get().uri("/api/rooms").with(receptionist())).hasStatusOk()
			.bodyJson()
			.isLenientlyEqualTo(
					"""
							[{"id": "%s", "roomNumber": "101", "status": "READY", "roomType": {"name": "Double", "basePrice": 89.90}}]
							"""
						.formatted(roomId));

		assertThat(changeStatus(roomId, "CLEANING")).hasStatusOk();
		assertThat(mvc.get().uri("/api/rooms?status=CLEANING").with(receptionist())).hasStatusOk()
			.bodyJson()
			.extractingPath("$[*].roomNumber")
			.asArray()
			.containsExactly("101");

		assertThat(changeStatus(roomId, "OCCUPIED")).hasStatus(HttpStatus.CONFLICT);
	}

	@Test
	void occupiedRoomStatusCannotBeChangedManually() {
		// Aucun cas d'usage ne rend encore une chambre OCCUPIED (check-in en
		// phase 5) : insertion directe
		jdbcTemplate.update("""
				INSERT INTO room_types (id, name, base_price, capacity)
				VALUES ('0b5ae5de-0000-4000-8000-000000000001', 'Suite', 150.00, 2)
				""");
		jdbcTemplate
			.update("""
					INSERT INTO rooms (id, room_type_id, room_number, floor, status)
					VALUES ('0b5ae5de-0000-4000-8000-000000000002', '0b5ae5de-0000-4000-8000-000000000001', '301', 3, 'OCCUPIED')
					""");

		assertThat(changeStatus("0b5ae5de-0000-4000-8000-000000000002", "READY")).hasStatus(HttpStatus.CONFLICT)
			.bodyJson()
			.extractingPath("$.detail")
			.isEqualTo("La chambre 301 est occupée : son statut changera au check-out");
	}

	private MockMvcTester.MockMvcRequestBuilder changeStatus(String roomId, String status) {
		return mvc.patch()
			.uri("/api/rooms/{id}/status", roomId)
			.with(receptionist())
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"status": "%s"}
					""".formatted(status));
	}

	private static String createdId(MockMvcTester.MockMvcRequestBuilder request) {
		var result = request.exchange();
		assertThat(result).hasStatus(HttpStatus.CREATED);
		try {
			return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
		}
		catch (java.io.UnsupportedEncodingException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
