import { INVOICE_STATUS_CLASS, INVOICE_STATUS_LABEL } from "./invoiceStatus";
import type { InvoiceStatus } from "./types";

export function InvoiceStatusBadge({ status }: { status: InvoiceStatus }) {
  return (
    <span
      className={`inline-block rounded-full px-2.5 py-0.5 text-xs font-medium ${INVOICE_STATUS_CLASS[status]}`}
    >
      {INVOICE_STATUS_LABEL[status]}
    </span>
  );
}
