import { Link, Outlet } from "react-router";

/** Cadre commun à toutes les pages : en-tête et zone de contenu. */
export function Layout() {
  return (
    <div className="min-h-screen bg-slate-50">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto max-w-5xl px-6 py-4">
          <Link to="/" className="text-lg font-semibold text-slate-900">
            PMS Hôtel
          </Link>
        </div>
      </header>
      <main className="mx-auto max-w-5xl px-6 py-8">
        <Outlet />
      </main>
    </div>
  );
}
