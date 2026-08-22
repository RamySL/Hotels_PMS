import type { Page } from "@/shared/lib/page";

/** Page de l'API, au format PagedModel de Spring Data. */
export function pageOf<T>(
  content: T[],
  { number = 0, totalPages = 1, totalElements = content.length } = {},
): Page<T> {
  return { content, page: { size: 20, number, totalElements, totalPages } };
}
