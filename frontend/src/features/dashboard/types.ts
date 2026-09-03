import type { Room } from "@/features/rooms/types";

/** Séjour à traiter aujourd'hui. balance : reste à payer de sa facture ; négatif, trop-perçu. */
export interface DayStay {
  reservationId: string;
  /** Prénom et nom du client. */
  clientName: string;
  roomNumber: string;
  checkInDate: string;
  checkOutDate: string;
  numberOfGuests: number;
  balance: number;
}

/** Chambres en service vendues pour la nuit de ce soir. */
export interface Occupancy {
  occupiedRooms: number;
  roomsInService: number;
  /** Pourcentage sur 100, à une décimale. */
  rate: number;
}

/** Journée de l'hôtel (GET /dashboard/today) : ce qu'il reste à faire, et son état. */
export interface Dashboard {
  /** Date de l'hôtel : un séjour dont l'arrivée, ou le départ, la précède est en retard. */
  date: string;
  arrivals: DayStay[];
  departures: DayStay[];
  occupancy: Occupancy;
  roomsToClean: Room[];
}
