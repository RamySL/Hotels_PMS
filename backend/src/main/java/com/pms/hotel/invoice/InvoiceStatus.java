package com.pms.hotel.invoice;

public enum InvoiceStatus {

	/** Rien n'est encore payé. */
	UNPAID,

	/** Au moins un paiement, mais il reste un solde. */
	PARTIALLY_PAID,

	/** Le montant payé couvre le total, avec éventuellement un trop-perçu. */
	PAID,

	/** La réservation a été annulée : la facture n'est plus due. */
	CANCELLED

}
