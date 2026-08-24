import {
  RESERVATION_STATUS_CLASS,
  RESERVATION_STATUS_LABEL,
} from "./reservationStatus";
import type { ReservationStatus } from "./types";

export function ReservationStatusBadge({
  status,
}: {
  status: ReservationStatus;
}) {
  return (
    <span
      className={`inline-block rounded-full px-2.5 py-0.5 text-xs font-medium ${RESERVATION_STATUS_CLASS[status]}`}
    >
      {RESERVATION_STATUS_LABEL[status]}
    </span>
  );
}
