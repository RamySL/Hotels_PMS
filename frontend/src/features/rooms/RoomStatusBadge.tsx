import { ROOM_STATUS_LABEL } from "./roomStatus";
import type { RoomStatus } from "./types";

const BADGE_CLASS: Record<RoomStatus, string> = {
  READY: "bg-green-100 text-green-800",
  OCCUPIED: "bg-blue-100 text-blue-800",
  CLEANING: "bg-amber-100 text-amber-800",
  OUT_OF_SERVICE: "bg-red-100 text-red-800",
};

export function RoomStatusBadge({ status }: { status: RoomStatus }) {
  return (
    <span
      className={`inline-block rounded-full px-2.5 py-0.5 text-xs font-medium ${BADGE_CLASS[status]}`}
    >
      {ROOM_STATUS_LABEL[status]}
    </span>
  );
}
