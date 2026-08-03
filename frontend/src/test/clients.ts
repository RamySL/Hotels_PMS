import type { Client } from "@/features/clients/types";
import type { Page } from "@/shared/lib/page";

export const HELENE_DUPONT: Client = {
  id: "client-dupont",
  firstName: "Hélène",
  lastName: "Dupont",
  email: "helene.dupont@example.com",
  phone: "06 12 34 56 78",
};

/** Client venu au comptoir, sans email ni téléphone. */
export const JEAN_MARTIN: Client = {
  id: "client-martin",
  firstName: "Jean",
  lastName: "Martin",
  email: null,
  phone: null,
};

/** Page de l'API, au format PagedModel de Spring Data. */
export function pageOf<T>(
  content: T[],
  { number = 0, totalPages = 1, totalElements = content.length } = {},
): Page<T> {
  return { content, page: { size: 20, number, totalElements, totalPages } };
}
