package com.pms.hotel.room;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record RoomRequest(
		@NotNull UUID roomTypeId,
		@NotBlank @Size(max = 10) String roomNumber,
		@NotNull @PositiveOrZero Integer floor) {
}
