/** Pagination d'une liste de l'API. */
export interface PageInfo {
  size: number;
  /** Numéro de la page, à partir de 0. */
  number: number;
  totalElements: number;
  totalPages: number;
}

/** Page d'une liste paginée de l'API (format PagedModel de Spring Data). */
export interface Page<T> {
  content: T[];
  page: PageInfo;
}

/** Page demandée dans l'adresse (?page=), à partir de 1 ; 1 si elle est absente ou invalide. */
export function pageFromAddress(value: string | null): number {
  const page = Number(value);
  return Number.isInteger(page) && page >= 1 ? page : 1;
}
