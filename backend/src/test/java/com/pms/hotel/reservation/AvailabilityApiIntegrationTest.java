package com.pms.hotel.reservation;

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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.pms.hotel.TestcontainersConfiguration;
import com.pms.hotel.client.Client;
import com.pms.hotel.client.ClientRepository;
import com.pms.hotel.room.Room;
import com.pms.hotel.room.RoomRepository;
import com.pms.hotel.room.RoomStatus;
import com.pms.hotel.room.RoomType;
import com.pms.hotel.room.RoomTypeRepository;
import com.pms.hotel.user.UserRepository;

/**
 * Disponibilité sur un vrai PostgreSQL, sans @Transactional (§6.3) : la 101 est réservée de J+1
 * à J+3, la 102 est libre, la 103 est hors service.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AvailabilityApiIntegrationTest {

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
	private ReservationRepository reservationRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private Clock clock;

	private LocalDate today;

	@BeforeEach
	void bookRoom101AndBreakRoom103() {
		today = LocalDate.now(clock);
		RoomType doubleType = roomTypeRepository.save(new RoomType("Double", null, new BigDecimal("89.90"), 2));
		Room room101 = roomRepository.save(new Room(doubleType, "101", 1));
		roomRepository.save(new Room(doubleType, "102", 1));
		Room room103 = new Room(doubleType, "103", 1);
		room103.changeStatus(RoomStatus.OUT_OF_SERVICE);
		roomRepository.save(room103);
		Client client = clientRepository.save(new Client("Jean", "Dupont", null, null));
		reservationRepository.save(new Reservation(client, room101, today.plusDays(1), today.plusDays(3), 2,
				userRepository.findByEmail("reception@pms.local").orElseThrow(), Instant.now(clock)));
	}

	@AfterEach
	void deleteData() {
		jdbcTemplate.update("DELETE FROM reservations");
		jdbcTemplate.update("DELETE FROM clients");
		jdbcTemplate.update("DELETE FROM rooms");
		jdbcTemplate.update("DELETE FROM room_types");
	}

	@Test
	void bookedAndOutOfServiceRoomsAreExcluded() {
		assertThat(availability(today.plusDays(1), today.plusDays(3))).hasStatusOk()
			.bodyJson()
			.extractingPath("$[*].roomNumber")
			.asArray()
			.containsExactly("102");
	}

	@Test
	void roomIsAvailableAgainFromItsGuestDepartureDay() {
		assertThat(availability(today.plusDays(3), today.plusDays(5))).bodyJson()
			.extractingPath("$[*].roomNumber")
			.asArray()
			.containsExactly("101", "102");
	}

	@Test
	void eachRoomComesWithThePriceOfTheStay() {
		assertThat(availability(today.plusDays(3), today.plusDays(5))).bodyJson().isLenientlyEqualTo("""
				[
				  {"roomNumber": "101", "roomTypeName": "Double", "capacity": 2,
				   "pricePerNight": 89.90, "nights": 2, "totalPrice": 179.80},
				  {"roomNumber": "102", "nights": 2, "totalPrice": 179.80}
				]
				""");
	}

	private MvcTestResult availability(LocalDate checkIn, LocalDate checkOut) {
		return mvc.get()
			.uri("/api/rooms/availability?checkIn={in}&checkOut={out}", checkIn, checkOut)
			.with(receptionist())
			.exchange();
	}

}
