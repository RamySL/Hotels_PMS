import { useEffect, useState } from "react";
import type { Client } from "@/features/clients/types";
import { Alert } from "@/shared/components/Alert";
import { Button } from "@/shared/components/Button";
import { TextField } from "@/shared/components/TextField";
import { useApiData } from "@/shared/hooks/useApiData";
import type { Page } from "@/shared/lib/page";

const SEARCH_DELAY_MS = 300;
const MAX_RESULTS = 5;

/** Client de la réservation : encore à chercher, à créer sur place, ou choisi. */
export type ClientChoice =
  | { kind: "search" }
  | { kind: "new"; firstName: string; lastName: string }
  | { kind: "chosen"; client: Client };

interface ClientPickerProps {
  value: ClientChoice;
  onChange: (value: ClientChoice) => void;
  /** Erreurs de l'API sur le prénom et le nom du nouveau client. */
  errors: Record<string, string>;
}

/** Cherche le client par son nom, ou le crée sur place : le prénom et le nom suffisent. */
export function ClientPicker({ value, onChange, errors }: ClientPickerProps) {
  if (value.kind === "chosen") {
    const { client } = value;
    return (
      <div className="flex items-center justify-between gap-4 rounded-md border border-slate-200 bg-white px-3 py-2">
        <div>
          <p className="font-medium text-slate-900">
            {client.firstName} {client.lastName}
          </p>
          <p className="text-sm text-slate-600">{contactOf(client)}</p>
        </div>
        <Button
          variant="secondary"
          size="sm"
          onClick={() => onChange({ kind: "search" })}
        >
          Changer de client
        </Button>
      </div>
    );
  }

  if (value.kind === "new") {
    return (
      <div className="space-y-4">
        <div className="grid gap-4 sm:grid-cols-2">
          <TextField
            label="Prénom"
            required
            maxLength={100}
            value={value.firstName}
            onChange={(event) =>
              onChange({ ...value, firstName: event.target.value })
            }
            error={errors.firstName}
          />
          <TextField
            label="Nom"
            required
            maxLength={100}
            value={value.lastName}
            onChange={(event) =>
              onChange({ ...value, lastName: event.target.value })
            }
            error={errors.lastName}
          />
        </div>
        <Button
          variant="secondary"
          size="sm"
          onClick={() => onChange({ kind: "search" })}
        >
          Chercher un client existant
        </Button>
      </div>
    );
  }

  return (
    <ClientSearch
      onChoose={(client) => onChange({ kind: "chosen", client })}
      onCreate={() => onChange({ kind: "new", firstName: "", lastName: "" })}
    />
  );
}

interface ClientSearchProps {
  onChoose: (client: Client) => void;
  onCreate: () => void;
}

function ClientSearch({ onChoose, onCreate }: ClientSearchProps) {
  const [query, setQuery] = useState("");
  const [search, setSearch] = useState("");
  const results = useApiData<Page<Client>>(
    search
      ? `/clients?${new URLSearchParams({ search, page: "0", size: String(MAX_RESULTS) })}`
      : null,
  );

  // La recherche part après une pause dans la frappe
  useEffect(() => {
    const wanted = query.trim();
    if (wanted === search) {
      return;
    }
    const timer = setTimeout(() => setSearch(wanted), SEARCH_DELAY_MS);
    return () => clearTimeout(timer);
  }, [query, search]);

  return (
    <div>
      <TextField
        label="Rechercher un client"
        type="search"
        placeholder="Nom, email ou téléphone"
        value={query}
        onChange={(event) => setQuery(event.target.value)}
      />
      {results.error && (
        <Alert tone="error" className="mt-2">
          {results.error.message}
        </Alert>
      )}
      {search &&
        results.data &&
        (results.data.content.length === 0 ? (
          <p className="mt-2 text-sm text-slate-600">
            Aucun client ne correspond à « {search} ».
          </p>
        ) : (
          <ul className="mt-2 divide-y divide-slate-100 rounded-md border border-slate-200 bg-white">
            {results.data.content.map((client) => (
              <li key={client.id}>
                <button
                  type="button"
                  onClick={() => onChoose(client)}
                  className="w-full px-3 py-2 text-left hover:bg-slate-50"
                >
                  <span className="font-medium text-slate-900">
                    {client.firstName} {client.lastName}
                  </span>{" "}
                  <span className="text-sm text-slate-600">
                    {contactOf(client)}
                  </span>
                </button>
              </li>
            ))}
          </ul>
        ))}
      {search &&
        results.data &&
        results.data.page.totalElements > MAX_RESULTS && (
          <p className="mt-2 text-sm text-slate-600">
            Plus de {MAX_RESULTS} clients correspondent : précisez la recherche.
          </p>
        )}
      <Button variant="secondary" size="sm" className="mt-3" onClick={onCreate}>
        Nouveau client
      </Button>
    </div>
  );
}

function contactOf(client: Client): string {
  return (
    [client.email, client.phone].filter(Boolean).join(" · ") ||
    "Ni email ni téléphone"
  );
}
