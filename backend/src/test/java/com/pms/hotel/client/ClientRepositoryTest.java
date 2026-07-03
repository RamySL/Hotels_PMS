package com.pms.hotel.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.pms.hotel.TestcontainersConfiguration;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
class ClientRepositoryTest {

	@Autowired
	private ClientRepository clientRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void walkInClientNeedsOnlyANameAndBlankContactsAreStoredAsNull() {
		Client client = clientRepository.saveAndFlush(new Client("Jean", "Dupont", " ", ""));

		assertThat(row(client)).containsEntry("email", null)
			.containsEntry("phone", null)
			.containsEntry("search_text", "jean dupont");
	}

	@Test
	void storesNormalizedEmailAndSearchText() {
		Client client = clientRepository
			.saveAndFlush(new Client(" Hélène ", "Lefèvre", " Helene.Lefevre@Mail.FR ", "06 12 34 56 78"));

		assertThat(row(client)).containsEntry("first_name", "Hélène")
			.containsEntry("email", "helene.lefevre@mail.fr")
			.containsEntry("phone", "06 12 34 56 78")
			.containsEntry("search_text", "helene lefevre helene.lefevre@mail.fr 0612345678");
	}

	@Test
	void searchIgnoresAccentsAndCase() {
		clientRepository
			.saveAll(List.of(new Client("Hélène", "Lefèvre", null, null), new Client("Jean", "Dupont", null, null)));

		assertThat(
				clientRepository.findBySearchTextContaining(ClientSearch.searchKey("LEFEVRE"), PageRequest.of(0, 20)))
			.extracting(Client::getLastName)
			.containsExactly("Lefèvre");
	}

	@Test
	void percentSignIsSearchedLiterallyNotAsAWildcard() {
		clientRepository.save(new Client("Jean", "Dupont", null, null));

		assertThat(clientRepository.findBySearchTextContaining("%", PageRequest.of(0, 20))).isEmpty();
	}

	private Map<String, Object> row(Client client) {
		return jdbcTemplate.queryForMap("SELECT first_name, email, phone, search_text FROM clients WHERE id = ?",
				client.getId());
	}

}
