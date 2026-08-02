import { Link } from "react-router";
import { useIsAdmin } from "@/features/auth/useIsAdmin";
import { Alert } from "@/shared/components/Alert";
import { ButtonLink } from "@/shared/components/ButtonLink";
import { useApiData } from "@/shared/hooks/useApiData";
import { formatEuros } from "@/shared/lib/format";
import type { RoomType } from "./types";

/** Types de chambre et tarifs : visibles par tous, modifiables par l'administration. */
export function RoomTypesPage() {
  const isAdmin = useIsAdmin();
  const types = useApiData<RoomType[]>("/room-types");

  return (
    <>
      <Link to="/chambres" className="text-sm text-blue-700 hover:underline">
        ← Chambres
      </Link>
      <div className="mt-2 flex items-center justify-between">
        <h1 className="text-2xl font-bold text-slate-900">Types et tarifs</h1>
        {isAdmin && (
          <ButtonLink to="/chambres/types/nouveau">Nouveau type</ButtonLink>
        )}
      </div>

      {types.error && (
        <Alert tone="error" className="mt-4">
          {types.error.message}
        </Alert>
      )}

      {types.data === undefined ? (
        types.loading && <p className="mt-6 text-slate-500">Chargement…</p>
      ) : types.data.length === 0 ? (
        <p className="mt-6 text-slate-500">Aucun type de chambre.</p>
      ) : (
        <table className="mt-6 w-full border-collapse overflow-hidden rounded-lg bg-white text-left shadow-sm">
          <thead className="border-b border-slate-200 text-sm text-slate-600">
            <tr>
              <th className="px-4 py-3 font-medium">Nom</th>
              <th className="px-4 py-3 font-medium">Description</th>
              <th className="px-4 py-3 font-medium">Prix par nuit</th>
              <th className="px-4 py-3 font-medium">Capacité</th>
              {isAdmin && (
                <th className="px-4 py-3 font-medium">
                  <span className="sr-only">Actions</span>
                </th>
              )}
            </tr>
          </thead>
          <tbody>
            {types.data.map((type) => (
              <tr
                key={type.id}
                className="border-b border-slate-100 last:border-0"
              >
                <td className="px-4 py-3 font-medium text-slate-900">
                  {type.name}
                </td>
                <td className="px-4 py-3 text-slate-700">{type.description}</td>
                <td className="px-4 py-3 text-slate-700">
                  {formatEuros(type.basePrice)}
                </td>
                <td className="px-4 py-3 text-slate-700">
                  {type.capacity} pers.
                </td>
                {isAdmin && (
                  <td className="px-4 py-3 text-right">
                    <ButtonLink
                      to={`/chambres/types/${type.id}/modifier`}
                      variant="secondary"
                      size="sm"
                      aria-label={`Modifier le type ${type.name}`}
                    >
                      Modifier
                    </ButtonLink>
                  </td>
                )}
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </>
  );
}
