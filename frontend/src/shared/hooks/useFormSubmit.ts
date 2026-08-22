import { useState, type FormEvent } from "react";
import { ApiError } from "@/shared/lib/apiClient";

interface FormSubmitOptions {
  /** Message affiché pour une erreur qui ne vient pas de l'API. */
  unexpectedError?: string;
  /** Appelé après un échec, par exemple pour vider un mot de passe. */
  onError?: () => void;
}

export interface FormSubmit {
  saving: boolean;
  /** Erreur générale, à afficher dans une alerte. */
  error: string | null;
  /** Erreurs par champ renvoyées par l'API (400), à afficher sous les champs. */
  fieldErrors: Record<string, string>;
  /** Gestionnaire de soumission : il empêche le rechargement de la page, puis lance save. */
  handleSubmit: (
    save: () => Promise<void>,
  ) => (event: FormEvent<HTMLFormElement>) => Promise<void>;
}

/** État d'un formulaire envoyé à l'API : envoi en cours, erreur générale, erreurs par champ. */
export function useFormSubmit({
  unexpectedError = "L'enregistrement a échoué.",
  onError,
}: FormSubmitOptions = {}): FormSubmit {
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  function handleSubmit(save: () => Promise<void>) {
    return async (event: FormEvent<HTMLFormElement>) => {
      event.preventDefault();
      setSaving(true);
      setError(null);
      setFieldErrors({});
      try {
        await save();
      } catch (e) {
        setError(e instanceof ApiError ? e.message : unexpectedError);
        setFieldErrors(e instanceof ApiError ? e.errors : {});
        onError?.();
      } finally {
        setSaving(false);
      }
    };
  }

  return { saving, error, fieldErrors, handleSubmit };
}
