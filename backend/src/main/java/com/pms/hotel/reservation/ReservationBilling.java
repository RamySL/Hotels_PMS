package com.pms.hotel.reservation;

/**
 * Ce dont les réservations ont besoin de la facturation (ADR-007). Le package invoice
 * l'implémente : les réservations ne dépendent pas de la facturation, et il n'y a pas de
 * dépendance cyclique entre les deux packages.
 */
public interface ReservationBilling {

	/** Émet la facture de la réservation qui vient d'être créée. */
	void reservationBooked(Reservation reservation);

	/** Le séjour a changé : la facture suit le nouveau prix. */
	void reservationRescheduled(Reservation reservation);

	/** La réservation est annulée : sa facture aussi. */
	void reservationCancelled(Reservation reservation);

	/** Refuse (409) le check-out tant que la facture n'est pas soldée. */
	void ensureSettled(Reservation reservation);

}
