import { Link, Outlet } from "react-router";
import { useIsAdmin } from "./useIsAdmin";

/**
 * Réserve les routes enfants à l'administration. Ce n'est qu'un confort : le backend
 * refuse de toute façon ces actions aux autres rôles (403).
 */
export function RequireAdmin() {
  const isAdmin = useIsAdmin();

  if (!isAdmin) {
    return (
      <>
        <h1 className="text-2xl font-bold text-slate-900">
          Page réservée à l'administration
        </h1>
        <p className="mt-2 text-slate-600">
          Votre compte ne permet pas d'ouvrir cette page.
        </p>
        <Link
          to="/"
          className="mt-4 inline-block text-blue-700 hover:underline"
        >
          Retour à l'accueil
        </Link>
      </>
    );
  }
  return <Outlet />;
}
