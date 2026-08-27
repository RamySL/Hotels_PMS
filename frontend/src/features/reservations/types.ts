export type ReservationStatus =
  "CONFIRMED" | "CHECKED_IN" | "CHECKED_OUT" | "CANCELLED";

interface Person {
  id: string;
  firstName: string;
  lastName: string;
}

export interface Reservation {
  id: string;
  status: ReservationStatus;
  /** Jours au format de l'API : "2026-10-07". La dernière nuit est la veille du départ. */
  checkInDate: string;
  checkOutDate: string;
  nights: number;
  numberOfGuests: number;
  totalPrice: number;
  client: Person;
  room: { id: string; roomNumber: string; roomTypeName: string };
  createdBy: Person;
  /** Instant de la réservation, en UTC : "2026-10-05T12:00:00Z". */
  createdAt: string;
}

/** Corps envoyé pour réserver une chambre. */
export interface ReservationRequest {
  clientId: string;
  roomId: string;
  checkInDate: string;
  checkOutDate: string;
  numberOfGuests: number;
}

/** Chambre libre sur une période (GET /rooms/availability), avec le prix du séjour. */
export interface AvailableRoom {
  roomId: string;
  roomNumber: string;
  floor: number;
  roomTypeName: string;
  capacity: number;
  pricePerNight: number;
  nights: number;
  totalPrice: number;
}
