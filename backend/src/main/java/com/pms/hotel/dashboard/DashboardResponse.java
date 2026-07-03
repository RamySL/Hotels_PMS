package com.pms.hotel.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.pms.hotel.reservation.Reservation;

/**
 * Journée de l'hôtel : arrivées et départs encore à faire, retards compris. date est la date de
 * l'hôtel : un séjour dont l'arrivée, ou le départ, précède date est en retard.
 */
public record DashboardResponse(LocalDate date, List<Stay> arrivals, List<Stay> departures) {

	/** Séjour à traiter. balance : reste à payer de sa facture ; négatif, trop-perçu. */
	public record Stay(UUID reservationId, String clientName, String roomNumber, LocalDate checkInDate,
			LocalDate checkOutDate, int numberOfGuests, BigDecimal balance) {

		static Stay of(Reservation reservation, BigDecimal balance) {
			var client = reservation.getClient();
			return new Stay(reservation.getId(), client.getFirstName() + " " + client.getLastName(),
					reservation.getRoom().getRoomNumber(), reservation.getCheckInDate(),
					reservation.getCheckOutDate(), reservation.getNumberOfGuests(), balance);
		}

	}

}
