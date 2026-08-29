import type { PageInfo } from "@/shared/lib/page";
import { Button } from "./Button";

interface PaginationProps {
  /** Page affichée, comptée à partir de 1, comme dans l'adresse. */
  page: number;
  info: PageInfo;
  /** Ce que compte la liste, au singulier puis au pluriel. */
  noun: [string, string];
  onChange: (page: number) => void;
}

/** Bas d'une liste paginée : le total, la page affichée, et de quoi changer de page. */
export function Pagination({
  page,
  info,
  noun: [singular, plural],
  onChange,
}: PaginationProps) {
  const total = info.totalElements;
  const totalPages = Math.max(info.totalPages, 1);

  return (
    <div className="mt-4 flex items-center justify-between text-sm text-slate-600">
      <p>
        {total} {total > 1 ? plural : singular}
      </p>
      <div className="flex items-center gap-3">
        <Button
          variant="secondary"
          size="sm"
          disabled={page <= 1}
          onClick={() => onChange(page - 1)}
          aria-label="Page précédente"
        >
          Précédente
        </Button>
        <span>
          Page {page} sur {totalPages}
        </span>
        <Button
          variant="secondary"
          size="sm"
          disabled={page >= totalPages}
          onClick={() => onChange(page + 1)}
          aria-label="Page suivante"
        >
          Suivante
        </Button>
      </div>
    </div>
  );
}
