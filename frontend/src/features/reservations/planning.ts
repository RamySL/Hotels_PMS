import type { Room } from "@/features/rooms/types";
import { addDays, daysBetween } from "@/shared/lib/dates";
import type { Reservation } from "./types";

/** Jour où la chambre n'est pas occupée. */
export interface FreeCell {
  kind: "free";
  date: string;
}

/** Séjour, sur les jours qu'il couvre dans la période. */
export interface StayCell {
  kind: "stay";
  reservation: Reservation;
  /** Nombre de jours couverts dans la période. */
  span: number;
  /** Le séjour a commencé avant la période. */
  startsBefore: boolean;
  /** Le séjour continue après la période. */
  endsAfter: boolean;
}

export type PlanningCell = FreeCell | StayCell;

export interface PlanningRow {
  room: Room;
  /** Cases de gauche à droite : ensemble, elles couvrent chaque jour de la période une fois. */
  cells: PlanningCell[];
}

/**
 * Découpe chaque chambre en jours libres et en séjours, sur des jours consécutifs. Un
 * jour représente une nuit : un séjour du 7 au 9 couvre le 7 et le 8. Les réservations
 * annulées sont écartées, comme celles d'autres chambres ou d'autres dates.
 */
export function buildPlanning(
  rooms: Room[],
  reservations: Reservation[],
  days: string[],
  today: string,
): PlanningRow[] {
  const from = days[0];
  const to = addDays(days[days.length - 1], 1);

  return rooms.map((room) => {
    const stays = reservations
      .filter(
        (reservation) =>
          reservation.room.id === room.id && reservation.status !== "CANCELLED",
      )
      .sort(byArrival);
    const cells: PlanningCell[] = [];
    let day = from;
    for (const [index, reservation] of stays.entries()) {
      const departure = departureOf(reservation, stays[index + 1], today);
      const start =
        reservation.checkInDate > from ? reservation.checkInDate : from;
      const end = departure < to ? departure : to;
      if (start < end) {
        cells.push(...freeDays(day, start), {
          kind: "stay",
          reservation,
          span: daysBetween(start, end),
          startsBefore: reservation.checkInDate < from,
          endsAfter: departure > to,
        });
        day = end;
      }
    }
    cells.push(...freeDays(day, to));
    return { room, cells };
  });
}

/**
 * Le départ prévu, sauf si le client est parti plus tôt : le check-out fait, il a quitté
 * la chambre au plus tard aujourd'hui, et elle a pu être réservée à nouveau depuis.
 */
function departureOf(
  reservation: Reservation,
  next: Reservation | undefined,
  today: string,
): string {
  let departure = reservation.checkOutDate;
  if (reservation.status === "CHECKED_OUT" && today < departure) {
    departure = today;
  }
  if (next && next.checkInDate < departure) {
    departure = next.checkInDate;
  }
  return departure;
}

/** Par date d'arrivée ; le même jour, un séjour terminé passe avant la réservation qui le suit. */
function byArrival(a: Reservation, b: Reservation): number {
  return (
    a.checkInDate.localeCompare(b.checkInDate) ||
    Number(b.status === "CHECKED_OUT") - Number(a.status === "CHECKED_OUT")
  );
}

function freeDays(from: string, to: string): FreeCell[] {
  const cells: FreeCell[] = [];
  for (let day = from; day < to; day = addDays(day, 1)) {
    cells.push({ kind: "free", date: day });
  }
  return cells;
}
