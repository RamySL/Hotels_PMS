/**
 * Les dates de l'API sont des jours sans heure ni fuseau : "2026-10-07". Elles se lisent
 * et s'écrivent en heure locale ; new Date("2026-10-07") donnerait minuit UTC, qui tombe
 * la veille dans un fuseau en retard sur l'UTC.
 */

/** Date du jour au format de l'API, selon l'horloge locale. */
export function today(): string {
  return toIsoDate(new Date());
}

/** Jour local d'une Date, au format de l'API ; toISOString() donnerait le jour UTC. */
export function toIsoDate(date: Date): string {
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${date.getFullYear()}-${month}-${day}`;
}

/** Minuit, heure locale, du jour donné au format de l'API. */
export function parseDate(isoDate: string): Date {
  const [year, month, day] = isoDate.split("-").map(Number);
  return new Date(year, month - 1, day);
}

const DAY = new Intl.DateTimeFormat("fr-FR", {
  weekday: "short",
  day: "numeric",
  month: "short",
  year: "numeric",
});

/** Jour à afficher : « mer. 7 oct. 2026 ». Une Date (un instant de l'API) donne son jour local. */
export function formatDate(date: string | Date): string {
  return DAY.format(typeof date === "string" ? parseDate(date) : date);
}
