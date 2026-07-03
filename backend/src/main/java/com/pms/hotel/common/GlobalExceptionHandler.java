package com.pms.hotel.common;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Format d'erreur unique de l'API : RFC 7807 (Problem Details). Les erreurs standard de
 * Spring MVC (400, 404, 405…) sont traitées par {@link ResponseEntityExceptionHandler}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	/**
	 * Échec du login. Même réponse pour un mot de passe faux, un email inconnu ou un
	 * compte désactivé : l'API ne révèle pas quels comptes existent.
	 */
	@ExceptionHandler({ BadCredentialsException.class, AccountStatusException.class })
	ResponseEntity<ProblemDetail> handleLoginFailure(AuthenticationException exception) {
		return unauthorized("Bearer", "Email ou mot de passe incorrect");
	}

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
		return problem(HttpStatus.FORBIDDEN, "Accès refusé");
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	ResponseEntity<ProblemDetail> handleNotFound(ResourceNotFoundException exception) {
		return problem(HttpStatus.NOT_FOUND, exception.getMessage());
	}

	@ExceptionHandler(BusinessRuleException.class)
	ResponseEntity<ProblemDetail> handleBusinessRule(BusinessRuleException exception) {
		return problem(HttpStatus.CONFLICT, exception.getMessage());
	}

	/**
	 * Verrouillage optimiste (@Version) : quelqu'un a modifié la même donnée entre-temps.
	 */
	@ExceptionHandler(OptimisticLockingFailureException.class)
	ResponseEntity<ProblemDetail> handleConcurrentModification(OptimisticLockingFailureException exception) {
		return problem(HttpStatus.CONFLICT,
				"Ces données viennent d'être modifiées par quelqu'un d'autre : rechargez-les et réessayez");
	}

	/**
	 * Corps de requête invalide : chaque champ en erreur est détaillé dans « errors »,
	 * pour que le front affiche le message sous le champ concerné.
	 */
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		Map<String, String> errors = new LinkedHashMap<>();
		exception.getBindingResult()
			.getFieldErrors()
			.forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, "Un ou plusieurs champs sont invalides");
		problem.setTitle("Validation Error");
		problem.setProperty("errors", errors);
		return handleExceptionInternal(exception, problem, headers, status, request);
	}

	/** RFC 6750 : un 401 indique le schéma attendu dans WWW-Authenticate. */
	private static ResponseEntity<ProblemDetail> unauthorized(String challenge, String detail) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
			.header(HttpHeaders.WWW_AUTHENTICATE, challenge)
			.body(ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, detail));
	}

	private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
		return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detail));
	}

}
