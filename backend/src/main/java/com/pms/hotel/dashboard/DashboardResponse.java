package com.pms.hotel.dashboard;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.pms.hotel.reservation.Reservation;
import com.pms.hotel.room.RoomResponse;

/**
 * Journée de l'hôtel : arrivées et départs encore à faire (retards compris), occupation de la
 * nuit et chambres à nettoyer. date est la date de l'hôtel : un séjour dont l'arrivée, ou le
 * départ, précède date est en retard.
 */
public record DashboardResponse(LocalDate date, List<Stay> arrivals, List<Stay> departures, Occupancy occupancy,
		List<RoomResponse> roomsToClean) {

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

	/**
	 * Nuits vendues ce soir parmi les chambres en service. rate : pourcentage arrondi à une
	 * décimale, 0 s'il n'y a aucune chambre en service.
	 */
	public record Occupancy(long occupiedRooms, long roomsInService, BigDecimal rate) {

		static Occupancy of(long occupiedRooms, long roomsInService) {
			// L'arrondi se fait dans la division : sans échelle, divide exige un résultat exact et
			// lève une ArithmeticException dès que le taux ne tombe pas juste (2 sur 3 : 66,66…)
			BigDecimal rate = roomsInService == 0 ? BigDecimal.ZERO
					: BigDecimal.valueOf(occupiedRooms * 100)
						.divide(BigDecimal.valueOf(roomsInService), 1, RoundingMode.HALF_UP);
			return new Occupancy(occupiedRooms, roomsInService, rate);
		}

	}

}
