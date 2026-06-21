package com.pms.hotel.reservation;

import static com.pms.hotel.TestAuthentication.receptionist;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.pms.hotel.config.SecurityConfig;

@WebMvcTest(AvailabilityController.class)
@Import(SecurityConfig.class)
class AvailabilityControllerTest {

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private AvailabilityService availabilityService;

	@Test
	void requiresAuthentication() {
		assertThat(mvc.get().uri("/api/rooms/availability?checkIn=2026-10-10&checkOut=2026-10-12"))
			.hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void listsFreeRoomsWithThePriceOfTheStay() {
		when(availabilityService.findAvailableRoomsWithPrice(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 12)))
			.thenReturn(List.of(new AvailableRoomResponse(UUID.randomUUID(), "102", 1, "Double", 2,
					new BigDecimal("89.90"), 2, new BigDecimal("179.80"))));

		assertThat(availability("2026-10-10", "2026-10-12")).hasStatusOk().bodyJson().isLenientlyEqualTo("""
				[{"roomNumber": "102", "roomTypeName": "Double", "capacity": 2,
				  "pricePerNight": 89.90, "nights": 2, "totalPrice": 179.80}]
				""");
	}

	@ParameterizedTest
	@CsvSource({ "2026-10-12, 2026-10-12", "2026-10-12, 2026-10-10" })
	void departureMustFollowArrival(String checkIn, String checkOut) {
		assertThat(availability(checkIn, checkOut)).hasStatus(HttpStatus.BAD_REQUEST)
			.bodyJson()
			.extractingPath("$.detail")
			.isEqualTo("La date de départ doit être postérieure à la date d'arrivée");
		verifyNoInteractions(availabilityService);
	}

	@Test
	void bothDatesAreRequired() {
		assertThat(mvc.get().uri("/api/rooms/availability?checkIn=2026-10-10").with(receptionist()))
			.hasStatus(HttpStatus.BAD_REQUEST);
	}

	private MvcTestResult availability(String checkIn, String checkOut) {
		return mvc.get()
			.uri("/api/rooms/availability?checkIn={in}&checkOut={out}", checkIn, checkOut)
			.with(receptionist())
			.exchange();
	}

}
