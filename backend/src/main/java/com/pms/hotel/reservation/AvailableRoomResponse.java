package com.pms.hotel.reservation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.pms.hotel.room.Room;
import com.pms.hotel.room.RoomType;

/** Chambre libre sur la période demandée, avec ce qu'il faut pour remplir le formulaire de réservation. */
public record AvailableRoomResponse(UUID roomId, String roomNumber, int floor, String roomTypeName, int capacity,
		BigDecimal pricePerNight, long nights, BigDecimal totalPrice) {

	static AvailableRoomResponse of(Room room, LocalDate checkIn, LocalDate checkOut) {
		RoomType roomType = room.getRoomType();
		return new AvailableRoomResponse(room.getId(), room.getRoomNumber(), room.getFloor(), roomType.getName(),
				roomType.getCapacity(), roomType.getBasePrice(), Reservation.nightsBetween(checkIn, checkOut),
				Reservation.priceOf(roomType, checkIn, checkOut));
	}

}
