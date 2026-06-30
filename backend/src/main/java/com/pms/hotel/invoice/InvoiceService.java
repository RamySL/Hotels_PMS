package com.pms.hotel.invoice;

import static com.pms.hotel.invoice.InvoiceSpecifications.forReservation;
import static com.pms.hotel.invoice.InvoiceSpecifications.withStatus;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pms.hotel.common.ResourceNotFoundException;
import com.pms.hotel.reservation.Reservation;
import com.pms.hotel.reservation.ReservationBilling;
import com.pms.hotel.user.UserRepository;

/**
 * Facturation : consultation et encaissement des factures, et implémentation de
 * ReservationBilling pour les réservations (ADR-007).
 */
@Service
public class InvoiceService implements ReservationBilling {

	/** Factures les plus récentes d'abord ; l'id départage pour que les pages restent stables. */
	private static final Sort SORT = Sort.by(Sort.Order.desc("issuedAt"), Sort.Order.asc("id"));

	private final InvoiceRepository invoiceRepository;

	private final UserRepository userRepository;

	private final Clock clock;

	public InvoiceService(InvoiceRepository invoiceRepository, UserRepository userRepository, Clock clock) {
		this.invoiceRepository = invoiceRepository;
		this.userRepository = userRepository;
		this.clock = clock;
	}

	/** Facture émise à la création de la réservation, dans la même transaction. */
	@Override
	@Transactional
	public void reservationBooked(Reservation reservation) {
		invoiceRepository.save(new Invoice(reservation, Instant.now(clock)));
	}

	/** Le séjour a changé : la facture suit le nouveau prix. */
	@Override
	@Transactional
	public void reservationRescheduled(Reservation reservation) {
		findByReservation(reservation).updateTotal(reservation.getTotalPrice());
	}

	@Override
	@Transactional
	public void reservationCancelled(Reservation reservation) {
		findByReservation(reservation).cancel();
	}

	/** Refuse (409) le check-out tant que la facture de la réservation n'est pas soldée. */
	@Override
	@Transactional(readOnly = true)
	public void ensureSettled(Reservation reservation) {
		findByReservation(reservation).ensureSettled();
	}

	/**
	 * Le flush envoie le paiement en base avant de construire la réponse : il reçoit ainsi son
	 * id, et un encaissement concurrent est détecté ici (@Version) plutôt qu'au commit.
	 */
	@Transactional
	public InvoiceResponse recordPayment(UUID invoiceId, PaymentRequest request, UUID userId) {
		Invoice invoice = invoiceRepository.findById(invoiceId)
			.orElseThrow(() -> new ResourceNotFoundException("Facture", invoiceId));
		invoice.recordPayment(request.amount(), request.method(), userRepository.getReferenceById(userId),
				Instant.now(clock));
		invoiceRepository.flush();
		return InvoiceResponse.from(invoice);
	}

	@Transactional(readOnly = true)
	public InvoiceResponse findById(UUID id) {
		return InvoiceResponse.from(
				invoiceRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Facture", id)));
	}

	/** Factures filtrées par réservation et par statut ; chaque filtre est facultatif. */
	@Transactional(readOnly = true)
	public Page<InvoiceResponse> search(UUID reservationId, InvoiceStatus status, Pageable pageable) {
		PageRequest page = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), SORT);
		return invoiceRepository.findAll(Specification.allOf(forReservation(reservationId), withStatus(status)), page)
			.map(InvoiceResponse::from);
	}

	private Invoice findByReservation(Reservation reservation) {
		return invoiceRepository.findByReservationId(reservation.getId())
			.orElseThrow(() -> new IllegalStateException("Réservation sans facture : " + reservation.getId()));
	}

}
