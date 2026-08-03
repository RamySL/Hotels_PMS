/** Page d'une liste paginée de l'API (format PagedModel de Spring Data). */
export interface Page<T> {
  content: T[];
  page: {
    size: number;
    /** Numéro de la page, à partir de 0. */
    number: number;
    totalElements: number;
    totalPages: number;
  };
}
