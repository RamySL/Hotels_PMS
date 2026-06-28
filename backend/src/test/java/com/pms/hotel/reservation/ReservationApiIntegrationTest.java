package com.pms.hotel.reservation;

import static com.pms.hotel.TestAuthentication.receptionist;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

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
import com.pms.hotel.TestDatabase;
import com.pms.hotel.TestcontainersConfiguration;
import com.pms.hotel.client.Client;
import com.pms.hotel.client.ClientRepository;
import com.pms.hotel.room.Room;
import com.pms.hotel.room.RoomRepository;
import com.pms.hotel.room.RoomType;
import com.pms.hotel.room.RoomTypeRepository;

/**
 * Création et modification des réservations sur un vrai PostgreSQL. Sans @Transactional (voir
 * le §6.3 du doc) : chaque requête s'exécute dans ses propres transactions, comme en production.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ReservationApiIntegrationTest {

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

	private Room room102;

	private Client client;

	@BeforeEach
	void createRoomsAndClient() {
		today = LocalDate.now(clock);
		RoomType doubleType = roomTypeRepository.save(new RoomType("Double", null, new BigDecimal("89.90"), 2));
		room101 = roomRepository.save(new Room(doubleType, "101", 1));
		room102 = roomRepository.save(new Room(doubleType, "102", 1));
		client = clientRepository.save(new Client("Jean", "Dupont", null, null));
	}

	@AfterEach
	void deleteData() {
		TestDatabase.deleteBusinessData(jdbcTemplate);
	}

	@Test
	void bookingIsConfirmedPricedAndAttributedToTheReceptionist() {
		assertThat(book(room101, today.plusDays(1), today.plusDays(4))).hasStatus(HttpStatus.CREATED)
			.bodyJson()
			.isLenientlyEqualTo("""
					{
					  "status": "CONFIRMED", "nights": 3, "numberOfGuests": 2, "totalPrice": 269.70,
					  "client": {"lastName": "Dupont"},
					  "room": {"roomNumber": "101", "roomTypeName": "Double"},
					  "createdBy": {"firstName": "Lucas", "lastName": "Bernard"}
					}
					""");
	}

	@Test
	void roomCannotBeBookedTwiceForTheSameNights() {
		book(room101, today.plusDays(1), today.plusDays(4));

		assertThat(book(room101, today.plusDays(3), today.plusDays(5))).hasStatus(HttpStatus.CONFLICT)
			.bodyJson()
			.extractingPath("$.detail")
			.isEqualTo("La chambre 101 est déjà réservée sur cette période");
	}

	@Test
	void nextGuestCanArriveOnThePreviousGuestDepartureDay() {
		book(room101, today.plusDays(1), today.plusDays(4));

		assertThat(book(room101, today.plusDays(4), today.plusDays(6))).hasStatus(HttpStatus.CREATED);
	}

	@Test
	void arrivalInThePastIsRefused() {
		assertThat(book(room101, today.minusDays(1), today.plusDays(1))).hasStatus(HttpStatus.CONFLICT)
			.bodyJson()
			.extractingPath("$.detail")
			.isEqualTo("La date d'arrivée ne peut pas être dans le passé");
	}

	@Test
	void listIsFilteredByPeriodAndStatusAndPaginated() throws Exception {
		String soon = idOf(book(room101, today.plusDays(1), today.plusDays(3)));
		String later = idOf(book(room102, today.plusDays(10), today.plusDays(12)));
		// L'annulation arrive au commit suivant : on la simule en base
		jdbcTemplate.update("UPDATE reservations SET status = 'CANCELLED' WHERE id = ?", UUID.fromString(later));

		assertThat(list("from=" + today + "&to=" + today.plusDays(5))).bodyJson()
			.extractingPath("$.content[*].id")
			.asArray()
			.containsExactly(soon);
		assertThat(list("status=CANCELLED")).bodyJson()
			.extractingPath("$.content[*].id")
			.asArray()
			.containsExactly(later);
		assertThat(list("size=1")).bodyJson().isLenientlyEqualTo("""
				{"content": [{"id": "%s"}], "page": {"size": 1, "totalElements": 2, "totalPages": 2}}
				""".formatted(soon));
	}

	@Test
	void modificationMovesTheStayAndRecalculatesThePrice() throws Exception {
		String id = idOf(book(room101, today.plusDays(1), today.plusDays(3)));

		assertThat(update(id, room101, today.plusDays(2), today.plusDays(6))).hasStatusOk()
			.bodyJson()
			.isLenientlyEqualTo("""
					{"nights": 4, "totalPrice": 359.60}
					""");
	}

	@Test
	void modificationOntoABookedPeriodIsRefused() throws Exception {
		book(room101, today.plusDays(5), today.plusDays(8));
		String id = idOf(book(room101, today.plusDays(1), today.plusDays(3)));

		assertThat(update(id, room101, today.plusDays(2), today.plusDays(6))).hasStatus(HttpStatus.CONFLICT)
			.bodyJson()
			.extractingPath("$.detail")
			.isEqualTo("La chambre 101 est déjà réservée sur cette période");
	}

	@Test
	void duringTheStayOnlyTheDepartureCanChange() throws Exception {
		String id = idOf(book(room101, today, today.plusDays(2)));
		// Le check-in arrive au commit suivant : on le simule en base
		jdbcTemplate.update("UPDATE reservations SET status = 'CHECKED_IN' WHERE id = ?", UUID.fromString(id));

		assertThat(update(id, room102, today, today.plusDays(2))).hasStatus(HttpStatus.CONFLICT)
			.bodyJson()
			.extractingPath("$.detail")
			.isEqualTo("Pendant le séjour, seule la date de départ peut être modifiée");
		assertThat(update(id, room101, today, today.plusDays(3))).hasStatusOk().bodyJson().isLenientlyEqualTo("""
				{"nights": 3, "totalPrice": 269.70}
				""");
	}

	@Test
	void cancelledReservationCannotBeModified() throws Exception {
		String id = idOf(book(room101, today.plusDays(1), today.plusDays(2)));
		jdbcTemplate.update("UPDATE reservations SET status = 'CANCELLED' WHERE id = ?", UUID.fromString(id));

		assertThat(update(id, room101, today.plusDays(1), today.plusDays(3))).hasStatus(HttpStatus.CONFLICT)
			.bodyJson()
			.extractingPath("$.detail")
			.isEqualTo("Une réservation terminée ou annulée n'est plus modifiable");
	}

	private MvcTestResult book(Room room, LocalDate checkIn, LocalDate checkOut) {
		return mvc.post()
			.uri("/api/reservations")
			.with(receptionist())
			.contentType(MediaType.APPLICATION_JSON)
			.content(body(room, checkIn, checkOut))
			.exchange();
	}

	private MvcTestResult update(String id, Room room, LocalDate checkIn, LocalDate checkOut) {
		return mvc.put()
			.uri("/api/reservations/{id}", id)
			.with(receptionist())
			.contentType(MediaType.APPLICATION_JSON)
			.content(body(room, checkIn, checkOut))
			.exchange();
	}

	private MvcTestResult list(String query) {
		return mvc.get().uri("/api/reservations?" + query).with(receptionist()).exchange();
	}

	private String body(Room room, LocalDate checkIn, LocalDate checkOut) {
		return """
				{"clientId": "%s", "roomId": "%s", "checkInDate": "%s", "checkOutDate": "%s", "numberOfGuests": 2}
				""".formatted(client.getId(), room.getId(), checkIn, checkOut);
	}

	private static String idOf(MvcTestResult result) throws Exception {
		assertThat(result).hasStatus(HttpStatus.CREATED);
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

}
