package com.pms.hotel.reservation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Le départ doit suivre l'arrivée (au moins une nuit). L'erreur est rattachée au champ
 * checkOutDate, pour que le front l'affiche sous ce champ.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = StayDatesValidator.class)
public @interface ValidStayDates {

	String message() default "doit être postérieure à la date d'arrivée";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}
