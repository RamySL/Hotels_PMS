package com.pms.hotel.reservation;

import static com.pms.hotel.TestAuthentication.receptionist;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.jayway.jsonpath.JsonPath;
import com.pms.hotel.TestcontainersConfiguration;
import com.pms.hotel.client.Client;
import com.pms.hotel.client.ClientRepository;
import com.pms.hotel.room.Room;
import com.pms.hotel.room.RoomRepository;
import com.pms.hotel.room.RoomType;
import com.pms.hotel.room.RoomTypeRepository;

/** Cycle de vie complet d'une réservation, sur un vrai PostgreSQL et sans @Transactional (§6.3). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ReservationLifecycleIntegrationTest {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private RoomTypeRepository roomTypeRepository;

	@Autowired
	private RoomRepository roomRepository;

	@Autowired
	private ClientRepository clientRepository;

	@Autowired
	private Clock clock;

	private LocalDate today;

	private Room room101;

	private Client client;

	@BeforeEach
	void createRoomAndClient() {
		today = LocalDate.now(clock);
		RoomType doubleType = roomTypeRepository.save(new RoomType("Double", null, new BigDecimal("89.90"), 2));
		room101 = roomRepository.save(new Room(doubleType, "101", 1));
		client = clientRepository.save(new Client("Jean", "Dupont", null, null));
	}

	@AfterEach
	void deleteData() {
		jdbcTemplate.update("DELETE FROM reservations");
		jdbcTemplate.update("DELETE FROM clients");
		jdbcTemplate.update("DELETE FROM rooms");
		jdbcTemplate.update("DELETE FROM room_types");
	}

	@Test
	void fromBookingToDeparture() throws Exception {
		String id = idOf(book(today, today.plusDays(2)));

		assertThat(patch(id, "check-in")).hasStatusOk()
			.bodyJson()
			.extractingPath("$.status")
			.isEqualTo("CHECKED_IN");
		assertThat(roomStatus()).isEqualTo("OCCUPIED");
		assertThat(changeRoomStatus("READY")).hasStatus(HttpStatus.CONFLICT);

		assertThat(mvc.put()
			.uri("/api/reservations/{id}", id)
			.with(receptionist())
			.contentType(MediaType.APPLICATION_JSON)
			.content(body(today, today.plusDays(3)))).hasStatusOk()
			.bodyJson()
			.extractingPath("$.totalPrice")
			.isEqualTo(269.70);

		assertThat(patch(id, "check-out")).hasStatusOk()
			.bodyJson()
			.extractingPath("$.status")
			.isEqualTo("CHECKED_OUT");
		assertThat(roomStatus()).isEqualTo("CLEANING");
		// Le ménage fait, la réception remet la chambre en service pour le client suivant
		assertThat(changeRoomStatus("READY")).hasStatusOk();
	}

	@Test
	void cancellationFreesTheRoom() throws Exception {
		String id = idOf(book(today.plusDays(1), today.plusDays(3)));

		assertThat(patch(id, "cancel")).hasStatusOk().bodyJson().extractingPath("$.status").isEqualTo("CANCELLED");
		assertThat(book(today.plusDays(1), today.plusDays(3))).hasStatus(HttpStatus.CREATED);
	}

	@Test
	void checkInRefusedWhileTheRoomIsBeingCleaned() throws Exception {
		jdbcTemplate.update("UPDATE rooms SET status = 'CLEANING' WHERE id = ?", room101.getId());
		String id = idOf(book(today, today.plusDays(1)));

		assertThat(patch(id, "check-in")).hasStatus(HttpStatus.CONFLICT)
			.bodyJson()
			.extractingPath("$.detail")
			.isEqualTo("La chambre 101 n'est pas prête (CLEANING)");
	}

	@Test
	void checkInRefusedBeforeTheArrivalDay() throws Exception {
		String id = idOf(book(today.plusDays(1), today.plusDays(2)));

		assertThat(patch(id, "check-in")).hasStatus(HttpStatus.CONFLICT);
	}

	private String roomStatus() {
		return jdbcTemplate.queryForObject("SELECT status FROM rooms WHERE id = ?", String.class, room101.getId());
	}

	private MvcTestResult changeRoomStatus(String status) {
		return mvc.patch()
			.uri("/api/rooms/{id}/status", room101.getId())
			.with(receptionist())
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"status": "%s"}
					""".formatted(status))
			.exchange();
	}

	private MvcTestResult patch(String id, String action) {
		return mvc.patch().uri("/api/reservations/{id}/" + action, id).with(receptionist()).exchange();
	}

	private MvcTestResult book(LocalDate checkIn, LocalDate checkOut) {
		return mvc.post()
			.uri("/api/reservations")
			.with(receptionist())
			.contentType(MediaType.APPLICATION_JSON)
			.content(body(checkIn, checkOut))
			.exchange();
	}

	private String body(LocalDate checkIn, LocalDate checkOut) {
		return """
				{"clientId": "%s", "roomId": "%s", "checkInDate": "%s", "checkOutDate": "%s", "numberOfGuests": 2}
				""".formatted(client.getId(), room101.getId(), checkIn, checkOut);
	}

	private static String idOf(MvcTestResult result) throws Exception {
		assertThat(result).hasStatus(HttpStatus.CREATED);
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

}
