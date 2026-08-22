import { RESERVATION_STATUS_LABEL } from "./reservationStatus";
import type { ReservationStatus } from "./types";

const BADGE_CLASS: Record<ReservationStatus, string> = {
  CONFIRMED: "bg-blue-100 text-blue-800",
  CHECKED_IN: "bg-green-100 text-green-800",
  CHECKED_OUT: "bg-slate-100 text-slate-700",
  CANCELLED: "bg-red-100 text-red-800",
};

export function ReservationStatusBadge({
  status,
}: {
  status: ReservationStatus;
}) {
  return (
    <span
      className={`inline-block rounded-full px-2.5 py-0.5 text-xs font-medium ${BADGE_CLASS[status]}`}
    >
      {RESERVATION_STATUS_LABEL[status]}
    </span>
  );
}
