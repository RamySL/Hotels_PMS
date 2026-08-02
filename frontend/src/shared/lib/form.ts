/**
 * Nombre saisi dans un champ, ou null si le champ est vide : le backend signale alors
 * le champ manquant.
 */
export function toNumberOrNull(value: string): number | null {
  return Number(value) || null;
}

/** Texte saisi, sans les espaces autour, ou null si le champ est vide. */
export function toTextOrNull(value: string): string | null {
  return value.trim() || null;
}
