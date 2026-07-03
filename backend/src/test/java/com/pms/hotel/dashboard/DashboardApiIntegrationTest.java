package com.pms.hotel.dashboard;

import static com.pms.hotel.TestAuthentication.receptionist;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
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
import com.pms.hotel.TestAuthentication;
import com.pms.hotel.TestDatabase;
import com.pms.hotel.TestcontainersConfiguration;
import com.pms.hotel.client.Client;
import com.pms.hotel.client.ClientRepository;
import com.pms.hotel.invoice.Invoice;
import com.pms.hotel.invoice.InvoiceRepository;
import com.pms.hotel.reservation.Reservation;
import com.pms.hotel.reservation.ReservationRepository;
import com.pms.hotel.room.Room;
import com.pms.hotel.room.RoomRepository;
import com.pms.hotel.room.RoomStatus;
import com.pms.hotel.room.RoomType;
import com.pms.hotel.room.RoomTypeRepository;
import com.pms.hotel.user.User;
import com.pms.hotel.user.UserRepository;

/** Tableau de bord du jour, sur un vrai PostgreSQL et sans @Transactional (§6.3). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class DashboardApiIntegrationTest {

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
	private UserRepository userRepository;

	@Autowired
	private ReservationRepository reservationRepository;

	@Autowired
	private InvoiceRepository invoiceRepository;

	@Autowired
	private Clock clock;

	private LocalDate today;

	private RoomType doubleType;

	private Client client;

	@BeforeEach
	void createRoomTypeAndClient() {
		today = LocalDate.now(clock);
		doubleType = roomTypeRepository.save(new RoomType("Double", null, new BigDecimal("89.90"), 2));
		client = clientRepository.save(new Client("Jean", "Dupont", null, null));
	}

	@AfterEach
	void deleteData() {
		TestDatabase.deleteBusinessData(jdbcTemplate);
	}

	@Test
	void arrivalsShowWhatIsLeftToPayAndLeaveTheListAtCheckIn() throws Exception {
		String twoNights = book(room("101"), today, today.plusDays(2));
		String oneNight = book(room("102"), today, today.plusDays(1));
		pay(twoNights, "50.00");

		assertThat(dashboard()).hasStatusOk().bodyJson().isLenientlyEqualTo("""
				{"date": "%s", "departures": [], "arrivals": [
				  {"reservationId": "%s", "clientName": "Jean Dupont", "roomNumber": "101", "balance": 129.80},
				  {"reservationId": "%s", "roomNumber": "102", "balance": 89.90}
				]}
				""".formatted(today, twoNights, oneNight));

		assertThat(mvc.patch().uri("/api/reservations/{id}/check-in", twoNights).with(receptionist())).hasStatusOk();

		assertThat(dashboard()).bodyJson().extractingPath("$.arrivals[*].roomNumber").asArray().containsExactly("102");
	}

	@Test
	void departureForgottenYesterdayStaysListedUntilCheckOut() throws Exception {
		String forgotten = stayCheckedIn(room("103"), today.minusDays(3), today.minusDays(1));

		assertThat(dashboard()).hasStatusOk().bodyJson().isLenientlyEqualTo("""
				{"arrivals": [], "departures": [
				  {"reservationId": "%s", "roomNumber": "103", "checkOutDate": "%s", "balance": 179.80}
				]}
				""".formatted(forgotten, today.minusDays(1)));

		pay(forgotten, "179.80");
		assertThat(mvc.patch().uri("/api/reservations/{id}/check-out", forgotten).with(receptionist())).hasStatusOk();

		assertThat(dashboard()).bodyJson().extractingPath("$.departures").asArray().isEmpty();
	}

	@Test
	void occupancyCountsTheNightsSoldTonightAmongRoomsInService() throws Exception {
		book(room("101"), today, today.plusDays(2));
		// Le client de la 102 part aujourd'hui : la nuit de ce soir n'est pas vendue
		stayCheckedIn(room("102"), today.minusDays(1), today);
		room("103");
		roomOutOfService("104");

		assertThat(dashboard()).hasStatusOk().bodyJson().isLenientlyEqualTo("""
				{"occupancy": {"occupiedRooms": 1, "roomsInService": 3, "rate": 33.3}}
				""");
	}

	@Test
	void checkedOutRoomIsToCleanUntilItIsReadyAgain() throws Exception {
		Room room103 = room("103");
		String leavingToday = stayCheckedIn(room103, today.minusDays(1), today);
		pay(leavingToday, "89.90");
		assertThat(mvc.patch().uri("/api/reservations/{id}/check-out", leavingToday).with(receptionist()))
			.hasStatusOk();

		assertThat(dashboard()).hasStatusOk().bodyJson().isLenientlyEqualTo("""
				{"roomsToClean": [{"roomNumber": "103", "status": "CLEANING"}]}
				""");

		assertThat(mvc.patch()
			.uri("/api/rooms/{id}/status", room103.getId())
			.with(receptionist())
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"status": "READY"}
					""")).hasStatusOk();

		assertThat(dashboard()).bodyJson().extractingPath("$.roomsToClean").asArray().isEmpty();
	}

	private MvcTestResult dashboard() {
		return mvc.get().uri("/api/dashboard/today").with(receptionist()).exchange();
	}

	private Room room(String roomNumber) {
		return roomRepository.save(new Room(doubleType, roomNumber, 1));
	}

	private void roomOutOfService(String roomNumber) {
		Room room = new Room(doubleType, roomNumber, 1);
		room.changeStatus(RoomStatus.OUT_OF_SERVICE);
		roomRepository.save(room);
	}

	private String book(Room room, LocalDate checkIn, LocalDate checkOut) throws Exception {
		MvcTestResult result = mvc.post()
			.uri("/api/reservations")
			.with(receptionist())
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"clientId": "%s", "roomId": "%s", "checkInDate": "%s", "checkOutDate": "%s", "numberOfGuests": 2}
					""".formatted(client.getId(), room.getId(), checkIn, checkOut))
			.exchange();
		assertThat(result).hasStatus(HttpStatus.CREATED);
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	/**
	 * L'API refuse une arrivée passée : un séjour commencé avant aujourd'hui est créé
	 * directement.
	 */
	private String stayCheckedIn(Room room, LocalDate checkIn, LocalDate checkOut) {
		User receptionist = userRepository.findById(TestAuthentication.RECEPTIONIST_ID).orElseThrow();
		Reservation stay = new Reservation(client, room, checkIn, checkOut, 2, receptionist, Instant.now(clock));
		stay.checkIn(checkIn);
		roomRepository.save(room);
		reservationRepository.save(stay);
		invoiceRepository.save(new Invoice(stay, Instant.now(clock)));
		return stay.getId().toString();
	}

	private void pay(String reservationId, String amount) throws Exception {
		String invoiceId = JsonPath.read(mvc.get()
			.uri("/api/invoices?reservationId={id}", reservationId)
			.with(receptionist())
			.exchange()
			.getResponse()
			.getContentAsString(), "$.content[0].id");
		assertThat(mvc.post()
			.uri("/api/invoices/{id}/payments", invoiceId)
			.with(receptionist())
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"amount": %s, "method": "CASH"}
					""".formatted(amount))).hasStatus(HttpStatus.CREATED);
	}

}
