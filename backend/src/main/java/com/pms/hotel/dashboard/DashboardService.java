package com.pms.hotel.dashboard;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pms.hotel.dashboard.DashboardResponse.Occupancy;
import com.pms.hotel.dashboard.DashboardResponse.Stay;
import com.pms.hotel.invoice.Invoice;
import com.pms.hotel.invoice.InvoiceRepository;
import com.pms.hotel.reservation.AvailabilityService;
import com.pms.hotel.reservation.Reservation;
import com.pms.hotel.reservation.ReservationRepository;
import com.pms.hotel.room.RoomRepository;
import com.pms.hotel.room.RoomService;
import com.pms.hotel.room.RoomStatus;

/**
 * Tableau de bord du jour : ce que la réception a encore à faire et l'état de l'hôtel. Il ne
 * définit aucune règle : il rassemble celles des réservations, de la facturation et des chambres.
 */
@Service
public class DashboardService {

	private final ReservationRepository reservationRepository;

	private final InvoiceRepository invoiceRepository;

	private final RoomRepository roomRepository;

	private final AvailabilityService availabilityService;

	private final RoomService roomService;

	private final Clock clock;

	public DashboardService(ReservationRepository reservationRepository, InvoiceRepository invoiceRepository,
			RoomRepository roomRepository, AvailabilityService availabilityService, RoomService roomService,
			Clock clock) {
		this.reservationRepository = reservationRepository;
		this.invoiceRepository = invoiceRepository;
		this.roomRepository = roomRepository;
		this.availabilityService = availabilityService;
		this.roomService = roomService;
		this.clock = clock;
	}

	/** « Aujourd'hui » est la date de l'hôtel, pas celle du serveur. */
	@Transactional(readOnly = true)
	public DashboardResponse today() {
		LocalDate today = LocalDate.now(clock);
		List<Reservation> arrivals = reservationRepository.findExpectedArrivals(today);
		List<Reservation> departures = reservationRepository.findExpectedDepartures(today);
		Map<UUID, BigDecimal> balances = balancesOf(Stream.concat(arrivals.stream(), departures.stream()).toList());
		return new DashboardResponse(today, stays(arrivals, balances), stays(departures, balances), occupancy(today),
				roomService.findAll(RoomStatus.CLEANING));
	}

	/**
	 * Chambres en service vendues pour la nuit de ce soir : celles que la règle de disponibilité
	 * ne propose plus sur [aujourd'hui, demain).
	 */
	private Occupancy occupancy(LocalDate today) {
		long roomsInService = roomRepository.countByStatusNot(RoomStatus.OUT_OF_SERVICE);
		long freeTonight = availabilityService.findAvailableRooms(today, today.plusDays(1)).size();
		return Occupancy.of(roomsInService - freeTonight, roomsInService);
	}

	/** Reste à payer de chaque réservation : leurs factures sont lues en une seule requête. */
	private Map<UUID, BigDecimal> balancesOf(List<Reservation> reservations) {
		if (reservations.isEmpty()) {
			return Map.of();
		}
		return invoiceRepository.findByReservationIdIn(reservations.stream().map(Reservation::getId).toList())
			.stream()
			.collect(Collectors.toMap(invoice -> invoice.getReservation().getId(), Invoice::balance));
	}

	private static List<Stay> stays(List<Reservation> reservations, Map<UUID, BigDecimal> balances) {
		return reservations.stream()
			.map(reservation -> Stay.of(reservation, balanceOf(reservation, balances)))
			.toList();
	}

	// Chaque réservation reçoit sa facture à sa création, dans la même transaction (ADR-007)
	private static BigDecimal balanceOf(Reservation reservation, Map<UUID, BigDecimal> balances) {
		BigDecimal balance = balances.get(reservation.getId());
		if (balance == null) {
			throw new IllegalStateException("Réservation sans facture : " + reservation.getId());
		}
		return balance;
	}

}
