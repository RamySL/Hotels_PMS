package com.pms.hotel.client;

import static com.pms.hotel.TestAuthentication.receptionist;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.pms.hotel.common.ResourceNotFoundException;
import com.pms.hotel.config.SecurityConfig;

@WebMvcTest(ClientController.class)
@Import(SecurityConfig.class)
class ClientControllerTest {

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private ClientService clientService;

	@Test
	void listRequiresAuthentication() {
		assertThat(mvc.get().uri("/api/clients")).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void receptionistGetsAPageOfClients() {
		when(clientService.search(eq("dupont"), any()))
			.thenReturn(new PageImpl<>(List.of(dupont()), PageRequest.of(0, 20), 1));

		assertThat(mvc.get().uri("/api/clients?search=dupont").with(receptionist())).hasStatusOk()
			.bodyJson()
			.isLenientlyEqualTo("""
					{
					  "content": [{"firstName": "Jean", "lastName": "Dupont", "email": null, "phone": null}],
					  "page": {"size": 20, "number": 0, "totalElements": 1, "totalPages": 1}
					}
					""");
	}

	@Test
	void requestedPageIsPassedAndItsSizeCappedAt100() {
		when(clientService.search(any(), any())).thenReturn(Page.empty());

		assertThat(mvc.get().uri("/api/clients?page=2&size=500").with(receptionist())).hasStatusOk();

		ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
		verify(clientService).search(isNull(), pageable.capture());
		assertThat(pageable.getValue().getPageNumber()).isEqualTo(2);
		assertThat(pageable.getValue().getPageSize()).isEqualTo(100);
	}

	@Test
	void receptionistRegistersWalkInClient() {
		ClientResponse created = dupont();
		when(clientService.create(any())).thenReturn(created);

		assertThat(
				mvc.post().uri("/api/clients").with(receptionist()).contentType(MediaType.APPLICATION_JSON).content("""
						{"firstName": "Jean", "lastName": "Dupont"}
						"""))
			.hasStatus(HttpStatus.CREATED)
			.hasHeader(HttpHeaders.LOCATION, "http://localhost/api/clients/" + created.id());
	}

	@Test
	void invalidClientIsRejectedFieldByField() {
		assertThat(
				mvc.post().uri("/api/clients").with(receptionist()).contentType(MediaType.APPLICATION_JSON).content("""
						{"firstName": " ", "lastName": "", "email": "pas-un-email", "phone": "06 12 AB"}
						"""))
			.hasStatus(HttpStatus.BAD_REQUEST)
			.bodyJson()
			.extractingPath("$.errors")
			.asMap()
			.containsOnlyKeys("firstName", "lastName", "email", "phone")
			.containsEntry("phone", "ne doit contenir que des chiffres, des espaces et + . ( ) -");
		verifyNoInteractions(clientService);
	}

	@Test
	void unknownClientIsNotFound() {
		UUID id = UUID.randomUUID();
		when(clientService.findById(id)).thenThrow(new ResourceNotFoundException("Client", id));

		assertThat(mvc.get().uri("/api/clients/{id}", id).with(receptionist())).hasStatus(HttpStatus.NOT_FOUND);
	}

	private static ClientResponse dupont() {
		return new ClientResponse(UUID.randomUUID(), "Jean", "Dupont", null, null);
	}

}
