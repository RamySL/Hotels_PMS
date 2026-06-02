package com.pms.hotel.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.pms.hotel.config.SecurityConfig;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Traduction des exceptions en Problem Details, vérifiée sur un contrôleur sonde. */
@WebMvcTest(GlobalExceptionHandlerTest.ProbeController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandlerTest.ProbeController.class })
class GlobalExceptionHandlerTest {

	@Autowired
	private MockMvcTester mvc;

	@Test
	void resourceNotFoundBecomes404() {
		assertThat(mvc.get().uri("/api/test/not-found").with(jwt()))
			.hasStatus(HttpStatus.NOT_FOUND)
			.hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
			.bodyJson()
			.extractingPath("$.detail")
			.isEqualTo("Chambre introuvable : 101");
	}

	@Test
	void businessRuleViolationBecomes409() {
		assertThat(mvc.get().uri("/api/test/conflict").with(jwt()))
			.hasStatus(HttpStatus.CONFLICT)
			.hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
			.bodyJson()
			.extractingPath("$.detail")
			.isEqualTo("Le numéro de chambre 101 existe déjà");
	}

	@Test
	void invalidBodyListsEachFieldInFrenchWhateverTheClientLanguage() {
		assertThat(mvc.post()
			.uri("/api/test/validation")
			.with(jwt())
			.header(HttpHeaders.ACCEPT_LANGUAGE, "en-US")
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"name": "", "capacity": 0}
					"""))
			.hasStatus(HttpStatus.BAD_REQUEST)
			.hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
			.bodyJson()
			.isLenientlyEqualTo("""
					{
					  "title": "Validation Error",
					  "detail": "Un ou plusieurs champs sont invalides",
					  "instance": "/api/test/validation",
					  "errors": {
					    "name": "ne doit pas être vide",
					    "capacity": "doit être supérieur à 0"
					  }
					}
					""");
	}

	@Test
	void validBodyPasses() {
		assertThat(mvc.post()
			.uri("/api/test/validation")
			.with(jwt())
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"name": "Suite", "capacity": 2}
					"""))
			.hasStatusOk();
	}

	/** Exclu du scan des autres tests (classe imbriquée dans une classe de test). */
	@RestController
	public static class ProbeController {

		@GetMapping("/api/test/not-found")
		public void notFound() {
			throw new ResourceNotFoundException("Chambre", "101");
		}

		@GetMapping("/api/test/conflict")
		public void conflict() {
			throw new BusinessRuleException("Le numéro de chambre 101 existe déjà");
		}

		@PostMapping("/api/test/validation")
		public void validation(@Valid @RequestBody ProbeRequest request) {
		}

		public record ProbeRequest(@NotBlank String name, @NotNull @Positive Integer capacity) {
		}

	}

}
