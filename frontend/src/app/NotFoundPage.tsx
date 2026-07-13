import { Link } from "react-router";

export function NotFoundPage() {
  return (
    <>
      <h1 className="text-2xl font-bold text-slate-900">Page introuvable</h1>
      <p className="mt-2 text-slate-600">
        Cette adresse ne correspond à aucune page.
      </p>
      <Link to="/" className="mt-4 inline-block text-blue-700 hover:underline">
        Retour à l'accueil
      </Link>
    </>
  );
}
