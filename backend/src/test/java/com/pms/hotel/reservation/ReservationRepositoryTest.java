package com.pms.hotel.reservation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.pms.hotel.TestcontainersConfiguration;
import com.pms.hotel.client.Client;
import com.pms.hotel.client.ClientRepository;
import com.pms.hotel.room.Room;
import com.pms.hotel.room.RoomRepository;
import com.pms.hotel.room.RoomType;
import com.pms.hotel.room.RoomTypeRepository;
import com.pms.hotel.user.User;
import com.pms.hotel.user.UserRepository;

/**
 * Contrainte d'exclusion ex_reservations_no_overlap (ADR-006) et requêtes des
 * réservations, testées directement en base.
 */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
class ReservationRepositoryTest {

	private static final LocalDate DAY_10 = LocalDate.of(2026, 10, 10);

	@Autowired
	private ReservationRepository reservationRepository;

	@Autowired
	private RoomTypeRepository roomTypeRepository;

	@Autowired
	private RoomRepository roomRepository;

	@Autowired
	private ClientRepository clientRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private Room room101;

	private Client client;

	private User receptionist;

	@BeforeEach
	void setUp() {
		RoomType twin = roomTypeRepository.save(new RoomType("Twin", null, new BigDecimal("79.00"), 2));
		room101 = roomRepository.save(new Room(twin, "101", 1));
		client = clientRepository.save(new Client("Jean", "Dupont", null, null));
		receptionist = userRepository.findByEmail("reception@pms.local").orElseThrow();
	}

	@Test
	void databaseRefusesTwoActiveReservationsOverlappingOnTheSameRoom() {
		book(room101, DAY_10, DAY_10.plusDays(3));

		assertThatExceptionOfType(DataIntegrityViolationException.class)
			.isThrownBy(() -> book(room101, DAY_10.plusDays(2), DAY_10.plusDays(4)))
			.withMessageContaining("ex_reservations_no_overlap");
	}

	@Test
	void checkOutAndCheckInOnTheSameDayDoNotOverlap() {
		book(room101, DAY_10, DAY_10.plusDays(3));

		assertThatCode(() -> book(room101, DAY_10.plusDays(3), DAY_10.plusDays(5))).doesNotThrowAnyException();
	}

	@Test
	void sameDatesOnAnotherRoomAreAllowed() {
		Room room102 = roomRepository.save(new Room(room101.getRoomType(), "102", 1));
		book(room101, DAY_10, DAY_10.plusDays(3));

		assertThatCode(() -> book(room102, DAY_10, DAY_10.plusDays(3))).doesNotThrowAnyException();
	}

	@Test
	void cancelledReservationNoLongerHoldsTheRoom() {
		Reservation cancelled = book(room101, DAY_10, DAY_10.plusDays(3));
		// L'annulation arrive avec le cycle de vie : on la simule en base
		jdbcTemplate.update("UPDATE reservations SET status = 'CANCELLED' WHERE id = ?", cancelled.getId());

		assertThatCode(() -> book(room101, DAY_10.plusDays(1), DAY_10.plusDays(2))).doesNotThrowAnyException();
	}

	@Test
	void expectedArrivalsAreConfirmedStaysThatCanStillCheckIn() {
		Reservation arrivingToday = book(room("201", 2), DAY_10, DAY_10.plusDays(2));
		Reservation lateButStillExpected = book(room("102", 1), DAY_10.minusDays(1), DAY_10.plusDays(1));
		// Séjour fini sans que le client soit venu : le check-in n'est plus possible
		book(room("103", 1), DAY_10.minusDays(1), DAY_10);
		book(room("104", 1), DAY_10.plusDays(1), DAY_10.plusDays(2));
		book(room("105", 1), DAY_10, DAY_10.plusDays(2)).checkIn(DAY_10);
		book(room("106", 1), DAY_10, DAY_10.plusDays(2)).cancel();

		// Triées par étage puis numéro : la 102 avant la 201
		assertThat(reservationRepository.findExpectedArrivals(DAY_10)).containsExactly(lateButStillExpected,
				arrivingToday);
	}

	@Test
	void expectedDeparturesAreGuestsStillInWhoseDepartureIsTodayOrPast() {
		Reservation leavingToday = checkedIn(room("201", 2), DAY_10.minusDays(2), DAY_10);
		Reservation forgottenYesterday = checkedIn(room("102", 1), DAY_10.minusDays(3), DAY_10.minusDays(1));
		checkedIn(room("103", 1), DAY_10.minusDays(1), DAY_10.plusDays(1));
		checkedIn(room("104", 1), DAY_10.minusDays(2), DAY_10).checkOut();
		// Client jamais venu : ce n'est pas un départ
		book(room("105", 1), DAY_10.minusDays(2), DAY_10);

		assertThat(reservationRepository.findExpectedDepartures(DAY_10)).containsExactly(forgottenYesterday,
				leavingToday);
	}

	private Room room(String roomNumber, int floor) {
		return roomRepository.save(new Room(room101.getRoomType(), roomNumber, floor));
	}

	private Reservation checkedIn(Room room, LocalDate checkIn, LocalDate checkOut) {
		Reservation stay = book(room, checkIn, checkOut);
		stay.checkIn(checkIn);
		return stay;
	}

	private Reservation book(Room room, LocalDate checkIn, LocalDate checkOut) {
		return reservationRepository
			.saveAndFlush(new Reservation(client, room, checkIn, checkOut, 2, receptionist, Instant.now()));
	}

}
