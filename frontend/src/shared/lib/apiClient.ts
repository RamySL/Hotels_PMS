/** Corps d'erreur renvoyé par le backend (RFC 7807, ProblemDetail). */
interface ProblemDetail {
  title?: string;
  detail?: string;
  errors?: Record<string, string>;
}

/**
 * Échec d'un appel à l'API. status vaut 0 quand le serveur n'a pas répondu ;
 * errors détaille, champ par champ, une erreur de validation (400).
 */
export class ApiError extends Error {
  readonly status: number;
  readonly title: string;
  readonly errors: Record<string, string>;

  constructor(
    status: number,
    title: string,
    detail: string,
    errors: Record<string, string> = {},
  ) {
    super(detail);
    this.name = "ApiError";
    this.status = status;
    this.title = title;
    this.errors = errors;
  }
}

type TokenProvider = () => string | null;

let tokenProvider: TokenProvider = () => null;

/** Source du jeton JWT ajouté à chaque requête : celui de l'utilisateur connecté. */
export function setTokenProvider(provider: TokenProvider): void {
  tokenProvider = provider;
}

let onUnauthorized: () => void = () => {};

/** Appelé quand l'API refuse le jeton envoyé (401) : il a expiré ou n'est plus valide. */
export function setUnauthorizedHandler(handler: () => void): void {
  onUnauthorized = handler;
}

async function request<T>(
  method: string,
  path: string,
  body?: unknown,
): Promise<T> {
  const headers: Record<string, string> = { Accept: "application/json" };
  if (body !== undefined) {
    headers["Content-Type"] = "application/json";
  }
  const token = tokenProvider();
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  let response: Response;
  try {
    response = await fetch(`/api${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch {
    throw new ApiError(
      0,
      "Serveur injoignable",
      "Le serveur ne répond pas. Vérifiez la connexion, puis réessayez.",
    );
  }

  if (!response.ok) {
    // Sans jeton (un login refusé, par exemple), un 401 ne met fin à aucune session
    if (response.status === 401 && token) {
      onUnauthorized();
    }
    throw await toApiError(response);
  }
  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}

async function toApiError(response: Response): Promise<ApiError> {
  const problem: ProblemDetail = await response.json().catch(() => ({}));
  return new ApiError(
    response.status,
    problem.title ?? `Erreur ${response.status}`,
    problem.detail ?? "La requête a échoué.",
    problem.errors ?? {},
  );
}

/** Appels au backend, sur la même origine : /api est relayé par le proxy. */
export const api = {
  get: <T>(path: string) => request<T>("GET", path),
  post: <T>(path: string, body?: unknown) => request<T>("POST", path, body),
  put: <T>(path: string, body: unknown) => request<T>("PUT", path, body),
  patch: <T>(path: string, body?: unknown) => request<T>("PATCH", path, body),
};
