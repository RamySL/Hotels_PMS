import { Link, Outlet } from "react-router";
import { logout } from "@/features/auth/api";
import type { Role } from "@/features/auth/types";
import { useSession } from "@/features/auth/useSession";

const ROLE_LABEL: Record<Role, string> = {
  ADMIN: "Administration",
  RECEPTIONIST: "Réception",
};

/**
 * Cadre commun à toutes les pages : en-tête et zone de contenu. Le nom de la personne
 * connectée reste visible, pour que chacun se déconnecte à la relève (ADR 0008).
 */
export function Layout() {
  const user = useSession()?.user;

  return (
    <div className="min-h-screen bg-slate-50">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-5xl items-center justify-between px-6 py-4">
          <Link to="/" className="text-lg font-semibold text-slate-900">
            PMS Hôtel
          </Link>
          {user && (
            <div className="flex items-center gap-4 text-sm">
              <p className="text-slate-700">
                <span className="font-medium">
                  {user.firstName} {user.lastName}
                </span>{" "}
                <span className="text-slate-500">{ROLE_LABEL[user.role]}</span>
              </p>
              <button
                type="button"
                onClick={logout}
                className="rounded-md border border-slate-300 px-3 py-1.5 text-slate-700 hover:bg-slate-100"
              >
                Se déconnecter
              </button>
            </div>
          )}
        </div>
      </header>
      <main className="mx-auto max-w-5xl px-6 py-8">
        <Outlet />
      </main>
    </div>
  );
}
