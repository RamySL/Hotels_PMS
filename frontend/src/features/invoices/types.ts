export type InvoiceStatus = "UNPAID" | "PARTIALLY_PAID" | "PAID" | "CANCELLED";

export type PaymentMethod = "CASH" | "CARD" | "TRANSFER";

export interface Payment {
  id: string;
  amount: number;
  method: PaymentMethod;
  /** Instant de l'encaissement, en UTC. */
  paidAt: string;
  /** Prénom et nom de la personne qui a encaissé. */
  recordedBy: string;
}

/** Facture d'une réservation, créée avec elle et tenue à jour de son prix. */
export interface Invoice {
  id: string;
  status: InvoiceStatus;
  amountTotal: number;
  amountPaid: number;
  /** Reste à payer : négatif, c'est un trop-perçu. Une facture annulée n'est plus due. */
  balance: number;
  payments: Payment[];
}

/** Corps envoyé pour encaisser un paiement ; null désigne un champ laissé vide. */
export interface PaymentRequest {
  amount: number | null;
  method: PaymentMethod | null;
}
