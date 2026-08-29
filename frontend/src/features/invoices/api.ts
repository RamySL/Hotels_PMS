import { api } from "@/shared/lib/apiClient";
import type { Invoice, PaymentRequest } from "./types";

/** Enregistre un paiement ; l'API renvoie la facture à jour. */
export function recordPayment(
  invoiceId: string,
  request: PaymentRequest,
): Promise<Invoice> {
  return api.post<Invoice>(`/invoices/${invoiceId}/payments`, request);
}
