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

import com.pms.hotel.dashboard.DashboardResponse.Stay;
import com.pms.hotel.invoice.Invoice;
import com.pms.hotel.invoice.InvoiceRepository;
import com.pms.hotel.reservation.Reservation;
import com.pms.hotel.reservation.ReservationRepository;

/**
 * Tableau de bord du jour : ce que la réception a encore à faire. Il ne définit aucune règle : il
 * rassemble celles des réservations et de la facturation.
 */
@Service
public class DashboardService {

	private final ReservationRepository reservationRepository;

	private final InvoiceRepository invoiceRepository;

	private final Clock clock;

	public DashboardService(ReservationRepository reservationRepository, InvoiceRepository invoiceRepository,
			Clock clock) {
		this.reservationRepository = reservationRepository;
		this.invoiceRepository = invoiceRepository;
		this.clock = clock;
	}

	/** « Aujourd'hui » est la date de l'hôtel, pas celle du serveur. */
	@Transactional(readOnly = true)
	public DashboardResponse today() {
		LocalDate today = LocalDate.now(clock);
		List<Reservation> arrivals = reservationRepository.findExpectedArrivals(today);
		List<Reservation> departures = reservationRepository.findExpectedDepartures(today);
		Map<UUID, BigDecimal> balances = balancesOf(Stream.concat(arrivals.stream(), departures.stream()).toList());
		return new DashboardResponse(today, stays(arrivals, balances), stays(departures, balances));
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
