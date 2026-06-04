package com.pms.hotel.room;

import java.util.UUID;

public record RoomResponse(UUID id, String roomNumber, int floor, RoomStatus status, RoomTypeResponse roomType) {

	static RoomResponse from(Room room) {
		return new RoomResponse(room.getId(), room.getRoomNumber(), room.getFloor(), room.getStatus(),
				RoomTypeResponse.from(room.getRoomType()));
	}

}
