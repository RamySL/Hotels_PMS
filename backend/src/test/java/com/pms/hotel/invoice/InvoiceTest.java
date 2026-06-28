package com.pms.hotel.invoice;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.pms.hotel.client.Client;
import com.pms.hotel.reservation.Reservation;
import com.pms.hotel.room.Room;
import com.pms.hotel.room.RoomType;
import com.pms.hotel.user.Role;
import com.pms.hotel.user.User;

class InvoiceTest {

	private static final LocalDate ARRIVAL = LocalDate.of(2026, 10, 10);

	private final Invoice invoice = new Invoice(twoNightsInADouble(), Instant.EPOCH);

	@Test
	void newInvoiceIsUnpaidAndOwesThePriceOfTheStay() {
		assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.UNPAID);
		assertThat(invoice.getAmountTotal()).isEqualByComparingTo("179.80");
		assertThat(invoice.balance()).isEqualByComparingTo("179.80");
	}

	@Test
	void totalFollowsTheReservationPrice() {
		invoice.updateTotal(new BigDecimal("269.70"));

		assertThat(invoice.getAmountTotal()).isEqualByComparingTo("269.70");
		assertThat(invoice.balance()).isEqualByComparingTo("269.70");
		assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.UNPAID);
	}

	@Test
	void cancelledInvoiceIsNoLongerDue() {
		invoice.cancel();

		assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.CANCELLED);
		assertThat(invoice.balance()).isEqualByComparingTo("0");
	}

	@Test
	void cancelledInvoiceStaysCancelledWhenItsTotalChanges() {
		invoice.cancel();

		invoice.updateTotal(new BigDecimal("89.90"));

		assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.CANCELLED);
	}

	private static Reservation twoNightsInADouble() {
		Room room101 = new Room(new RoomType("Double", null, new BigDecimal("89.90"), 2), "101", 1);
		return new Reservation(new Client("Jean", "Dupont", null, null), room101, ARRIVAL, ARRIVAL.plusDays(2), 2,
				new User("reception@pms.local", "hash", "Lucas", "Bernard", Role.RECEPTIONIST), Instant.EPOCH);
	}

}
