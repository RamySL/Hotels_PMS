const EUROS = new Intl.NumberFormat("fr-FR", {
  style: "currency",
  currency: "EUR",
});

/** Montant en euros, au format français : 89,90 €. */
export function formatEuros(amount: number): string {
  return EUROS.format(amount);
}
