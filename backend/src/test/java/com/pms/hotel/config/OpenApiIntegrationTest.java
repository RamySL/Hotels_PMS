package com.pms.hotel.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.pms.hotel.TestcontainersConfiguration;

/**
 * Documentation de l'API : publique et complète, alors que l'API qu'elle décrit reste
 * protégée.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OpenApiIntegrationTest {

	@Autowired
	private MockMvcTester mvc;

	@Test
	void apiDescriptionIsPublicAndGroupsTheRoutesUnderTheJwtScheme() {
		assertThat(apiDocs()).hasStatusOk().bodyJson().isLenientlyEqualTo("""
				{"paths": {
				   "/api/dashboard/today": {"get": {"tags": ["Tableau de bord"]}},
				   "/api/reservations/{id}/check-in": {"patch": {"tags": ["Réservations"]}},
				   "/api/invoices/{id}/payments": {"post": {"tags": ["Facturation"]}}},
				 "components": {"securitySchemes": {
				   "bearer-jwt": {"type": "http", "scheme": "bearer", "bearerFormat": "JWT"}}},
				 "security": [{"bearer-jwt": []}]}
				""");
	}

	@Test
	void onlyLoginAndPingAreDocumentedWithoutJwt() {
		var docs = assertThat(apiDocs()).bodyJson();
		docs.extractingPath("$.paths['/api/auth/login'].post.security").asArray().isEmpty();
		docs.extractingPath("$.paths['/api/ping'].get.security").asArray().isEmpty();
		// Les autres opérations ne déclarent rien :
		// elles héritent du JWT exigé globalement
		docs.extractingPath("$.paths.*.*.security").asArray().hasSize(2);
	}

	@Test
	void paymentAuthorComesFromTheTokenNotFromAParameter() {
		assertThat(apiDocs()).bodyJson()
			.extractingPath("$.paths['/api/invoices/{id}/payments'].post.parameters[*].name")
			.asArray()
			.containsExactly("id");
	}

	@Test
	void paginationIsDocumentedAsPageSizeAndSort() {
		assertThat(apiDocs()).bodyJson()
			.extractingPath("$.paths['/api/reservations'].get.parameters[*].name")
			.asArray()
			.contains("page", "size", "sort");
	}

	@Test
	void swaggerUiIsPublic() {
		assertThat(mvc.get().uri("/swagger-ui/index.html")).hasStatusOk();
	}

	@Test
	void theDocumentedApiStaysProtected() {
		assertThat(mvc.get().uri("/api/rooms")).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	private MvcTestResult apiDocs() {
		return mvc.get().uri("/v3/api-docs").exchange();
	}

}
