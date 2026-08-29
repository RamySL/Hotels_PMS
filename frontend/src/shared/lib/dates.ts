/**
 * Les dates de l'API sont des jours sans heure ni fuseau : "2026-10-07". Elles se lisent
 * et s'écrivent en heure locale ; new Date("2026-10-07") donnerait minuit UTC, qui tombe
 * la veille dans un fuseau en retard sur l'UTC.
 */

const MS_PER_DAY = 24 * 60 * 60 * 1000;

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

/** Vrai pour un jour qui existe, au format de l'API : "2026-02-30" ne passe pas. */
export function isIsoDate(value: string | null): value is string {
  return (
    value !== null &&
    /^\d{4}-\d{2}-\d{2}$/.test(value) &&
    toIsoDate(parseDate(value)) === value
  );
}

/** Le jour situé n jours plus loin, ou plus tôt si n est négatif. */
export function addDays(isoDate: string, days: number): string {
  const date = parseDate(isoDate);
  // Des jours du calendrier, pas des tranches de 24 h : le changement d'heure ne décale rien
  date.setDate(date.getDate() + days);
  return toIsoDate(date);
}

/** Nombre de jours de from à to : 2 du 7 au 9 octobre, soit 2 nuits. */
export function daysBetween(from: string, to: string): number {
  // Un jour de changement d'heure dure 23 h ou 25 h : l'arrondi les compte pour un jour
  return Math.round(
    (parseDate(to).getTime() - parseDate(from).getTime()) / MS_PER_DAY,
  );
}

const DAY = new Intl.DateTimeFormat("fr-FR", {
  weekday: "short",
  day: "numeric",
  month: "short",
  year: "numeric",
});

const SHORT_DAY = new Intl.DateTimeFormat("fr-FR", {
  weekday: "short",
  day: "numeric",
  month: "short",
});

const WEEKDAY = new Intl.DateTimeFormat("fr-FR", { weekday: "short" });

const DAY_AND_TIME = new Intl.DateTimeFormat("fr-FR", {
  weekday: "short",
  day: "numeric",
  month: "short",
  year: "numeric",
  hour: "2-digit",
  minute: "2-digit",
});

/** Jour à afficher : « mer. 7 oct. 2026 ». Une Date (un instant de l'API) donne son jour local. */
export function formatDate(date: string | Date): string {
  return DAY.format(typeof date === "string" ? parseDate(date) : date);
}

/** Jour sans l'année, quand elle se devine : « mer. 7 oct. ». */
export function formatShortDate(isoDate: string): string {
  return SHORT_DAY.format(parseDate(isoDate));
}

/** Jour de la semaine abrégé : « mer. ». */
export function formatWeekday(isoDate: string): string {
  return WEEKDAY.format(parseDate(isoDate));
}

/** Instant de l'API, en heure locale : « mer. 7 oct. 2026, 10:32 ». */
export function formatDateTime(instant: string): string {
  return DAY_AND_TIME.format(new Date(instant));
}
