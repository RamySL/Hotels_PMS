import { useId, useState } from "react";
import { Alert } from "@/shared/components/Alert";
import { Button } from "@/shared/components/Button";
import { Detail } from "@/shared/components/Detail";
import { SelectField } from "@/shared/components/SelectField";
import { TextField } from "@/shared/components/TextField";
import { useFormSubmit } from "@/shared/hooks/useFormSubmit";
import { formatDateTime } from "@/shared/lib/dates";
import { toNumberOrNull } from "@/shared/lib/form";
import { formatEuros } from "@/shared/lib/format";
import { recordPayment } from "./api";
import { InvoiceStatusBadge } from "./InvoiceStatusBadge";
import {
  isPaymentMethod,
  PAYMENT_METHOD_LABEL,
  PAYMENT_METHODS,
} from "./paymentMethod";
import type { Invoice } from "./types";

const METHOD_OPTIONS = [
  { value: "", label: "Choisir le mode" },
  ...PAYMENT_METHODS.map((method) => ({
    value: method,
    label: PAYMENT_METHOD_LABEL[method],
  })),
];

interface InvoicePanelProps {
  invoice: Invoice;
  /** Relit la facture : après un paiement, ou après un refus qui peut venir d'un changement. */
  reload: () => void;
}

/**
 * Facture d'une réservation : montants, paiements reçus et, tant qu'elle est due, le
 * formulaire d'encaissement.
 */
export function InvoicePanel({ invoice, reload }: InvoicePanelProps) {
  const headingId = useId();
  const due =
    invoice.status === "UNPAID" || invoice.status === "PARTIALLY_PAID";

  return (
    <section aria-labelledby={headingId} className="mt-10 max-w-2xl">
      <div className="flex items-center gap-3">
        <h2 id={headingId} className="text-lg font-semibold text-slate-900">
          Facture
        </h2>
        <InvoiceStatusBadge status={invoice.status} />
      </div>

      <dl className="mt-3 grid grid-cols-3 gap-x-8 rounded-lg bg-white p-6 shadow-sm">
        <Detail label="Total">{formatEuros(invoice.amountTotal)}</Detail>
        <Detail label="Payé">{formatEuros(invoice.amountPaid)}</Detail>
        {invoice.balance < 0 ? (
          <Detail label="Trop-perçu">{formatEuros(-invoice.balance)}</Detail>
        ) : (
          <Detail label="Reste à payer">{formatEuros(invoice.balance)}</Detail>
        )}
      </dl>
      {invoice.balance < 0 && (
        <p className="mt-2 text-sm text-amber-800">
          Le trop-perçu est à rembourser au client, en dehors de l'application.
        </p>
      )}

      <h3 className="mt-6 font-medium text-slate-900">Paiements</h3>
      {invoice.payments.length === 0 ? (
        <p className="mt-2 text-sm text-slate-500">
          Aucun paiement pour l'instant.
        </p>
      ) : (
        <table className="mt-2 w-full overflow-hidden rounded-lg bg-white text-left text-sm shadow-sm">
          <thead className="border-b border-slate-200 text-slate-600">
            <tr>
              <th className="px-4 py-2 font-medium">Date</th>
              <th className="px-4 py-2 font-medium">Montant</th>
              <th className="px-4 py-2 font-medium">Mode</th>
              <th className="px-4 py-2 font-medium">Encaissé par</th>
            </tr>
          </thead>
          <tbody>
            {invoice.payments.map((payment) => (
              <tr
                key={payment.id}
                className="border-b border-slate-100 last:border-0"
              >
                <td className="px-4 py-2 text-slate-700">
                  {formatDateTime(payment.paidAt)}
                </td>
                <td className="px-4 py-2 font-medium text-slate-900">
                  {formatEuros(payment.amount)}
                </td>
                <td className="px-4 py-2 text-slate-700">
                  {PAYMENT_METHOD_LABEL[payment.method]}
                </td>
                <td className="px-4 py-2 text-slate-700">
                  {payment.recordedBy}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {due && <PaymentForm invoice={invoice} reload={reload} />}
    </section>
  );
}

/** Encaissement : le montant proposé est le reste à payer, le mode se choisit à chaque fois. */
function PaymentForm({ invoice, reload }: InvoicePanelProps) {
  const [amount, setAmount] = useState(invoice.balance.toFixed(2));
  const [method, setMethod] = useState("");
  // Refusé, le paiement a pu croiser un autre encaissement : la facture est relue
  const form = useFormSubmit({ onError: reload });

  async function save() {
    const updated = await recordPayment(invoice.id, {
      amount: toNumberOrNull(amount),
      method: isPaymentMethod(method) ? method : null,
    });
    setAmount(updated.balance.toFixed(2));
    setMethod("");
    reload();
  }

  return (
    <form
      onSubmit={form.handleSubmit(save)}
      className="mt-6 rounded-lg border border-slate-200 bg-white p-4"
    >
      <h3 className="font-medium text-slate-900">Encaisser</h3>
      {form.error && (
        <Alert tone="error" className="mt-3">
          {form.error}
        </Alert>
      )}
      <div className="mt-3 flex flex-wrap items-start gap-4">
        <TextField
          label="Montant (€)"
          type="number"
          required
          min="0.01"
          step="0.01"
          value={amount}
          onChange={(event) => setAmount(event.target.value)}
          error={form.fieldErrors.amount}
          className="w-40"
        />
        <SelectField
          label="Mode de paiement"
          required
          options={METHOD_OPTIONS}
          value={method}
          onChange={(event) => setMethod(event.target.value)}
          error={form.fieldErrors.method}
        />
        <Button type="submit" disabled={form.saving} className="mt-6">
          {form.saving ? "Encaissement…" : "Encaisser"}
        </Button>
      </div>
    </form>
  );
}
