import { api } from "@/shared/lib/apiClient";
import type { Reservation, ReservationRequest } from "./types";

export function createReservation(
  request: ReservationRequest,
): Promise<Reservation> {
  return api.post<Reservation>("/reservations", request);
}

export function checkIn(id: string): Promise<Reservation> {
  return api.patch<Reservation>(`/reservations/${id}/check-in`);
}

export function checkOut(id: string): Promise<Reservation> {
  return api.patch<Reservation>(`/reservations/${id}/check-out`);
}

export function cancelReservation(id: string): Promise<Reservation> {
  return api.patch<Reservation>(`/reservations/${id}/cancel`);
}
