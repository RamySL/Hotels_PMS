package com.pms.hotel.reservation;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ReservationResponse(UUID id, ReservationStatus status, LocalDate checkInDate, LocalDate checkOutDate,
		long nights, int numberOfGuests, BigDecimal totalPrice, ClientSummary client, RoomSummary room,
		UserSummary createdBy, Instant createdAt) {

	static ReservationResponse from(Reservation reservation) {
		var client = reservation.getClient();
		var room = reservation.getRoom();
		var createdBy = reservation.getCreatedBy();
		return new ReservationResponse(reservation.getId(), reservation.getStatus(), reservation.getCheckInDate(),
				reservation.getCheckOutDate(), reservation.nights(), reservation.getNumberOfGuests(),
				reservation.getTotalPrice(),
				new ClientSummary(client.getId(), client.getFirstName(), client.getLastName()),
				new RoomSummary(room.getId(), room.getRoomNumber(), room.getRoomType().getName()),
				new UserSummary(createdBy.getId(), createdBy.getFirstName(), createdBy.getLastName()),
				reservation.getCreatedAt());
	}

	public record ClientSummary(UUID id, String firstName, String lastName) {
	}

	public record RoomSummary(UUID id, String roomNumber, String roomTypeName) {
	}

	public record UserSummary(UUID id, String firstName, String lastName) {
	}

}
