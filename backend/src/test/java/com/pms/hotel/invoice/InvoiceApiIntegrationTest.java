package com.pms.hotel.invoice;

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
import com.pms.hotel.TestDatabase;
import com.pms.hotel.TestcontainersConfiguration;
import com.pms.hotel.client.Client;
import com.pms.hotel.client.ClientRepository;
import com.pms.hotel.room.Room;
import com.pms.hotel.room.RoomRepository;
import com.pms.hotel.room.RoomType;
import com.pms.hotel.room.RoomTypeRepository;

/** Facture créée et tenue à jour par les réservations, sur un vrai PostgreSQL et sans @Transactional (§6.3). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class InvoiceApiIntegrationTest {

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
		TestDatabase.deleteBusinessData(jdbcTemplate);
	}

	@Test
	void bookingIssuesAnUnpaidInvoiceForThePriceOfTheStay() throws Exception {
		String reservationId = book(today.plusDays(1), today.plusDays(3));

		assertThat(invoicesOf(reservationId)).hasStatusOk().bodyJson().isLenientlyEqualTo("""
				{"content": [{
				  "status": "UNPAID", "amountTotal": 179.80, "amountPaid": 0.00, "balance": 179.80,
				  "reservation": {"id": "%s", "clientName": "Jean Dupont", "roomNumber": "101"},
				  "payments": []
				}], "page": {"totalElements": 1}}
				""".formatted(reservationId));
	}

	@Test
	void invoiceFollowsTheStayWhenItIsExtended() throws Exception {
		String reservationId = book(today.plusDays(1), today.plusDays(3));

		assertThat(mvc.put()
			.uri("/api/reservations/{id}", reservationId)
			.with(receptionist())
			.contentType(MediaType.APPLICATION_JSON)
			.content(body(today.plusDays(1), today.plusDays(4)))).hasStatusOk();

		assertThat(invoicesOf(reservationId)).bodyJson()
			.extractingPath("$.content[0].amountTotal")
			.isEqualTo(269.70);
	}

	@Test
	void cancellingTheReservationCancelsItsInvoice() throws Exception {
		String reservationId = book(today.plusDays(1), today.plusDays(3));

		assertThat(mvc.patch().uri("/api/reservations/{id}/cancel", reservationId).with(receptionist()))
			.hasStatusOk();

		assertThat(mvc.get().uri("/api/invoices?status=CANCELLED").with(receptionist())).bodyJson()
			.isLenientlyEqualTo("""
					{"content": [{"status": "CANCELLED", "balance": 0.00, "reservation": {"id": "%s"}}]}
					""".formatted(reservationId));
	}

	@Test
	void invoiceDetailIsReachableByItsId() throws Exception {
		String reservationId = book(today.plusDays(1), today.plusDays(2));
		String invoiceId = JsonPath.read(invoicesOf(reservationId).getResponse().getContentAsString(),
				"$.content[0].id");

		assertThat(mvc.get().uri("/api/invoices/{id}", invoiceId).with(receptionist())).hasStatusOk()
			.bodyJson()
			.extractingPath("$.amountTotal")
			.isEqualTo(89.90);
	}

	private MvcTestResult invoicesOf(String reservationId) {
		return mvc.get().uri("/api/invoices?reservationId={id}", reservationId).with(receptionist()).exchange();
	}

	private String book(LocalDate checkIn, LocalDate checkOut) throws Exception {
		MvcTestResult result = mvc.post()
			.uri("/api/reservations")
			.with(receptionist())
			.contentType(MediaType.APPLICATION_JSON)
			.content(body(checkIn, checkOut))
			.exchange();
		assertThat(result).hasStatus(HttpStatus.CREATED);
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	private String body(LocalDate checkIn, LocalDate checkOut) {
		return """
				{"clientId": "%s", "roomId": "%s", "checkInDate": "%s", "checkOutDate": "%s", "numberOfGuests": 2}
				""".formatted(client.getId(), room101.getId(), checkIn, checkOut);
	}

}
