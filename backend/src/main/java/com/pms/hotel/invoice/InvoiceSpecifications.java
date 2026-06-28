package com.pms.hotel.invoice;

import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

/** Filtres facultatifs de la liste des factures : sans effet si la valeur est null. */
final class InvoiceSpecifications {

	private InvoiceSpecifications() {
	}

	static Specification<Invoice> forReservation(UUID reservationId) {
		return (root, query, cb) -> reservationId == null ? cb.conjunction()
				: cb.equal(root.get("reservation").get("id"), reservationId);
	}

	static Specification<Invoice> withStatus(InvoiceStatus status) {
		return (root, query, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
	}

}
