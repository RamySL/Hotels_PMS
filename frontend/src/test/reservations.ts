import type { Invoice } from "@/features/invoices/types";
import type { Reservation } from "@/features/reservations/types";

/** Deux nuits en chambre double pour Hélène Dupont, du mercredi 7 au vendredi 9 octobre 2026. */
export const DUPONT_STAY: Reservation = {
  id: "resa-dupont",
  status: "CONFIRMED",
  checkInDate: "2026-10-07",
  checkOutDate: "2026-10-09",
  nights: 2,
  numberOfGuests: 2,
  totalPrice: 179.8,
  client: { id: "client-dupont", firstName: "Hélène", lastName: "Dupont" },
  room: { id: "room-101", roomNumber: "101", roomTypeName: "Double" },
  createdBy: { id: "user-reception", firstName: "Lucas", lastName: "Bernard" },
  createdAt: "2026-10-05T12:00:00Z",
};

/** Facture de la réservation, après les paiements reçus. */
export function invoiceFor(reservation: Reservation, amountPaid = 0): Invoice {
  const balance = reservation.totalPrice - amountPaid;
  return {
    id: `invoice-${reservation.id}`,
    status:
      amountPaid === 0 ? "UNPAID" : balance > 0 ? "PARTIALLY_PAID" : "PAID",
    amountTotal: reservation.totalPrice,
    amountPaid,
    balance,
  };
}
