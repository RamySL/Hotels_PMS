package com.pms.hotel.common;

/**
 * Règle métier violée par rapport à l'état actuel des données (doublon, transition de
 * statut interdite…). Traduite en 409 par GlobalExceptionHandler.
 */
public class BusinessRuleException extends RuntimeException {

	public BusinessRuleException(String message) {
		super(message);
	}

}
