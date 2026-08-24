import type { ReservationStatus } from "./types";

export const RESERVATION_STATUS_LABEL: Record<ReservationStatus, string> = {
  CONFIRMED: "Confirmée",
  CHECKED_IN: "En séjour",
  CHECKED_OUT: "Terminée",
  CANCELLED: "Annulée",
};

/** Couleurs d'un statut, communes au badge et aux barres du planning. */
export const RESERVATION_STATUS_CLASS: Record<ReservationStatus, string> = {
  CONFIRMED: "bg-blue-100 text-blue-800",
  CHECKED_IN: "bg-green-100 text-green-800",
  CHECKED_OUT: "bg-slate-100 text-slate-700",
  CANCELLED: "bg-red-100 text-red-800",
};
