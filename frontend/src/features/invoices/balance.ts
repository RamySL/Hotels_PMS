import { formatEuros } from "@/shared/lib/format";

/** Reste à payer d'une facture, ou trop-perçu à rembourser s'il est négatif. */
export function formatBalance(balance: number): string {
  return balance < 0
    ? `Trop-perçu : ${formatEuros(-balance)}`
    : formatEuros(balance);
}
