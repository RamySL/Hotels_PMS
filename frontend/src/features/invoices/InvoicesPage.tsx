import { Link, useSearchParams } from "react-router";
import { Alert } from "@/shared/components/Alert";
import { Pagination } from "@/shared/components/Pagination";
import { SelectField } from "@/shared/components/SelectField";
import { useApiData } from "@/shared/hooks/useApiData";
import { formatDate, formatShortDate } from "@/shared/lib/dates";
import { formatEuros } from "@/shared/lib/format";
import { pageFromAddress, type Page } from "@/shared/lib/page";
import { formatBalance } from "./balance";
import {
  INVOICE_STATUS_LABEL,
  INVOICE_STATUSES,
  isInvoiceStatus,
} from "./invoiceStatus";
import { InvoiceStatusBadge } from "./InvoiceStatusBadge";
import type { Invoice, InvoiceStatus } from "./types";

const PAGE_SIZE = 20;

const FILTER_OPTIONS = [
  { value: "", label: "Tous les statuts" },
  ...INVOICE_STATUSES.map((status) => ({
    value: status,
    label: INVOICE_STATUS_LABEL[status],
  })),
];

/**
 * Factures, les plus récentes d'abord. Le filtre et la page vivent dans l'adresse
 * (?statut=UNPAID&page=2). L'encaissement se fait sur la fiche de la réservation.
 */
export function InvoicesPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const statusParam = searchParams.get("statut");
  const status = isInvoiceStatus(statusParam) ? statusParam : null;
  const page = pageFromAddress(searchParams.get("page"));
  const invoices = useApiData<Page<Invoice>>(invoicesPath(status, page));

  function goToPage(target: number) {
    const params: Record<string, string> = {};
    if (status) {
      params.statut = status;
    }
    if (target > 1) {
      params.page = String(target);
    }
    setSearchParams(params);
  }

  return (
    <>
      <h1 className="text-2xl font-bold text-slate-900">Factures</h1>

      <SelectField
        label="Statut"
        options={FILTER_OPTIONS}
        value={status ?? ""}
        // Un autre filtre repart de la première page
        onChange={(event) =>
          setSearchParams(
            event.target.value ? { statut: event.target.value } : {},
          )
        }
        className="mt-6 max-w-xs"
      />

      {invoices.error && (
        <Alert tone="error" className="mt-4">
          {invoices.error.message}
        </Alert>
      )}

      {invoices.data === undefined ? (
        invoices.loading && <p className="mt-6 text-slate-500">Chargement…</p>
      ) : invoices.data.content.length === 0 ? (
        <p className="mt-6 text-slate-500">
          {status ? "Aucune facture dans ce statut." : "Aucune facture."}
        </p>
      ) : (
        <>
          <table className="mt-6 w-full border-collapse overflow-hidden rounded-lg bg-white text-left text-sm shadow-sm">
            <thead className="border-b border-slate-200 text-slate-600">
              <tr>
                <th className="px-4 py-3 font-medium">Émise le</th>
                <th className="px-4 py-3 font-medium">Client</th>
                <th className="px-4 py-3 font-medium">Chambre</th>
                <th className="px-4 py-3 font-medium">Séjour</th>
                <th className="px-4 py-3 text-right font-medium">Total</th>
                <th className="px-4 py-3 text-right font-medium">
                  Reste à payer
                </th>
                <th className="px-4 py-3 font-medium">Statut</th>
              </tr>
            </thead>
            <tbody>
              {invoices.data.content.map((invoice) => (
                <tr
                  key={invoice.id}
                  className="border-b border-slate-100 last:border-0"
                >
                  <td className="px-4 py-3 text-slate-700">
                    {formatDate(new Date(invoice.issuedAt))}
                  </td>
                  <td className="px-4 py-3">
                    {/* La fiche de la réservation porte la facture et son encaissement */}
                    <Link
                      to={`/reservations/${invoice.reservation.id}`}
                      className="font-medium text-blue-700 hover:underline"
                    >
                      {invoice.reservation.clientName}
                    </Link>
                  </td>
                  <td className="px-4 py-3 text-slate-700">
                    {invoice.reservation.roomNumber}
                  </td>
                  <td className="px-4 py-3 text-slate-700">
                    {formatShortDate(invoice.reservation.checkInDate)} →{" "}
                    {formatShortDate(invoice.reservation.checkOutDate)}
                  </td>
                  <td className="px-4 py-3 text-right text-slate-700">
                    {formatEuros(invoice.amountTotal)}
                  </td>
                  <td className="px-4 py-3 text-right font-medium text-slate-900">
                    {formatBalance(invoice.balance)}
                  </td>
                  <td className="px-4 py-3">
                    <InvoiceStatusBadge status={invoice.status} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          <Pagination
            page={page}
            info={invoices.data.page}
            noun={["facture", "factures"]}
            onChange={goToPage}
          />
        </>
      )}
    </>
  );
}

/** Adresse de l'API : sa pagination part de 0, celle de l'adresse du navigateur de 1. */
function invoicesPath(status: InvoiceStatus | null, page: number): string {
  const params = new URLSearchParams();
  if (status) {
    params.set("status", status);
  }
  params.set("page", String(page - 1));
  params.set("size", String(PAGE_SIZE));
  return `/invoices?${params}`;
}
