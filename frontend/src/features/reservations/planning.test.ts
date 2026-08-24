import { describe, expect, it } from "vitest";
import { addDays } from "@/shared/lib/dates";
import { DUPONT_STAY } from "@/test/reservations";
import { room } from "@/test/rooms";
import { buildPlanning, type PlanningRow } from "./planning";
import type { Reservation, ReservationStatus } from "./types";

/** Du mercredi 7 au mardi 13 octobre 2026. */
const WEEK = Array.from({ length: 7 }, (_, index) =>
  addDays("2026-10-07", index),
);
const ROOM_101 = room("101");
const ROOM_102 = room("102");

function stay(
  id: string,
  checkInDate: string,
  checkOutDate: string,
  status: ReservationStatus = "CONFIRMED",
  roomNumber = "101",
): Reservation {
  return {
    ...DUPONT_STAY,
    id,
    status,
    checkInDate,
    checkOutDate,
    room: { id: `room-${roomNumber}`, roomNumber, roomTypeName: "Double" },
  };
}

/** Une ligne, lisible d'un coup d'œil : « · » pour un jour libre, « ‹a×2› » pour un séjour. */
function layout(row: PlanningRow): string {
  return row.cells
    .map((cell) =>
      cell.kind === "free"
        ? "·"
        : `${cell.startsBefore ? "‹" : ""}${cell.reservation.id}×${cell.span}${cell.endsAfter ? "›" : ""}`,
    )
    .join(" ");
}

describe("buildPlanning", () => {
  it("lays out each room as free days and stays, one day per night", () => {
    const rows = buildPlanning(
      [ROOM_101, ROOM_102],
      [
        stay("a", "2026-10-08", "2026-10-10"),
        stay("b", "2026-10-10", "2026-10-11"),
      ],
      WEEK,
      "2026-10-07",
    );

    expect(rows.map((row) => row.room.roomNumber)).toEqual(["101", "102"]);
    expect(layout(rows[0])).toBe("· a×2 b×1 · · ·");
    expect(layout(rows[1])).toBe("· · · · · · ·");
    expect(rows[0].cells[0]).toEqual({ kind: "free", date: "2026-10-07" });
  });

  it("cuts the stays at the edges of the period", () => {
    const rows = buildPlanning(
      [ROOM_101, ROOM_102],
      [
        stay("before", "2026-10-05", "2026-10-08"),
        stay("after", "2026-10-12", "2026-10-20"),
        stay("whole", "2026-10-01", "2026-10-30", "CHECKED_IN", "102"),
      ],
      WEEK,
      "2026-10-07",
    );

    expect(layout(rows[0])).toBe("‹before×1 · · · · after×2›");
    expect(layout(rows[1])).toBe("‹whole×7›");
  });

  it("leaves out the cancelled reservations and the stays of other dates", () => {
    const rows = buildPlanning(
      [ROOM_101],
      [
        stay("cancelled", "2026-10-08", "2026-10-10", "CANCELLED"),
        stay("left-this-morning", "2026-10-05", "2026-10-07"),
        stay("next-week", "2026-10-14", "2026-10-16"),
      ],
      WEEK,
      "2026-10-07",
    );

    expect(layout(rows[0])).toBe("· · · · · · ·");
  });

  it("ends a stay checked out early at the next arrival, or today at the latest", () => {
    const rows = buildPlanning(
      [ROOM_101, ROOM_102, room("103")],
      [
        stay("left-early", "2026-10-05", "2026-10-12", "CHECKED_OUT"),
        stay("next-guest", "2026-10-08", "2026-10-10"),
        stay("gone", "2026-10-06", "2026-10-20", "CHECKED_OUT", "102"),
        stay("rebooked", "2026-10-07", "2026-10-08", "CONFIRMED", "103"),
        stay("same-day", "2026-10-07", "2026-10-09", "CHECKED_OUT", "103"),
      ],
      WEEK,
      "2026-10-09",
    );

    expect(layout(rows[0])).toBe("‹left-early×1 next-guest×2 · · · ·");
    expect(layout(rows[1])).toBe("‹gone×2 · · · · ·");
    expect(layout(rows[2])).toBe("rebooked×1 · · · · · ·");
  });
});
