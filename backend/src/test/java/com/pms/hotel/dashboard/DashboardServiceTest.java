package com.pms.hotel.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.pms.hotel.dashboard.DashboardResponse.Occupancy;
import com.pms.hotel.invoice.InvoiceRepository;
import com.pms.hotel.reservation.AvailabilityService;
import com.pms.hotel.reservation.ReservationRepository;
import com.pms.hotel.room.Room;
import com.pms.hotel.room.RoomRepository;
import com.pms.hotel.room.RoomService;
import com.pms.hotel.room.RoomStatus;
import com.pms.hotel.room.RoomType;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

	private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

	// 9h UTC = 10h à Paris
	private static final Instant JANUARY_15_AT_10 = Instant.parse("2030-01-15T09:00:00Z");

	private static final LocalDate JANUARY_15 = LocalDate.of(2030, 1, 15);

	@Mock
	private ReservationRepository reservationRepository;

	@Mock
	private InvoiceRepository invoiceRepository;

	@Mock
	private RoomRepository roomRepository;

	@Mock
	private AvailabilityService availabilityService;

	@Mock
	private RoomService roomService;

	@Test
	void theDayIsTheHotelDayEvenJustAfterMidnight() {
		// 23h30 UTC le 14 janvier = 0h30 à Paris le 15 : la journée du 15 a
		// commencé à l'hôtel
		DashboardService service = serviceAt(Instant.parse("2030-01-14T23:30:00Z"));

		assertThat(service.today().date()).isEqualTo(JANUARY_15);
		verify(reservationRepository).findExpectedArrivals(JANUARY_15);
		verify(reservationRepository).findExpectedDepartures(JANUARY_15);
	}

	@Test
	void nothingToDoReadsNoInvoice() {
		DashboardResponse dashboard = serviceAt(JANUARY_15_AT_10).today();

		assertThat(dashboard.arrivals()).isEmpty();
		assertThat(dashboard.departures()).isEmpty();
		verifyNoInteractions(invoiceRepository);
	}

	@Test
	void occupancyIsTheShareOfRoomsInServiceSoldTonight() {
		when(roomRepository.countByStatusNot(RoomStatus.OUT_OF_SERVICE)).thenReturn(3L);
		Room freeRoom = new Room(new RoomType("Double", null, new BigDecimal("89.90"), 2), "103", 1);
		when(availabilityService.findAvailableRooms(JANUARY_15, JANUARY_15.plusDays(1))).thenReturn(List.of(freeRoom));

		Occupancy occupancy = serviceAt(JANUARY_15_AT_10).today().occupancy();

		assertThat(occupancy.occupiedRooms()).isEqualTo(2);
		assertThat(occupancy.roomsInService()).isEqualTo(3);
		assertThat(occupancy.rate()).isEqualByComparingTo("66.7");
	}

	@Test
	void noRoomInServiceGivesARateOfZero() {
		when(roomRepository.countByStatusNot(RoomStatus.OUT_OF_SERVICE)).thenReturn(0L);

		assertThat(serviceAt(JANUARY_15_AT_10).today().occupancy().rate()).isEqualByComparingTo("0");
	}

	private DashboardService serviceAt(Instant instant) {
		return new DashboardService(reservationRepository, invoiceRepository, roomRepository, availabilityService,
				roomService, Clock.fixed(instant, PARIS));
	}

}
