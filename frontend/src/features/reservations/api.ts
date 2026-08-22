import { api } from "@/shared/lib/apiClient";
import type { Reservation } from "./types";

export function checkIn(id: string): Promise<Reservation> {
  return api.patch<Reservation>(`/reservations/${id}/check-in`);
}

export function checkOut(id: string): Promise<Reservation> {
  return api.patch<Reservation>(`/reservations/${id}/check-out`);
}

export function cancelReservation(id: string): Promise<Reservation> {
  return api.patch<Reservation>(`/reservations/${id}/cancel`);
}
