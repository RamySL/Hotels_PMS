package com.pms.hotel.reservation;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Une réservation est confirmée dès sa création. Seules CONFIRMED et CHECKED_IN
 * retiennent la chambre sur la période.
 */
public enum ReservationStatus {

	/** Enregistrée : la chambre est retenue sur la période. */
	CONFIRMED,

	/** Le client est arrivé : la chambre est OCCUPIED. */
	CHECKED_IN,

	/** Le client est parti : la réservation ne retient plus la chambre. */
	CHECKED_OUT,

	/** Annulée : la réservation ne retient plus la chambre. */
	CANCELLED;

	/**
	 * Statuts qui retiennent la chambre : les mêmes que dans ex_reservations_no_overlap.
	 */
	public static final Set<ReservationStatus> ACTIVE = Collections.unmodifiableSet(EnumSet.of(CONFIRMED, CHECKED_IN));

}
