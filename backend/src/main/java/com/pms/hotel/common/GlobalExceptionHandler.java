package com.pms.hotel.common;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Format d'erreur unique de l'API : RFC 7807 (Problem Details). Les erreurs standard de
 * Spring MVC (400, 404, 405…) sont traitées par {@link ResponseEntityExceptionHandler}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	/** Jeton présent mais refusé : signature, expiration, émetteur ou format. */
	@ExceptionHandler(OAuth2AuthenticationException.class)
	ResponseEntity<ProblemDetail> handleInvalidToken(OAuth2AuthenticationException exception) {
		return unauthorized("Bearer error=\"invalid_token\"", "Jeton invalide ou expiré");
	}

	@ExceptionHandler(AuthenticationException.class)
	ResponseEntity<ProblemDetail> handleAuthentication(AuthenticationException exception) {
		return unauthorized("Bearer", "Authentification requise");
	}

	@ExceptionHandler(AccessDeniedException.class)
	ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException exception) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
			.body(ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Accès refusé"));
	}

	/** RFC 6750 : un 401 indique le schéma attendu dans WWW-Authenticate. */
	private static ResponseEntity<ProblemDetail> unauthorized(String challenge, String detail) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
			.header(HttpHeaders.WWW_AUTHENTICATE, challenge)
			.body(ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, detail));
	}

}
