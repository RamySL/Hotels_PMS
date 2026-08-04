import { useState, type FormEvent } from "react";
import { useNavigate, useParams } from "react-router";
import { Alert } from "@/shared/components/Alert";
import { Button } from "@/shared/components/Button";
import { ButtonLink } from "@/shared/components/ButtonLink";
import { TextField } from "@/shared/components/TextField";
import { useApiData } from "@/shared/hooks/useApiData";
import { ApiError } from "@/shared/lib/apiClient";
import { toTextOrNull } from "@/shared/lib/form";
import { createClient, updateClient } from "./api";
import type { Client, ClientRequest } from "./types";

/** Création (/clients/nouveau) ou modification (/clients/:id/modifier) d'un client. */
export function ClientFormPage() {
  const { id } = useParams();
  return id ? (
    <EditClient id={id} />
  ) : (
    <ClientForm title="Nouveau client" onSave={createClient} />
  );
}

function EditClient({ id }: { id: string }) {
  const client = useApiData<Client>(`/clients/${id}`);

  if (client.error) {
    return client.error.status === 404 ? (
      <p className="text-slate-600">Ce client n'existe pas.</p>
    ) : (
      <Alert tone="error">{client.error.message}</Alert>
    );
  }
  if (client.data === undefined) {
    return <p className="text-slate-500">Chargement…</p>;
  }
  return (
    <ClientForm
      key={client.data.id}
      title={`${client.data.firstName} ${client.data.lastName}`}
      initial={client.data}
      onSave={(request) => updateClient(id, request)}
    />
  );
}

interface ClientFormProps {
  title: string;
  initial?: Client;
  onSave: (request: ClientRequest) => Promise<Client>;
}

function ClientForm({ title, initial, onSave }: ClientFormProps) {
  const navigate = useNavigate();
  const [firstName, setFirstName] = useState(initial?.firstName ?? "");
  const [lastName, setLastName] = useState(initial?.lastName ?? "");
  const [email, setEmail] = useState(initial?.email ?? "");
  const [phone, setPhone] = useState(initial?.phone ?? "");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setError(null);
    setFieldErrors({});
    try {
      const saved = await onSave({
        firstName: firstName.trim(),
        lastName: lastName.trim(),
        email: toTextOrNull(email),
        phone: toTextOrNull(phone),
      });
      // La liste s'ouvre sur le client qui vient d'être enregistré
      navigate(
        `/clients?${new URLSearchParams({ recherche: saved.lastName })}`,
      );
    } catch (e) {
      setError(
        e instanceof ApiError ? e.message : "L'enregistrement a échoué.",
      );
      setFieldErrors(e instanceof ApiError ? e.errors : {});
      setSaving(false);
    }
  }

  return (
    <>
      <h1 className="text-2xl font-bold text-slate-900">{title}</h1>
      {error && (
        <Alert tone="error" className="mt-4">
          {error}
        </Alert>
      )}
      <form onSubmit={handleSubmit} className="mt-6 max-w-md space-y-4">
        <TextField
          label="Prénom"
          required
          maxLength={100}
          value={firstName}
          onChange={(event) => setFirstName(event.target.value)}
          error={fieldErrors.firstName}
        />
        <TextField
          label="Nom"
          required
          maxLength={100}
          value={lastName}
          onChange={(event) => setLastName(event.target.value)}
          error={fieldErrors.lastName}
        />
        <TextField
          label="Email (facultatif)"
          type="email"
          maxLength={255}
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          error={fieldErrors.email}
        />
        <TextField
          label="Téléphone (facultatif)"
          type="tel"
          maxLength={30}
          value={phone}
          onChange={(event) => setPhone(event.target.value)}
          error={fieldErrors.phone}
        />
        <div className="flex gap-3">
          <Button type="submit" disabled={saving}>
            {saving ? "Enregistrement…" : "Enregistrer"}
          </Button>
          <ButtonLink to="/clients" variant="secondary">
            Annuler
          </ButtonLink>
        </div>
      </form>
    </>
  );
}
