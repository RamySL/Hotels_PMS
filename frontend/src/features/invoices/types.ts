export type InvoiceStatus = "UNPAID" | "PARTIALLY_PAID" | "PAID" | "CANCELLED";

/** Facture d'une réservation, créée avec elle et tenue à jour de son prix. */
export interface Invoice {
  id: string;
  status: InvoiceStatus;
  amountTotal: number;
  amountPaid: number;
  /** Reste à payer : négatif, c'est un trop-perçu. Une facture annulée n'est plus due. */
  balance: number;
}
