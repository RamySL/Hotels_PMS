import { useId, useState, type ReactNode } from "react";
import { Link } from "react-router";
import { formatBalance } from "@/features/invoices/balance";
import { changeRoomStatus } from "@/features/rooms/api";
import type { Room } from "@/features/rooms/types";
import { Alert } from "@/shared/components/Alert";
import { Button } from "@/shared/components/Button";
import { useApiData } from "@/shared/hooks/useApiData";
import { ApiError } from "@/shared/lib/apiClient";
import { formatLongDate, formatShortDate } from "@/shared/lib/dates";
import { formatEuros, formatPercent } from "@/shared/lib/format";
import type { Dashboard, DayStay } from "./types";

/**
 * Tableau de bord du jour, en page d'accueil : ce que la réception a encore à faire
 * (arrivées, départs, ménage) et l'occupation de la nuit. Les arrivées et les départs se
 * traitent sur leur fiche ; seul le ménage se termine ici.
 */
export function DashboardPage() {
  const dashboard = useApiData<Dashboard>("/dashboard/today");
  const day = dashboard.data;

  return (
    <>
      <h1 className="text-2xl font-bold text-slate-900">Aujourd'hui</h1>
      {day && (
        <p className="mt-1 text-slate-600 first-letter:uppercase">
          {formatLongDate(day.date)}
        </p>
      )}

      {dashboard.error && (
        <Alert tone="error" className="mt-4">
          {dashboard.error.message}
        </Alert>
      )}

      {day === undefined ? (
        dashboard.loading && <p className="mt-6 text-slate-500">Chargement…</p>
      ) : (
        <>
          <dl className="mt-6 grid grid-cols-2 gap-4 sm:grid-cols-4">
            <Tile label="Arrivées">{day.arrivals.length}</Tile>
            <Tile label="Départs">{day.departures.length}</Tile>
            <Tile
              label="Occupation"
              detail={`${day.occupancy.occupiedRooms} ${day.occupancy.occupiedRooms > 1 ? "chambres" : "chambre"} sur ${day.occupancy.roomsInService} ce soir`}
            >
              {formatPercent(day.occupancy.rate)}
            </Tile>
            <Tile label="À nettoyer">{day.roomsToClean.length}</Tile>
          </dl>

          <DaySection title="Arrivées">
            <StayTable
              stays={day.arrivals}
              isLate={(stay) => stay.checkInDate < day.date}
              empty="Aucune arrivée prévue aujourd'hui."
            />
          </DaySection>
          <DaySection title="Départs">
            {/* Le check-out exige une facture soldée : le reste se voit d'emblée */}
            <StayTable
              stays={day.departures}
              isLate={(stay) => stay.checkOutDate < day.date}
              empty="Aucun départ prévu aujourd'hui."
              balanceDue
            />
          </DaySection>
          <DaySection title="Chambres à nettoyer">
            <RoomsToClean
              rooms={day.roomsToClean}
              onCleaned={dashboard.reload}
            />
          </DaySection>
        </>
      )}
    </>
  );
}

function Tile({
  label,
  detail,
  children,
}: {
  label: string;
  detail?: string;
  children: ReactNode;
}) {
  return (
    <div className="rounded-lg bg-white p-4 shadow-sm">
      <dt className="text-sm text-slate-500">{label}</dt>
      <dd className="mt-1 text-2xl font-semibold text-slate-900">{children}</dd>
      {detail && <dd className="text-sm text-slate-500">{detail}</dd>}
    </div>
  );
}

function DaySection({
  title,
  children,
}: {
  title: string;
  children: ReactNode;
}) {
  const headingId = useId();
  return (
    <section aria-labelledby={headingId} className="mt-8">
      <h2 id={headingId} className="text-lg font-semibold text-slate-900">
        {title}
      </h2>
      <div className="mt-3">{children}</div>
    </section>
  );
}

interface StayTableProps {
  stays: DayStay[];
  /** Arrivée, ou départ, qui aurait dû se faire avant aujourd'hui. */
  isLate: (stay: DayStay) => boolean;
  empty: string;
  /** Départs : un reste à payer est à encaisser avant le check-out. */
  balanceDue?: boolean;
}

/** Séjours à traiter ; le client mène à la fiche, où se font check-in, encaissement et check-out. */
function StayTable({
  stays,
  isLate,
  empty,
  balanceDue = false,
}: StayTableProps) {
  if (stays.length === 0) {
    return <p className="text-sm text-slate-500">{empty}</p>;
  }
  return (
    <table className="w-full border-collapse overflow-hidden rounded-lg bg-white text-left text-sm shadow-sm">
      <thead className="border-b border-slate-200 text-slate-600">
        <tr>
          <th className="px-4 py-3 font-medium">Client</th>
          <th className="px-4 py-3 font-medium">Chambre</th>
          <th className="px-4 py-3 font-medium">Séjour</th>
          <th className="px-4 py-3 font-medium">Personnes</th>
          <th className="px-4 py-3 text-right font-medium">Reste à payer</th>
        </tr>
      </thead>
      <tbody>
        {stays.map((stay) => (
          <tr
            key={stay.reservationId}
            className="border-b border-slate-100 last:border-0"
          >
            <td className="px-4 py-3">
              <Link
                to={`/reservations/${stay.reservationId}`}
                className="font-medium text-blue-700 hover:underline"
              >
                {stay.clientName}
              </Link>
              {isLate(stay) && (
                <>
                  {" "}
                  <span className="ml-1 rounded-full bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-800">
                    en retard
                  </span>
                </>
              )}
            </td>
            <td className="px-4 py-3 text-slate-700">{stay.roomNumber}</td>
            <td className="px-4 py-3 text-slate-700">
              {formatShortDate(stay.checkInDate)} →{" "}
              {formatShortDate(stay.checkOutDate)}
            </td>
            <td className="px-4 py-3 text-slate-700">{stay.numberOfGuests}</td>
            <td className="px-4 py-3 text-right">
              {balanceDue && stay.balance > 0 ? (
                <span className="font-semibold text-red-700">
                  {formatEuros(stay.balance)} à encaisser
                </span>
              ) : (
                <span className="text-slate-700">
                  {formatBalance(stay.balance)}
                </span>
              )}
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

/** Chambres libérées par un check-out : « Ménage terminé » les remet en service. */
function RoomsToClean({
  rooms,
  onCleaned,
}: {
  rooms: Room[];
  onCleaned: () => void;
}) {
  const [pendingRoomId, setPendingRoomId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function markClean(room: Room) {
    setPendingRoomId(room.id);
    setError(null);
    try {
      await changeRoomStatus(room.id, "READY");
      onCleaned();
    } catch (e) {
      setError(
        e instanceof ApiError ? e.message : "Le statut n'a pas pu être changé.",
      );
    } finally {
      setPendingRoomId(null);
    }
  }

  return (
    <>
      {error && (
        <Alert tone="error" className="mb-3">
          {error}
        </Alert>
      )}
      {rooms.length === 0 ? (
        <p className="text-sm text-slate-500">Aucune chambre à nettoyer.</p>
      ) : (
        <ul className="divide-y divide-slate-100 rounded-lg bg-white shadow-sm">
          {rooms.map((room) => (
            <li
              key={room.id}
              className="flex items-center justify-between px-4 py-3 text-sm"
            >
              <span>
                <span className="font-medium text-slate-900">
                  Chambre {room.roomNumber}
                </span>{" "}
                <span className="text-slate-500">{room.roomType.name}</span>
              </span>
              <Button
                variant="secondary"
                size="sm"
                disabled={pendingRoomId === room.id}
                onClick={() => markClean(room)}
                aria-label={`Ménage terminé, chambre ${room.roomNumber}`}
              >
                Ménage terminé
              </Button>
            </li>
          ))}
        </ul>
      )}
    </>
  );
}
