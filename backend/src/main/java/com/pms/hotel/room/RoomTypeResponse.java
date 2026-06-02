package com.pms.hotel.room;

import java.math.BigDecimal;
import java.util.UUID;

public record RoomTypeResponse(UUID id, String name, String description, BigDecimal basePrice, int capacity) {

	static RoomTypeResponse from(RoomType roomType) {
		return new RoomTypeResponse(roomType.getId(), roomType.getName(), roomType.getDescription(),
				roomType.getBasePrice(), roomType.getCapacity());
	}

}
