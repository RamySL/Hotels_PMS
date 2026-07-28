import type { RoomStatus } from "./types";

export const ROOM_STATUS_LABEL: Record<RoomStatus, string> = {
  READY: "Prête",
  OCCUPIED: "Occupée",
  CLEANING: "À nettoyer",
  OUT_OF_SERVICE: "Hors service",
};

export const ROOM_STATUSES = Object.keys(ROOM_STATUS_LABEL) as RoomStatus[];

export interface StatusAction {
  label: string;
  status: RoomStatus;
}

/**
 * Actions de la réception selon le statut de la chambre. Une chambre occupée n'en a
 * aucune : le check-out la libère.
 */
export const STATUS_ACTIONS: Record<RoomStatus, StatusAction[]> = {
  READY: [
    { label: "À nettoyer", status: "CLEANING" },
    { label: "Mettre hors service", status: "OUT_OF_SERVICE" },
  ],
  CLEANING: [{ label: "Ménage terminé", status: "READY" }],
  OUT_OF_SERVICE: [{ label: "Remettre en service", status: "READY" }],
  OCCUPIED: [],
};

export function isRoomStatus(value: string | null): value is RoomStatus {
  return value !== null && value in ROOM_STATUS_LABEL;
}
