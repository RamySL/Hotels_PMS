package com.pms.hotel.reservation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.pms.hotel.client.Client;
import com.pms.hotel.common.BusinessRuleException;
import com.pms.hotel.room.Room;
import com.pms.hotel.room.RoomStatus;
import com.pms.hotel.room.RoomType;
import com.pms.hotel.user.Role;
import com.pms.hotel.user.User;

class ReservationTest {

	private static final LocalDate ARRIVAL = LocalDate.of(2026, 10, 10);

	private final Room room101 = new Room(new RoomType("Double", null, new BigDecimal("89.90"), 2), "101", 1);

	private final Client client = new Client("Jean", "Dupont", null, null);

	private final User receptionist = new User("reception@pms.local", "hash", "Lucas", "Bernard", Role.RECEPTIONIST);

	@Test
	void newReservationIsConfirmedAndPricedPerNight() {
		Reservation reservation = new Reservation(client, room101, ARRIVAL, ARRIVAL.plusDays(3), 2, receptionist,
				Instant.EPOCH);

		assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
		assertThat(reservation.nights()).isEqualTo(3);
		assertThat(reservation.getTotalPrice()).isEqualByComparingTo("269.70");
	}

	@Test
	void rescheduledReservationIsRepriced() {
		Reservation reservation = new Reservation(client, room101, ARRIVAL, ARRIVAL.plusDays(3), 2, receptionist,
				Instant.EPOCH);

		reservation.reschedule(room101, ARRIVAL, ARRIVAL.plusDays(5), 1);

		assertThat(reservation.getTotalPrice()).isEqualByComparingTo("449.50");
		assertThat(reservation.getNumberOfGuests()).isEqualTo(1);
	}

	@Test
	void guestsCannotExceedTheRoomTypeCapacity() {
		assertThatExceptionOfType(BusinessRuleException.class).isThrownBy(
				() -> new Reservation(client, room101, ARRIVAL, ARRIVAL.plusDays(1), 3, receptionist, Instant.EPOCH))
			.withMessage("La chambre 101 (Double) accueille 2 personnes au maximum");
	}

	@Test
	void checkInOnTheArrivalDayOccupiesTheRoom() {
		Reservation reservation = stay(ARRIVAL, ARRIVAL.plusDays(2));

		reservation.checkIn(ARRIVAL);

		assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CHECKED_IN);
		assertThat(room101.getStatus()).isEqualTo(RoomStatus.OCCUPIED);
	}

	@Test
	void checkInIsOnlyPossibleDuringTheStay() {
		Reservation reservation = stay(ARRIVAL, ARRIVAL.plusDays(2));

		assertThatExceptionOfType(BusinessRuleException.class)
			.isThrownBy(() -> reservation.checkIn(ARRIVAL.minusDays(1)))
			.withMessage("Le check-in n'est possible qu'à partir du 2026-10-10");
		assertThatExceptionOfType(BusinessRuleException.class)
			.isThrownBy(() -> reservation.checkIn(ARRIVAL.plusDays(2)))
			.withMessage("Le séjour est terminé : départ prévu le 2026-10-12");
	}

	@Test
	void checkInHappensOnlyOnce() {
		Reservation reservation = stay(ARRIVAL, ARRIVAL.plusDays(2));
		reservation.checkIn(ARRIVAL);

		assertThatExceptionOfType(BusinessRuleException.class).isThrownBy(() -> reservation.checkIn(ARRIVAL))
			.withMessage("Seule une réservation confirmée peut faire l'objet d'un check-in");
	}

	@Test
	void checkOutEndsTheStayAndLeavesTheRoomToClean() {
		Reservation reservation = stay(ARRIVAL, ARRIVAL.plusDays(2));
		reservation.checkIn(ARRIVAL);

		reservation.checkOut();

		assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CHECKED_OUT);
		assertThat(room101.getStatus()).isEqualTo(RoomStatus.CLEANING);
	}

	@Test
	void checkOutRequiresAnArrivedGuest() {
		Reservation reservation = stay(ARRIVAL, ARRIVAL.plusDays(2));

		assertThatExceptionOfType(BusinessRuleException.class).isThrownBy(reservation::checkOut)
			.withMessage("Seul un client arrivé peut faire l'objet d'un check-out");
	}

	@Test
	void confirmedReservationCanBeCancelledOnce() {
		Reservation reservation = stay(ARRIVAL, ARRIVAL.plusDays(2));

		reservation.cancel();

		assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
		assertThatExceptionOfType(BusinessRuleException.class).isThrownBy(reservation::cancel)
			.withMessage("Cette réservation est déjà annulée");
	}

	@Test
	void stayInProgressCannotBeCancelled() {
		Reservation reservation = stay(ARRIVAL, ARRIVAL.plusDays(2));
		reservation.checkIn(ARRIVAL);

		assertThatExceptionOfType(BusinessRuleException.class).isThrownBy(reservation::cancel)
			.withMessage("Un séjour commencé ou terminé ne peut pas être annulé");
	}

	@Test
	void finishedStayCannotBeCancelled() {
		Reservation reservation = stay(ARRIVAL, ARRIVAL.plusDays(2));
		reservation.checkIn(ARRIVAL);
		reservation.checkOut();

		assertThatExceptionOfType(BusinessRuleException.class).isThrownBy(reservation::cancel)
			.withMessage("Un séjour commencé ou terminé ne peut pas être annulé");
	}

	private Reservation stay(LocalDate checkIn, LocalDate checkOut) {
		return new Reservation(client, room101, checkIn, checkOut, 2, receptionist, Instant.EPOCH);
	}

}
