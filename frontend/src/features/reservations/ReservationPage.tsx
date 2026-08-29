import { useId, useState } from "react";
import { useParams } from "react-router";
import { InvoicePanel } from "@/features/invoices/InvoicePanel";
import type { Invoice } from "@/features/invoices/types";
import { Alert } from "@/shared/components/Alert";
import { Button } from "@/shared/components/Button";
import { ButtonLink } from "@/shared/components/ButtonLink";
import { Detail } from "@/shared/components/Detail";
import { useApiData } from "@/shared/hooks/useApiData";
import { ApiError } from "@/shared/lib/apiClient";
import { formatDate, today } from "@/shared/lib/dates";
import { formatEuros } from "@/shared/lib/format";
import type { Page } from "@/shared/lib/page";
import { cancelReservation, checkIn, checkOut } from "./api";
import { ReservationStatusBadge } from "./ReservationStatusBadge";
import type { Reservation } from "./types";

/** Fiche d'une réservation (/reservations/:id) : le séjour, les actions et la facture. */
export function ReservationPage() {
  // La route /reservations/:id fournit toujours l'id
  const id = useParams().id!;
  const reservation = useApiData<Reservation>(`/reservations/${id}`);
  const invoices = useApiData<Page<Invoice>>(`/invoices?reservationId=${id}`);

  if (reservation.error) {
    return reservation.error.status === 404 ? (
      <p className="text-slate-600">Cette réservation n'existe pas.</p>
    ) : (
      <Alert tone="error">{reservation.error.message}</Alert>
    );
  }
  // La facture décide du check-out : la fiche attend aussi sa réponse
  if (
    reservation.data === undefined ||
    (invoices.data === undefined && !invoices.error)
  ) {
    return <p className="text-slate-500">Chargement…</p>;
  }

  const stay = reservation.data;
  const invoice = invoices.data?.content[0];

  function reload() {
    reservation.reload();
    invoices.reload();
  }

  return (
    <>
      <div className="flex flex-wrap items-center gap-3">
        <h1 className="text-2xl font-bold text-slate-900">
          {stay.client.firstName} {stay.client.lastName}
        </h1>
        <ReservationStatusBadge status={stay.status} />
      </div>
      <p className="mt-1 text-slate-600">
        Chambre {stay.room.roomNumber} · {stay.room.roomTypeName}
      </p>

      {invoices.error && (
        <Alert tone="error" className="mt-4">
          {invoices.error.message}
        </Alert>
      )}

      <dl className="mt-6 grid max-w-2xl grid-cols-2 gap-x-8 gap-y-4 rounded-lg bg-white p-6 shadow-sm sm:grid-cols-3">
        <Detail label="Arrivée">{formatDate(stay.checkInDate)}</Detail>
        <Detail label="Départ">{formatDate(stay.checkOutDate)}</Detail>
        <Detail label="Durée">
          {stay.nights} {stay.nights > 1 ? "nuits" : "nuit"}
        </Detail>
        <Detail label="Personnes">{stay.numberOfGuests}</Detail>
        <Detail label="Prix du séjour">{formatEuros(stay.totalPrice)}</Detail>
      </dl>
      <p className="mt-3 text-sm text-slate-500">
        Réservée le {formatDate(new Date(stay.createdAt))} par{" "}
        {stay.createdBy.firstName} {stay.createdBy.lastName}
      </p>

      <ReservationActions
        reservation={stay}
        invoice={invoice}
        busy={reservation.loading || invoices.loading}
        onChange={reload}
      />

      {invoice && <InvoicePanel invoice={invoice} reload={invoices.reload} />}
    </>
  );
}

interface ReservationActionsProps {
  reservation: Reservation;
  invoice: Invoice | undefined;
  /** Fiche en cours de rechargement : les actions attendent les données à jour. */
  busy: boolean;
  onChange: () => void;
}

/**
 * Check-in ou annulation avant le séjour, check-out pendant, modification dans les deux
 * cas ; plus rien ensuite. Les règles affichées ici guident la réception, mais c'est le
 * backend qui tranche.
 */
function ReservationActions({
  reservation,
  invoice,
  busy,
  onChange,
}: ReservationActionsProps) {
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [confirmingCancel, setConfirmingCancel] = useState(false);
  const hintId = useId();

  if (
    reservation.status === "CHECKED_OUT" ||
    reservation.status === "CANCELLED"
  ) {
    return null;
  }

  async function run(action: (id: string) => Promise<unknown>) {
    setPending(true);
    setError(null);
    try {
      await action(reservation.id);
      setConfirmingCancel(false);
      onChange();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "L'action n'a pas abouti.");
    } finally {
      setPending(false);
    }
  }

  const disabled = pending || busy;
  const main =
    reservation.status === "CONFIRMED"
      ? {
          label: "Check-in",
          action: checkIn,
          blocker: checkInBlocker(reservation),
        }
      : {
          label: "Check-out",
          action: checkOut,
          blocker: checkOutBlocker(invoice),
        };

  return (
    <div className="mt-6 max-w-2xl">
      {error && (
        <Alert tone="error" className="mb-4">
          {error}
        </Alert>
      )}
      {confirmingCancel ? (
        <div className="rounded-lg border border-red-200 bg-red-50 p-4">
          <p className="text-sm text-red-900">
            Annuler cette réservation ? La chambre {reservation.room.roomNumber}{" "}
            sera libérée pour ces dates, et la facture annulée.
          </p>
          <div className="mt-3 flex gap-3">
            <Button
              variant="danger"
              size="sm"
              disabled={disabled}
              onClick={() => run(cancelReservation)}
            >
              Confirmer l'annulation
            </Button>
            <Button
              variant="secondary"
              size="sm"
              disabled={disabled}
              autoFocus
              onClick={() => setConfirmingCancel(false)}
            >
              Garder la réservation
            </Button>
          </div>
        </div>
      ) : (
        <>
          <div className="flex flex-wrap gap-3">
            <Button
              disabled={disabled || main.blocker !== null}
              aria-describedby={main.blocker ? hintId : undefined}
              onClick={() => run(main.action)}
            >
              {main.label}
            </Button>
            <ButtonLink
              to={`/reservations/${reservation.id}/modifier`}
              variant="secondary"
            >
              Modifier
            </ButtonLink>
            {reservation.status === "CONFIRMED" && (
              <Button
                variant="secondary"
                disabled={disabled}
                onClick={() => setConfirmingCancel(true)}
              >
                Annuler la réservation
              </Button>
            )}
          </div>
          {main.blocker && (
            <p id={hintId} className="mt-2 text-sm text-slate-600">
              {main.blocker}
            </p>
          )}
        </>
      )}
    </div>
  );
}

/** Le check-in se fait pendant le séjour prévu : de l'arrivée à la veille du départ. */
function checkInBlocker(reservation: Reservation): string | null {
  // Au format de l'API, l'ordre des dates est celui du texte
  const day = today();
  if (day < reservation.checkInDate) {
    return `Le check-in sera possible à partir du ${formatDate(reservation.checkInDate)}.`;
  }
  if (day >= reservation.checkOutDate) {
    return "La date de départ est passée : le check-in n'est plus possible.";
  }
  return null;
}

/** Le check-out exige une facture soldée. */
function checkOutBlocker(invoice: Invoice | undefined): string | null {
  if (invoice === undefined) {
    return "Le reste à payer n'a pas pu être vérifié.";
  }
  if (invoice.balance > 0) {
    return `Il reste ${formatEuros(invoice.balance)} à payer : le check-out sera possible une fois la facture soldée.`;
  }
  return null;
}
