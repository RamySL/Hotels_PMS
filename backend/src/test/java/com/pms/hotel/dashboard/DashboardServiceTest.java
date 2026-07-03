package com.pms.hotel.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.pms.hotel.invoice.InvoiceRepository;
import com.pms.hotel.reservation.ReservationRepository;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

	private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

	@Mock
	private ReservationRepository reservationRepository;

	@Mock
	private InvoiceRepository invoiceRepository;

	@Test
	void theDayIsTheHotelDayEvenJustAfterMidnight() {
		// 23h30 UTC le 14 janvier = 0h30 à Paris le 15 : la journée du 15 a commencé à l'hôtel
		DashboardService service = serviceAt(Instant.parse("2030-01-14T23:30:00Z"));
		LocalDate january15 = LocalDate.of(2030, 1, 15);

		assertThat(service.today().date()).isEqualTo(january15);
		verify(reservationRepository).findExpectedArrivals(january15);
		verify(reservationRepository).findExpectedDepartures(january15);
	}

	@Test
	void nothingToDoReadsNoInvoice() {
		DashboardResponse dashboard = serviceAt(Instant.parse("2030-01-15T10:00:00Z")).today();

		assertThat(dashboard.arrivals()).isEmpty();
		assertThat(dashboard.departures()).isEmpty();
		verifyNoInteractions(invoiceRepository);
	}

	private DashboardService serviceAt(Instant instant) {
		return new DashboardService(reservationRepository, invoiceRepository, Clock.fixed(instant, PARIS));
	}

}
