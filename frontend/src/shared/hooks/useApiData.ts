import { useCallback, useEffect, useState } from "react";
import { api, ApiError } from "@/shared/lib/apiClient";

export interface ApiData<T> {
  /** Dernières données reçues : elles restent affichées pendant un rechargement. */
  data: T | undefined;
  error: ApiError | null;
  loading: boolean;
  reload: () => void;
}

interface Result<T> {
  request: string;
  data?: T;
  error: ApiError | null;
}

/**
 * Lit path sur l'API, puis le relit quand path change ou que reload() est appelé. Une
 * réponse qui arrive après un nouveau changement est ignorée.
 */
export function useApiData<T>(path: string): ApiData<T> {
  const [version, setVersion] = useState(0);
  const [result, setResult] = useState<Result<T> | null>(null);
  const request = `${path}#${version}`;

  useEffect(() => {
    let current = true;
    api.get<T>(path).then(
      (data) => {
        if (current) {
          setResult({ request, data, error: null });
        }
      },
      (error: unknown) => {
        if (current) {
          setResult((previous) => ({
            request,
            data: previous?.data,
            error: toApiError(error),
          }));
        }
      },
    );
    return () => {
      current = false;
    };
  }, [path, request]);

  const reload = useCallback(() => setVersion((value) => value + 1), []);

  return {
    data: result?.data,
    error: result?.request === request ? result.error : null,
    loading: result?.request !== request,
    reload,
  };
}

function toApiError(error: unknown): ApiError {
  return error instanceof ApiError
    ? error
    : new ApiError(0, "Erreur", "Une erreur inattendue est survenue.");
}
