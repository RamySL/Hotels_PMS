package com.pms.hotel.dashboard;

import static com.pms.hotel.TestAuthentication.receptionist;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.pms.hotel.config.SecurityConfig;
import com.pms.hotel.dashboard.DashboardResponse.Stay;

@WebMvcTest(DashboardController.class)
@Import(SecurityConfig.class)
class DashboardControllerTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 10, 10);

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private DashboardService dashboardService;

	@Test
	void requiresAuthentication() {
		assertThat(mvc.get().uri("/api/dashboard/today")).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void showsTheStaysOfTheDayWithWhatIsLeftToPay() {
		Stay arrival = new Stay(UUID.randomUUID(), "Jean Dupont", "101", TODAY, TODAY.plusDays(2), 2,
				new BigDecimal("129.80"));
		when(dashboardService.today()).thenReturn(new DashboardResponse(TODAY, List.of(arrival), List.of()));

		assertThat(mvc.get().uri("/api/dashboard/today").with(receptionist())).hasStatusOk()
			.bodyJson()
			.isLenientlyEqualTo("""
					{"date": "2026-10-10",
					 "arrivals": [{"reservationId": "%s", "clientName": "Jean Dupont", "roomNumber": "101",
					               "checkInDate": "2026-10-10", "checkOutDate": "2026-10-12",
					               "numberOfGuests": 2, "balance": 129.80}],
					 "departures": []}
					""".formatted(arrival.reservationId()));
	}

}
