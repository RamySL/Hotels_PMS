package com.pms.hotel.invoice;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID>, JpaSpecificationExecutor<Invoice> {

	Optional<Invoice> findByReservationId(UUID reservationId);

	/** Factures de plusieurs réservations, lues en une seule requête. */
	List<Invoice> findByReservationIdIn(Collection<UUID> reservationIds);

	// La liste affiche la réservation, son client et sa chambre : chargés par
	// jointure. Les paiements, eux, sont chargés par lots
	// (hibernate.default_batch_fetch_size)
	@Override
	@EntityGraph(attributePaths = { "reservation", "reservation.client", "reservation.room" })
	Page<Invoice> findAll(Specification<Invoice> spec, Pageable pageable);

}
