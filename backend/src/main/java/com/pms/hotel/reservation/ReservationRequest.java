package com.pms.hotel.reservation;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@ValidStayDates
public record ReservationRequest(@NotNull UUID clientId, @NotNull UUID roomId, @NotNull LocalDate checkInDate,
		@NotNull LocalDate checkOutDate, @NotNull @Positive Integer numberOfGuests) {
}
