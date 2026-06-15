package com.pms.hotel.reservation;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

/** Critères de recherche des réservations, combinables avec Specification.allOf(...). */
final class ReservationSpecifications {

	private ReservationSpecifications() {
	}

	/** Réservations qui retiennent la chambre (CONFIRMED, CHECKED_IN). */
	static Specification<Reservation> active() {
		return (root, query, cb) -> root.get("status").in(ReservationStatus.ACTIVE);
	}

	static Specification<Reservation> forRoom(UUID roomId) {
		return (root, query, cb) -> cb.equal(root.get("room").get("id"), roomId);
	}

	/**
	 * Réservations dont le séjour chevauche la période [checkIn, checkOut). Les comparaisons
	 * sont strictes : un départ et une arrivée le même jour ne partagent aucune nuit, comme
	 * dans la contrainte ex_reservations_no_overlap.
	 */
	static Specification<Reservation> overlapping(LocalDate checkIn, LocalDate checkOut) {
		return (root, query, cb) -> cb.and(cb.lessThan(root.get("checkInDate"), checkOut),
				cb.greaterThan(root.get("checkOutDate"), checkIn));
	}

	/** Écarte la réservation en cours de modification ; sans effet si id est null (création). */
	static Specification<Reservation> excluding(UUID id) {
		return (root, query, cb) -> id == null ? cb.conjunction() : cb.notEqual(root.get("id"), id);
	}

}
