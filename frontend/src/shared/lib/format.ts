const EUROS = new Intl.NumberFormat("fr-FR", {
  style: "currency",
  currency: "EUR",
});

const PERCENT = new Intl.NumberFormat("fr-FR", {
  style: "percent",
  maximumFractionDigits: 1,
});

/** Montant en euros, au format français : 89,90 €. */
export function formatEuros(amount: number): string {
  return EUROS.format(amount);
}

/** Pourcentage donné sur 100, comme ceux de l'API, au format français : 66,7 %. */
export function formatPercent(rate: number): string {
  return PERCENT.format(rate / 100);
}
