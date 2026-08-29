import type { PaymentMethod } from "./types";

export const PAYMENT_METHOD_LABEL: Record<PaymentMethod, string> = {
  CASH: "Espèces",
  CARD: "Carte bancaire",
  TRANSFER: "Virement",
};

export const PAYMENT_METHODS = Object.keys(
  PAYMENT_METHOD_LABEL,
) as PaymentMethod[];

export function isPaymentMethod(value: string): value is PaymentMethod {
  return value in PAYMENT_METHOD_LABEL;
}
