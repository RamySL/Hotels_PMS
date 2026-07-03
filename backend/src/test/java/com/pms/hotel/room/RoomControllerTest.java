package com.pms.hotel.room;

import static com.pms.hotel.TestAuthentication.admin;
import static com.pms.hotel.TestAuthentication.receptionist;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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
import com.pms.hotel.config.SecurityConfig;

@WebMvcTest(RoomController.class)
@Import(SecurityConfig.class)
class RoomControllerTest {

	private static final UUID TYPE_ID = UUID.randomUUID();

	private static final String VALID_BODY = """
			{"roomTypeId": "%s", "roomNumber": "101", "floor": 1}
			""".formatted(TYPE_ID);

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private RoomService roomService;

	@Test
	void listRequiresAuthentication() {
		assertThat(mvc.get().uri("/api/rooms")).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void receptionistListsRoomsWithTheirType() {
		when(roomService.findAll(null)).thenReturn(List.of(room101(RoomStatus.READY)));

		assertThat(mvc.get().uri("/api/rooms").with(receptionist())).hasStatusOk().bodyJson().isLenientlyEqualTo("""
				[{"roomNumber": "101", "floor": 1, "status": "READY", "roomType": {"name": "Double"}}]
				""");
	}

	@Test
	void listCanBeFilteredByStatus() {
		when(roomService.findAll(RoomStatus.CLEANING)).thenReturn(List.of());

		assertThat(mvc.get().uri("/api/rooms?status=CLEANING").with(receptionist())).hasStatusOk();
		verify(roomService).findAll(RoomStatus.CLEANING);
	}

	@Test
	void unknownStatusFilterIsABadRequest() {
		assertThat(mvc.get().uri("/api/rooms?status=LIBRE").with(receptionist())).hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	void adminCreatesRoomAndGetsItsLocation() {
		RoomResponse created = room101(RoomStatus.READY);
		when(roomService.create(any())).thenReturn(created);

		assertThat(
				mvc.post().uri("/api/rooms").with(admin()).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
			.hasStatus(HttpStatus.CREATED)
			.hasHeader(HttpHeaders.LOCATION, "http://localhost/api/rooms/" + created.id());
	}

	@Test
	void receptionistCannotCreateOrUpdateRooms() {
		assertThat(mvc.post()
			.uri("/api/rooms")
			.with(receptionist())
			.contentType(MediaType.APPLICATION_JSON)
			.content(VALID_BODY)).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(mvc.put()
			.uri("/api/rooms/{id}", UUID.randomUUID())
			.with(receptionist())
			.contentType(MediaType.APPLICATION_JSON)
			.content(VALID_BODY)).hasStatus(HttpStatus.FORBIDDEN);
		verifyNoInteractions(roomService);
	}

	@Test
	void invalidRoomIsRejectedFieldByField() {
		assertThat(mvc.post().uri("/api/rooms").with(admin()).contentType(MediaType.APPLICATION_JSON).content("""
				{"roomNumber": "", "floor": -1}
				""")).hasStatus(HttpStatus.BAD_REQUEST)
			.bodyJson()
			.extractingPath("$.errors")
			.asMap()
			.containsOnlyKeys("roomTypeId", "roomNumber", "floor");
	}

	@Test
	void receptionistChangesRoomStatus() {
		UUID id = UUID.randomUUID();
		when(roomService.changeStatus(id, RoomStatus.READY)).thenReturn(room101(RoomStatus.READY));

		assertThat(mvc.patch()
			.uri("/api/rooms/{id}/status", id)
			.with(receptionist())
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"status": "READY"}
					""")).hasStatusOk().bodyJson().extractingPath("$.status").isEqualTo("READY");
	}

	@Test
	void forbiddenStatusChangeIsAConflict() {
		UUID id = UUID.randomUUID();
		when(roomService.changeStatus(eq(id), any()))
			.thenThrow(new BusinessRuleException("Le statut OCCUPIED est posé par le check-in, pas manuellement"));

		assertThat(mvc.patch()
			.uri("/api/rooms/{id}/status", id)
			.with(receptionist())
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"status": "OCCUPIED"}
					""")).hasStatus(HttpStatus.CONFLICT);
	}

	@Test
	void unknownStatusValueIsABadRequest() {
		assertThat(mvc.patch()
			.uri("/api/rooms/{id}/status", UUID.randomUUID())
			.with(receptionist())
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"status": "LIBRE"}
					""")).hasStatus(HttpStatus.BAD_REQUEST);
		verifyNoInteractions(roomService);
	}

	private static RoomResponse room101(RoomStatus status) {
		RoomTypeResponse doubleRoom = new RoomTypeResponse(TYPE_ID, "Double", null, new BigDecimal("89.90"), 2);
		return new RoomResponse(UUID.randomUUID(), "101", 1, status, doubleRoom);
	}

}
