/**
 * Nombre saisi dans un champ, ou null si le champ est vide : le backend signale alors
 * le champ manquant. Le test porte sur le texte saisi, et non sur le nombre obtenu :
 * 0 est « faux » en JavaScript, et `Number(value) || null` prendrait un étage 0 pour
 * un champ vide.
 */
export function toNumberOrNull(value: string): number | null {
  return value.trim() === "" ? null : Number(value);
}

/** Texte saisi, sans les espaces autour, ou null si le champ est vide. */
export function toTextOrNull(value: string): string | null {
  return value.trim() || null;
}
