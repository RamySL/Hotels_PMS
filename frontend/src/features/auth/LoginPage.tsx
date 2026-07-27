import { useState, type FormEvent } from "react";
import { Navigate, useLocation, type Location } from "react-router";
import { Alert } from "@/shared/components/Alert";
import { Button } from "@/shared/components/Button";
import { TextField } from "@/shared/components/TextField";
import { ApiError } from "@/shared/lib/apiClient";
import { login } from "./api";
import { getLastEndReason } from "./session";
import { useSession } from "./useSession";

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
          <Alert tone="notice" className="mt-4">
            Votre session a expiré, reconnectez-vous.
          </Alert>
        )}
        {error && (
          <Alert tone="error" className="mt-4">
            {error}
          </Alert>
        )}

        <form onSubmit={handleSubmit} className="mt-6 space-y-4">
          <TextField
            label="Email"
            type="email"
            autoComplete="username"
            required
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            error={fieldErrors.email}
          />
          <TextField
            label="Mot de passe"
            type="password"
            autoComplete="current-password"
            required
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            error={fieldErrors.password}
          />
          <Button type="submit" disabled={submitting} className="w-full">
            {submitting ? "Connexion…" : "Se connecter"}
          </Button>
        </form>
      </div>
    </main>
  );
}
