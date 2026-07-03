package com.pms.hotel.reservation;

import static com.pms.hotel.TestAuthentication.RECEPTIONIST_ID;
import static com.pms.hotel.TestAuthentication.receptionist;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.pms.hotel.common.BusinessRuleException;
import com.pms.hotel.common.ResourceNotFoundException;
import com.pms.hotel.config.SecurityConfig;

@WebMvcTest(ReservationController.class)
@Import(SecurityConfig.class)
class ReservationControllerTest {

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private ReservationService reservationService;

	@Test
	void listRequiresAuthentication() {
		assertThat(mvc.get().uri("/api/reservations")).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void receptionistBooksAndIsRecordedAsTheAuthor() {
		ReservationResponse created = sample();
		when(reservationService.create(any(), eq(RECEPTIONIST_ID))).thenReturn(created);

		assertThat(post(body("2026-10-10", "2026-10-13"))).hasStatus(HttpStatus.CREATED)
			.hasHeader(HttpHeaders.LOCATION, "http://localhost/api/reservations/" + created.id());
	}

	@Test
	void departureMustFollowArrival() {
		assertThat(post(body("2026-10-10", "2026-10-10"))).hasStatus(HttpStatus.BAD_REQUEST)
			.bodyJson()
			.extractingPath("$.errors.checkOutDate")
			.isEqualTo("doit être postérieure à la date d'arrivée");
		verifyNoInteractions(reservationService);
	}

	@Test
	void missingFieldsAreListed() {
		assertThat(post("{}")).hasStatus(HttpStatus.BAD_REQUEST)
			.bodyJson()
			.extractingPath("$.errors")
			.asMap()
			.containsOnlyKeys("clientId", "roomId", "checkInDate", "checkOutDate", "numberOfGuests");
	}

	@Test
	void listFiltersAreReadFromIsoDates() {
		when(reservationService.search(any(), any(), any(), any())).thenReturn(Page.empty());

		assertThat(
				mvc.get().uri("/api/reservations?from=2026-10-10&to=2026-10-17&status=CONFIRMED").with(receptionist()))
			.hasStatusOk();
		verify(reservationService).search(eq(LocalDate.of(2026, 10, 10)), eq(LocalDate.of(2026, 10, 17)),
				eq(ReservationStatus.CONFIRMED), any());
	}

	@Test
	void unknownReservationIsNotFound() {
		UUID id = UUID.randomUUID();
		when(reservationService.findById(id)).thenThrow(new ResourceNotFoundException("Réservation", id));

		assertThat(mvc.get().uri("/api/reservations/{id}", id).with(receptionist())).hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void receptionistChecksInTheGuest() {
		UUID id = UUID.randomUUID();
		when(reservationService.checkIn(id)).thenReturn(sample());

		assertThat(mvc.patch().uri("/api/reservations/{id}/check-in", id).with(receptionist())).hasStatusOk();
	}

	@Test
	void refusedTransitionIsAConflict() {
		UUID id = UUID.randomUUID();
		when(reservationService.checkOut(id))
			.thenThrow(new BusinessRuleException("Seul un client arrivé peut faire l'objet d'un check-out"));

		assertThat(mvc.patch().uri("/api/reservations/{id}/check-out", id).with(receptionist()))
			.hasStatus(HttpStatus.CONFLICT);
	}

	private org.springframework.test.web.servlet.assertj.MvcTestResult post(String body) {
		return mvc.post()
			.uri("/api/reservations")
			.with(receptionist())
			.contentType(MediaType.APPLICATION_JSON)
			.content(body)
			.exchange();
	}

	private static String body(String checkIn, String checkOut) {
		return """
				{"clientId": "%s", "roomId": "%s", "checkInDate": "%s", "checkOutDate": "%s", "numberOfGuests": 2}
				""".formatted(UUID.randomUUID(), UUID.randomUUID(), checkIn, checkOut);
	}

	private static ReservationResponse sample() {
		return new ReservationResponse(UUID.randomUUID(), ReservationStatus.CONFIRMED, LocalDate.of(2026, 10, 10),
				LocalDate.of(2026, 10, 13), 3, 2, new BigDecimal("269.70"),
				new ReservationResponse.ClientSummary(UUID.randomUUID(), "Jean", "Dupont"),
				new ReservationResponse.RoomSummary(UUID.randomUUID(), "101", "Double"),
				new ReservationResponse.UserSummary(RECEPTIONIST_ID, "Lucas", "Bernard"), Instant.EPOCH);
	}

}
