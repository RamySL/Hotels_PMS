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

@Service
public class InvoiceService {

	/** Factures les plus récentes d'abord ; l'id départage pour que les pages restent stables. */
	private static final Sort SORT = Sort.by(Sort.Order.desc("issuedAt"), Sort.Order.asc("id"));

	private final InvoiceRepository invoiceRepository;

	private final Clock clock;

	public InvoiceService(InvoiceRepository invoiceRepository, Clock clock) {
		this.invoiceRepository = invoiceRepository;
		this.clock = clock;
	}

	/** Facture émise à la création de la réservation, dans la même transaction. */
	@Transactional
	public void createFor(Reservation reservation) {
		invoiceRepository.save(new Invoice(reservation, Instant.now(clock)));
	}

	/** Le séjour a changé : la facture suit le nouveau prix. */
	@Transactional
	public void updateAmount(Reservation reservation) {
		findByReservation(reservation).updateTotal(reservation.getTotalPrice());
	}

	@Transactional
	public void cancelFor(Reservation reservation) {
		findByReservation(reservation).cancel();
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
