package com.pms.hotel.client;

import static com.pms.hotel.TestAuthentication.receptionist;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.jayway.jsonpath.JsonPath;
import com.pms.hotel.TestDatabase;
import com.pms.hotel.TestcontainersConfiguration;

/**
 * Parcours des clients sur un vrai PostgreSQL. Sans @Transactional (voir le §6.3 du doc)
 * : chaque requête s'exécute dans ses propres transactions, comme en production.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ClientApiIntegrationTest {

	private static final String HELENE = """
			{"firstName": "Hélène", "lastName": "Lefèvre", "email": "Helene.Lefevre@Mail.fr", "phone": "06 12 34 56 78"}
			""";

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@AfterEach
	void deleteClients() {
		TestDatabase.deleteBusinessData(jdbcTemplate);
	}

	@Test
	void walkInClientIsRegisteredWithItsNameOnly() throws Exception {
		String id = create("""
				{"firstName": "Jean", "lastName": "Dupont"}
				""");

		assertThat(mvc.get().uri("/api/clients/{id}", id).with(receptionist())).hasStatusOk()
			.bodyJson()
			.isLenientlyEqualTo("""
					{"firstName": "Jean", "lastName": "Dupont", "email": null, "phone": null}
					""");
	}

	@ParameterizedTest
	@ValueSource(strings = { "helene", "LEFEVRE", "lefevre@mail", "06.12", "0612 34" })
	void searchIgnoresCaseAccentsAndPhoneFormatting(String term) throws Exception {
		create(HELENE);
		create("""
				{"firstName": "Jean", "lastName": "Dupont"}
				""");

		assertThat(mvc.get().uri("/api/clients").param("search", term).with(receptionist())).hasStatusOk()
			.bodyJson()
			.extractingPath("$.content[*].lastName")
			.asArray()
			.containsExactly("Lefèvre");
	}

	@Test
	void clientsArePaginatedInAlphabeticalOrder() throws Exception {
		create("""
				{"firstName": "Paul", "lastName": "Martin"}
				""");
		create("""
				{"firstName": "Jean", "lastName": "Dupont"}
				""");
		create(HELENE);

		MvcTestResult firstPage = mvc.get().uri("/api/clients?size=2").with(receptionist()).exchange();
		assertThat(firstPage).bodyJson()
			.extractingPath("$.content[*].lastName")
			.asArray()
			.containsExactly("Dupont", "Lefèvre");
		assertThat(firstPage).bodyJson().isLenientlyEqualTo("""
				{"page": {"size": 2, "number": 0, "totalElements": 3, "totalPages": 2}}
				""");
		assertThat(mvc.get().uri("/api/clients?size=2&page=1").with(receptionist())).bodyJson()
			.extractingPath("$.content[*].lastName")
			.asArray()
			.containsExactly("Martin");
	}

	@Test
	void updateReplacesTheRecordAndTheSearchFollows() throws Exception {
		String id = create(HELENE);

		assertThat(mvc.put()
			.uri("/api/clients/{id}", id)
			.with(receptionist())
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"firstName": "Hélène", "lastName": "Martin"}
					""")).hasStatusOk().bodyJson().isLenientlyEqualTo("""
					{"lastName": "Martin", "email": null, "phone": null}
					""");
		assertThat(mvc.get().uri("/api/clients?search=martin").with(receptionist())).bodyJson()
			.extractingPath("$.page.totalElements")
			.isEqualTo(1);
		assertThat(mvc.get().uri("/api/clients?search=lefevre").with(receptionist())).bodyJson()
			.extractingPath("$.page.totalElements")
			.isEqualTo(0);
	}

	@Test
	void unknownClientIsNotFound() {
		assertThat(mvc.get().uri("/api/clients/{id}", "8a1d0f7e-0000-4000-8000-000000000000").with(receptionist()))
			.hasStatus(HttpStatus.NOT_FOUND);
	}

	private String create(String body) throws Exception {
		MvcTestResult result = mvc.post()
			.uri("/api/clients")
			.with(receptionist())
			.contentType(MediaType.APPLICATION_JSON)
			.content(body)
			.exchange();
		assertThat(result).hasStatus(HttpStatus.CREATED);
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

}
