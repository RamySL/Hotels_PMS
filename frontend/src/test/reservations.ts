import type {
  Invoice,
  Payment,
  PaymentMethod,
} from "@/features/invoices/types";
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

/** Paiement encaissé par Lucas Bernard le mercredi 7 octobre 2026, à 10 h 32. */
export function payment(
  amount: number,
  method: PaymentMethod = "CARD",
  id = `payment-${amount}`,
): Payment {
  return {
    id,
    amount,
    method,
    paidAt: "2026-10-07T08:32:00Z",
    recordedBy: "Lucas Bernard",
  };
}

/**
 * Facture de la réservation après ces paiements, calculée comme le backend : en
 * centimes, sans erreur d'arrondi, et annulée avec la réservation.
 */
export function invoiceFor(
  reservation: Reservation,
  payments: Payment[] = [],
): Invoice {
  const cents = (amount: number) => Math.round(amount * 100);
  const total = cents(reservation.totalPrice);
  const paid = payments.reduce((sum, { amount }) => sum + cents(amount), 0);
  const cancelled = reservation.status === "CANCELLED";
  return {
    id: `invoice-${reservation.id}`,
    status: cancelled
      ? "CANCELLED"
      : paid === 0
        ? "UNPAID"
        : paid < total
          ? "PARTIALLY_PAID"
          : "PAID",
    amountTotal: reservation.totalPrice,
    amountPaid: paid / 100,
    balance: ((cancelled ? 0 : total) - paid) / 100,
    issuedAt: reservation.createdAt,
    reservation: {
      id: reservation.id,
      checkInDate: reservation.checkInDate,
      checkOutDate: reservation.checkOutDate,
      clientName: `${reservation.client.firstName} ${reservation.client.lastName}`,
      roomNumber: reservation.room.roomNumber,
    },
    payments,
  };
}
