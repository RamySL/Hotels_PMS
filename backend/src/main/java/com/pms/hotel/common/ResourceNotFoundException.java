package com.pms.hotel.common;

/** Ressource demandée inexistante. Traduite en 404 par GlobalExceptionHandler. */
public class ResourceNotFoundException extends RuntimeException {

	public ResourceNotFoundException(String resourceName, Object id) {
		super(resourceName + " introuvable : " + id);
	}

}
