package com.pms.hotel.room;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record RoomTypeRequest(
		@NotBlank @Size(max = 100) String name,
		@Size(max = 1000) String description,
		@NotNull @Positive @Digits(integer = 8, fraction = 2) BigDecimal basePrice,
		@NotNull @Positive Integer capacity) {
}
