import type { InvoiceStatus } from "./types";

export const INVOICE_STATUS_LABEL: Record<InvoiceStatus, string> = {
  UNPAID: "Non payée",
  PARTIALLY_PAID: "Payée en partie",
  PAID: "Soldée",
  CANCELLED: "Annulée",
};

export const INVOICE_STATUSES = Object.keys(
  INVOICE_STATUS_LABEL,
) as InvoiceStatus[];

export function isInvoiceStatus(value: string | null): value is InvoiceStatus {
  return value !== null && value in INVOICE_STATUS_LABEL;
}

export const INVOICE_STATUS_CLASS: Record<InvoiceStatus, string> = {
  UNPAID: "bg-red-100 text-red-800",
  PARTIALLY_PAID: "bg-amber-100 text-amber-800",
  PAID: "bg-green-100 text-green-800",
  CANCELLED: "bg-slate-100 text-slate-700",
};
