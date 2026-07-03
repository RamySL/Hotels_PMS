package com.pms.hotel.room;

import static com.pms.hotel.TestAuthentication.admin;
import static com.pms.hotel.TestAuthentication.receptionist;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.pms.hotel.common.BusinessRuleException;
import com.pms.hotel.common.ResourceNotFoundException;
import com.pms.hotel.config.SecurityConfig;

@WebMvcTest(RoomTypeController.class)
@Import(SecurityConfig.class)
class RoomTypeControllerTest {

	private static final String VALID_BODY = """
			{"name": "Suite", "description": "Vue mer", "basePrice": 150.00, "capacity": 2}
			""";

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private RoomTypeService roomTypeService;

	@Test
	void listRequiresAuthentication() {
		assertThat(mvc.get().uri("/api/room-types")).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void receptionistCanListRoomTypes() {
		when(roomTypeService.findAll()).thenReturn(List.of(suite()));

		assertThat(mvc.get().uri("/api/room-types").with(receptionist())).hasStatusOk()
			.bodyJson()
			.isLenientlyEqualTo("""
					[{"name": "Suite", "description": "Vue mer", "basePrice": 150.00, "capacity": 2}]
					""");
	}

	@Test
	void adminCreatesRoomTypeAndGetsItsLocation() {
		RoomTypeResponse created = suite();
		when(roomTypeService.create(any())).thenReturn(created);

		assertThat(mvc.post()
			.uri("/api/room-types")
			.with(admin())
			.contentType(MediaType.APPLICATION_JSON)
			.content(VALID_BODY)).hasStatus(HttpStatus.CREATED)
			.hasHeader(HttpHeaders.LOCATION, "http://localhost/api/room-types/" + created.id())
			.bodyJson()
			.extractingPath("$.id")
			.isEqualTo(created.id().toString());
	}

	@Test
	void receptionistCannotCreateOrUpdateRoomTypes() {
		assertThat(mvc.post()
			.uri("/api/room-types")
			.with(receptionist())
			.contentType(MediaType.APPLICATION_JSON)
			.content(VALID_BODY)).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(mvc.put()
			.uri("/api/room-types/{id}", UUID.randomUUID())
			.with(receptionist())
			.contentType(MediaType.APPLICATION_JSON)
			.content(VALID_BODY)).hasStatus(HttpStatus.FORBIDDEN);
		verifyNoInteractions(roomTypeService);
	}

	@Test
	void invalidRoomTypeIsRejectedFieldByField() {
		assertThat(mvc.post().uri("/api/room-types").with(admin()).contentType(MediaType.APPLICATION_JSON).content("""
				{"name": " ", "basePrice": 12.345, "capacity": 0}
				""")).hasStatus(HttpStatus.BAD_REQUEST)
			.bodyJson()
			.extractingPath("$.errors")
			.asMap()
			.containsOnlyKeys("name", "basePrice", "capacity");
		verifyNoInteractions(roomTypeService);
	}

	@Test
	void existingNameIsAConflict() {
		when(roomTypeService.create(any()))
			.thenThrow(new BusinessRuleException("Un type de chambre nommé « Suite » existe déjà"));

		assertThat(mvc.post()
			.uri("/api/room-types")
			.with(admin())
			.contentType(MediaType.APPLICATION_JSON)
			.content(VALID_BODY)).hasStatus(HttpStatus.CONFLICT);
	}

	@Test
	void updateOfUnknownRoomTypeIsNotFound() {
		UUID id = UUID.randomUUID();
		when(roomTypeService.update(eq(id), any())).thenThrow(new ResourceNotFoundException("Type de chambre", id));

		assertThat(mvc.put()
			.uri("/api/room-types/{id}", id)
			.with(admin())
			.contentType(MediaType.APPLICATION_JSON)
			.content(VALID_BODY)).hasStatus(HttpStatus.NOT_FOUND);
	}

	private static RoomTypeResponse suite() {
		return new RoomTypeResponse(UUID.randomUUID(), "Suite", "Vue mer", new BigDecimal("150.00"), 2);
	}

}
