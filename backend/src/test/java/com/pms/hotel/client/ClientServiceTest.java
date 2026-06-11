package com.pms.hotel.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.pms.hotel.common.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

	@Mock
	private ClientRepository clientRepository;

	@InjectMocks
	private ClientService clientService;

	@Test
	void blankSearchListsAllClientsInAlphabeticalOrderWhateverTheRequestedSort() {
		when(clientRepository.findAll(any(Pageable.class)))
			.thenReturn(new PageImpl<>(List.of(new Client("Jean", "Dupont", null, null))));

		Page<ClientResponse> page = clientService.search("  ", PageRequest.of(1, 5, Sort.by("email")));

		assertThat(page.getContent()).extracting(ClientResponse::lastName).containsExactly("Dupont");
		verify(clientRepository).findAll(PageRequest.of(1, 5, Sort.by("lastName", "firstName", "id")));
	}

	@ParameterizedTest
	@CsvSource({ "Hélène, helene", "06 12, 0612" })
	void searchUsesTheNormalizedKey(String term, String key) {
		when(clientRepository.findBySearchTextContaining(eq(key), any())).thenReturn(Page.empty());

		clientService.search(term, PageRequest.of(0, 20));

		verify(clientRepository).findBySearchTextContaining(eq(key), any());
	}

	@Test
	void createsWalkInClientWithoutContact() {
		when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ClientResponse created = clientService.create(new ClientRequest("Jean", "Dupont", null, null));

		assertThat(created.lastName()).isEqualTo("Dupont");
		assertThat(created.email()).isNull();
		assertThat(created.phone()).isNull();
	}

	@Test
	void updateReplacesTheWholeRecord() {
		UUID id = UUID.randomUUID();
		Client client = new Client("Jean", "Dupont", "jean@mail.fr", "06 12");
		when(clientRepository.findById(id)).thenReturn(Optional.of(client));

		clientService.update(id, new ClientRequest("Jean", "Dupont-Martin", null, "06 12"));

		assertThat(client.getLastName()).isEqualTo("Dupont-Martin");
		assertThat(client.getEmail()).as("email omis : effacé").isNull();
	}

	@Test
	void unknownClientIsNotFound() {
		UUID id = UUID.randomUUID();
		when(clientRepository.findById(id)).thenReturn(Optional.empty());

		assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(() -> clientService.findById(id))
			.withMessage("Client introuvable : " + id);
	}

}
