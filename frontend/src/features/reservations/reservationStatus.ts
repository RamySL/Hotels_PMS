import type { ReservationStatus } from "./types";

export const RESERVATION_STATUS_LABEL: Record<ReservationStatus, string> = {
  CONFIRMED: "Confirmée",
  CHECKED_IN: "En séjour",
  CHECKED_OUT: "Terminée",
  CANCELLED: "Annulée",
};
