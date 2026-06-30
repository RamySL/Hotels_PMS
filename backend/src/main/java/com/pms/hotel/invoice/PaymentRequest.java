package com.pms.hotel.invoice;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PaymentRequest(
		@NotNull @Positive @Digits(integer = 8, fraction = 2) BigDecimal amount,
		@NotNull PaymentMethod method) {
}
