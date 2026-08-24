import { Link, useSearchParams } from "react-router";
import type { Room } from "@/features/rooms/types";
import { Alert } from "@/shared/components/Alert";
import { ButtonLink } from "@/shared/components/ButtonLink";
import { useApiData } from "@/shared/hooks/useApiData";
import {
  addDays,
  formatDate,
  formatShortDate,
  formatWeekday,
  isIsoDate,
  parseDate,
  today,
} from "@/shared/lib/dates";
import type { Page } from "@/shared/lib/page";
import { buildPlanning, type PlanningRow, type StayCell } from "./planning";
import {
  RESERVATION_STATUS_CLASS,
  RESERVATION_STATUS_LABEL,
} from "./reservationStatus";
import { ReservationStatusBadge } from "./ReservationStatusBadge";
import type { Reservation } from "./types";

const DAYS = 14;
/** Une seule page de l'API, à sa taille maximale : assez pour deux semaines d'un petit hôtel. */
const MAX_RESERVATIONS = 100;

/** Planning des chambres (/reservations) : deux semaines à partir de ?du=, aujourd'hui par défaut. */
export function PlanningPage() {
  const [searchParams] = useSearchParams();
  const start = searchParams.get("du");
  const currentDay = today();
  const from = isIsoDate(start) ? start : currentDay;
  const days = Array.from({ length: DAYS }, (_, index) => addDays(from, index));
  const lastDay = days[DAYS - 1];
  const rooms = useApiData<Room[]>("/rooms");
  const reservations = useApiData<Page<Reservation>>(
    `/reservations?from=${from}&to=${addDays(from, DAYS)}&size=${MAX_RESERVATIONS}`,
  );

  const error = rooms.error ?? reservations.error;

  return (
    <>
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Planning</h1>
          <p className="mt-1 text-slate-600">
            Du {formatShortDate(from)} au {formatDate(lastDay)}
          </p>
        </div>
        <nav aria-label="Période du planning" className="flex gap-2">
          <ButtonLink
            to={`/reservations?du=${addDays(from, -7)}`}
            variant="secondary"
            size="sm"
          >
            Semaine précédente
          </ButtonLink>
          <ButtonLink to="/reservations" variant="secondary" size="sm">
            Aujourd'hui
          </ButtonLink>
          <ButtonLink
            to={`/reservations?du=${addDays(from, 7)}`}
            variant="secondary"
            size="sm"
          >
            Semaine suivante
          </ButtonLink>
        </nav>
      </div>

      {error && (
        <Alert tone="error" className="mt-4">
          {error.message}
        </Alert>
      )}
      {reservations.data && reservations.data.page.totalPages > 1 && (
        <Alert tone="notice" className="mt-4">
          La période compte plus de {MAX_RESERVATIONS} réservations : seules les{" "}
          {MAX_RESERVATIONS} premières arrivées sont affichées.
        </Alert>
      )}

      {rooms.data === undefined || reservations.data === undefined ? (
        !error && <p className="mt-6 text-slate-500">Chargement…</p>
      ) : (
        <PlanningTable
          caption={`Occupation des chambres du ${formatShortDate(from)} au ${formatDate(lastDay)}`}
          days={days}
          today={currentDay}
          rows={buildPlanning(
            rooms.data,
            reservations.data.content,
            days,
            currentDay,
          )}
          loading={reservations.loading}
        />
      )}
    </>
  );
}

interface PlanningTableProps {
  caption: string;
  days: string[];
  today: string;
  rows: PlanningRow[];
  /** Une autre période se charge : la grille montre encore les séjours déjà reçus. */
  loading: boolean;
}

/** Une ligne par chambre, une colonne par nuit ; un séjour s'étend sur ses nuits (colSpan). */
function PlanningTable({
  caption,
  days,
  today,
  rows,
  loading,
}: PlanningTableProps) {
  return (
    <>
      <div className="mt-6 overflow-x-auto rounded-lg bg-white shadow-sm">
        <table
          aria-busy={loading}
          className={`w-full min-w-3xl table-fixed border-collapse text-sm ${loading ? "opacity-60" : ""}`}
        >
          <caption className="sr-only">{caption}</caption>
          <thead>
            <tr className="border-b border-slate-200 text-slate-600">
              <th scope="col" className="w-28 px-3 py-2 text-left font-medium">
                Chambre
              </th>
              {days.map((day) => (
                <th
                  key={day}
                  scope="col"
                  aria-current={day === today ? "date" : undefined}
                  className={`px-1 py-2 text-center font-medium ${day === today ? "bg-blue-50 text-blue-800" : ""}`}
                >
                  <span className="block text-xs font-normal">
                    {formatWeekday(day)}
                  </span>{" "}
                  {parseDate(day).getDate()}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {rows.map(({ room, cells }) => (
              <tr
                key={room.id}
                className="border-b border-slate-100 last:border-0"
              >
                <th
                  scope="row"
                  className="px-3 py-2 text-left font-medium text-slate-900"
                >
                  {room.roomNumber}{" "}
                  <span className="block text-xs font-normal text-slate-500">
                    {room.roomType.name}
                  </span>
                  {room.status === "OUT_OF_SERVICE" && (
                    <>
                      {" "}
                      <span className="block text-xs font-normal text-red-700">
                        Hors service
                      </span>
                    </>
                  )}
                </th>
                {cells.map((cell) =>
                  cell.kind === "free" ? (
                    <td
                      key={cell.date}
                      className={cell.date === today ? "bg-blue-50" : undefined}
                    />
                  ) : (
                    <td
                      key={cell.reservation.id}
                      colSpan={cell.span}
                      className="px-0.5 py-1.5"
                    >
                      <StayBar stay={cell} />
                    </td>
                  ),
                )}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <p className="mt-3 flex flex-wrap items-center gap-2 text-sm text-slate-600">
        Légende :
        <ReservationStatusBadge status="CONFIRMED" />
        <ReservationStatusBadge status="CHECKED_IN" />
        <ReservationStatusBadge status="CHECKED_OUT" />
      </p>
    </>
  );
}

/** Barre d'un séjour, qui mène à sa fiche ; un bord coupé montre qu'il déborde de la période. */
function StayBar({ stay }: { stay: StayCell }) {
  const { reservation, startsBefore, endsAfter } = stay;
  const name = `${reservation.client.firstName} ${reservation.client.lastName}`;
  const status = RESERVATION_STATUS_LABEL[reservation.status].toLowerCase();

  return (
    <Link
      to={`/reservations/${reservation.id}`}
      aria-label={`${name}, ${status}, du ${formatShortDate(reservation.checkInDate)} au ${formatShortDate(reservation.checkOutDate)}`}
      className={`flex items-center gap-1 px-2 py-1 text-xs font-medium hover:underline ${RESERVATION_STATUS_CLASS[reservation.status]} ${startsBefore ? "" : "rounded-l-md"} ${endsAfter ? "" : "rounded-r-md"}`}
    >
      {startsBefore && <span aria-hidden="true">‹</span>}
      <span className="truncate">{name}</span>
      {endsAfter && (
        <span aria-hidden="true" className="ml-auto">
          ›
        </span>
      )}
    </Link>
  );
}
