package com.pms.hotel.room;

/**
 * État physique de la chambre aujourd'hui. Il ne dépend pas des réservations : la
 * disponibilité sur une période se calcule à partir des réservations.
 */
public enum RoomStatus {

	/** Libre et propre, prête à accueillir un client. */
	READY,

	/** Un client y séjourne : posé par le check-in, levé par le check-out. */
	OCCUPIED,

	/** Client parti, ménage à faire. */
	CLEANING,

	/** Indisponible (travaux, panne…). */
	OUT_OF_SERVICE

}
