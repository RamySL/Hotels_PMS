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
import com.pms.hotel.room.RoomType;
import com.pms.hotel.user.Role;
import com.pms.hotel.user.User;

class ReservationTest {

	private static final LocalDate ARRIVAL = LocalDate.of(2026, 10, 10);

	private final Room room101 = new Room(new RoomType("Double", null, new BigDecimal("89.90"), 2), "101", 1);

	private final Client client = new Client("Jean", "Dupont", null, null);

	private final User receptionist = new User("reception@pms.local", "hash", "Lucas", "Bernard",
			Role.RECEPTIONIST);

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
		assertThatExceptionOfType(BusinessRuleException.class)
			.isThrownBy(() -> new Reservation(client, room101, ARRIVAL, ARRIVAL.plusDays(1), 3, receptionist,
					Instant.EPOCH))
			.withMessage("La chambre 101 (Double) accueille 2 personnes au maximum");
	}

}
