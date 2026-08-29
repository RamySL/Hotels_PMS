import { useEffect, useState } from "react";
import { useSearchParams } from "react-router";
import { Alert } from "@/shared/components/Alert";
import { ButtonLink } from "@/shared/components/ButtonLink";
import { Pagination } from "@/shared/components/Pagination";
import { TextField } from "@/shared/components/TextField";
import { useApiData } from "@/shared/hooks/useApiData";
import { pageFromAddress, type Page } from "@/shared/lib/page";
import type { Client } from "./types";

const PAGE_SIZE = 20;
const SEARCH_DELAY_MS = 300;

/**
 * Liste des clients, avec recherche et pagination. Les deux vivent dans l'adresse
 * (?recherche=dupont&page=2) : le rechargement et le bouton Retour les conservent.
 */
export function ClientsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const search = searchParams.get("recherche") ?? "";
  const page = pageFromAddress(searchParams.get("page"));
  const clients = useApiData<Page<Client>>(clientsPath(search, page));
  const [query, setQuery] = useState(search);
  const [shownSearch, setShownSearch] = useState(search);

  // L'adresse a changé sans passer par le champ (bouton Retour, lien) : le champ la suit
  if (search !== shownSearch) {
    setShownSearch(search);
    setQuery(search);
  }

  // La recherche part après une pause dans la frappe, et repart de la première page
  useEffect(() => {
    const wanted = query.trim();
    if (wanted === search) {
      return;
    }
    const timer = setTimeout(
      () =>
        setSearchParams(wanted ? { recherche: wanted } : {}, { replace: true }),
      SEARCH_DELAY_MS,
    );
    return () => clearTimeout(timer);
  }, [query, search, setSearchParams]);

  function goToPage(target: number) {
    const params: Record<string, string> = {};
    if (search) {
      params.recherche = search;
    }
    if (target > 1) {
      params.page = String(target);
    }
    setSearchParams(params);
  }

  return (
    <>
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-slate-900">Clients</h1>
        <ButtonLink to="/clients/nouveau">Nouveau client</ButtonLink>
      </div>

      <TextField
        label="Rechercher"
        type="search"
        placeholder="Nom, email ou téléphone"
        value={query}
        onChange={(event) => setQuery(event.target.value)}
        className="mt-6 max-w-md"
      />

      {clients.error && (
        <Alert tone="error" className="mt-4">
          {clients.error.message}
        </Alert>
      )}

      {clients.data === undefined ? (
        clients.loading && <p className="mt-6 text-slate-500">Chargement…</p>
      ) : clients.data.content.length === 0 ? (
        <p className="mt-6 text-slate-500">
          {search
            ? `Aucun client ne correspond à « ${search} ».`
            : "Aucun client enregistré."}
        </p>
      ) : (
        <>
          <table className="mt-6 w-full border-collapse overflow-hidden rounded-lg bg-white text-left shadow-sm">
            <thead className="border-b border-slate-200 text-sm text-slate-600">
              <tr>
                <th className="px-4 py-3 font-medium">Nom</th>
                <th className="px-4 py-3 font-medium">Prénom</th>
                <th className="px-4 py-3 font-medium">Email</th>
                <th className="px-4 py-3 font-medium">Téléphone</th>
                <th className="px-4 py-3 font-medium">
                  <span className="sr-only">Actions</span>
                </th>
              </tr>
            </thead>
            <tbody>
              {clients.data.content.map((client) => (
                <tr
                  key={client.id}
                  className="border-b border-slate-100 last:border-0"
                >
                  <td className="px-4 py-3 font-medium text-slate-900">
                    {client.lastName}
                  </td>
                  <td className="px-4 py-3 text-slate-700">
                    {client.firstName}
                  </td>
                  <td className="px-4 py-3 text-slate-700">
                    {client.email ?? "—"}
                  </td>
                  <td className="px-4 py-3 text-slate-700">
                    {client.phone ?? "—"}
                  </td>
                  <td className="px-4 py-3 text-right">
                    <ButtonLink
                      to={`/clients/${client.id}/modifier`}
                      variant="secondary"
                      size="sm"
                      aria-label={`Modifier ${client.firstName} ${client.lastName}`}
                    >
                      Modifier
                    </ButtonLink>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          <Pagination
            page={page}
            info={clients.data.page}
            noun={["client", "clients"]}
            onChange={goToPage}
          />
        </>
      )}
    </>
  );
}

/** Adresse de l'API : sa pagination part de 0, celle de l'adresse du navigateur de 1. */
function clientsPath(search: string, page: number): string {
  const params = new URLSearchParams();
  if (search) {
    params.set("search", search);
  }
  params.set("page", String(page - 1));
  params.set("size", String(PAGE_SIZE));
  return `/clients?${params}`;
}
