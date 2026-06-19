package com.pms.hotel.reservation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class StayDatesValidator implements ConstraintValidator<ValidStayDates, ReservationRequest> {

	@Override
	public boolean isValid(ReservationRequest request, ConstraintValidatorContext context) {
		if (request.checkInDate() == null || request.checkOutDate() == null
				|| request.checkOutDate().isAfter(request.checkInDate())) {
			// Des dates absentes sont signalées par @NotNull
			return true;
		}
		context.disableDefaultConstraintViolation();
		context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
			.addPropertyNode("checkOutDate")
			.addConstraintViolation();
		return false;
	}

}
