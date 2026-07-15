import { useState, type FormEvent } from "react";
import { Navigate, useLocation, type Location } from "react-router";
import { ApiError } from "@/shared/lib/apiClient";
import { login } from "./api";
import { getLastEndReason } from "./session";
import { useSession } from "./useSession";

const INPUT_CLASS =
  "mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 text-slate-900 focus:border-blue-600 focus:ring-1 focus:ring-blue-600 focus:outline-none";

export function LoginPage() {
  const session = useSession();
  const location = useLocation();
  const [expired] = useState(() => getLastEndReason() === "expired");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  // Une session ouverte, déjà ou par ce formulaire, mène à la page demandée au départ
  if (session) {
    const from = (location.state as { from?: Location } | null)?.from;
    return <Navigate to={from ?? "/"} replace />;
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setError(null);
    setFieldErrors({});
    try {
      await login(email, password);
    } catch (e) {
      setError(
        e instanceof ApiError
          ? e.message
          : "Une erreur inattendue est survenue.",
      );
      setFieldErrors(e instanceof ApiError ? e.errors : {});
      setPassword("");
      setSubmitting(false);
    }
  }

  return (
    <main className="flex min-h-screen items-center justify-center bg-slate-50 px-4">
      <div className="w-full max-w-sm rounded-lg border border-slate-200 bg-white p-8 shadow-sm">
        <h1 className="text-2xl font-bold text-slate-900">PMS Hôtel</h1>
        <p className="mt-1 text-sm text-slate-600">
          Connectez-vous pour accéder à l'application.
        </p>

        {expired && (
          <p
            role="status"
            className="mt-4 rounded-md bg-amber-50 px-3 py-2 text-sm text-amber-800"
          >
            Votre session a expiré, reconnectez-vous.
          </p>
        )}
        {error && (
          <p
            role="alert"
            className="mt-4 rounded-md bg-red-50 px-3 py-2 text-sm text-red-700"
          >
            {error}
          </p>
        )}

        <form onSubmit={handleSubmit} className="mt-6 space-y-4">
          <div>
            <label
              htmlFor="email"
              className="block text-sm font-medium text-slate-700"
            >
              Email
            </label>
            <input
              id="email"
              type="email"
              autoComplete="username"
              required
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              aria-invalid={fieldErrors.email ? true : undefined}
              aria-describedby={fieldErrors.email ? "email-error" : undefined}
              className={INPUT_CLASS}
            />
            {fieldErrors.email && (
              <p id="email-error" className="mt-1 text-sm text-red-700">
                {fieldErrors.email}
              </p>
            )}
          </div>
          <div>
            <label
              htmlFor="password"
              className="block text-sm font-medium text-slate-700"
            >
              Mot de passe
            </label>
            <input
              id="password"
              type="password"
              autoComplete="current-password"
              required
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              aria-invalid={fieldErrors.password ? true : undefined}
              aria-describedby={
                fieldErrors.password ? "password-error" : undefined
              }
              className={INPUT_CLASS}
            />
            {fieldErrors.password && (
              <p id="password-error" className="mt-1 text-sm text-red-700">
                {fieldErrors.password}
              </p>
            )}
          </div>
          <button
            type="submit"
            disabled={submitting}
            className="w-full rounded-md bg-blue-700 px-4 py-2 font-medium text-white hover:bg-blue-800 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {submitting ? "Connexion…" : "Se connecter"}
          </button>
        </form>
      </div>
    </main>
  );
}
