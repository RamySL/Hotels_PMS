import { vi } from "vitest";

export interface ApiRequest {
  method: string;
  /** Chemin sans le préfixe /api, paramètres compris : "/rooms?status=READY". */
  path: string;
  body: unknown;
}

type Reply = (request: ApiRequest) => Response | Promise<Response>;

/**
 * Remplace fetch. Chaque requête reçoit la réponse prévue pour « MÉTHODE /chemin » :
 * une valeur devient une réponse JSON 200, une fonction construit la réponse. Une
 * requête imprévue reçoit un 501 explicite. Renvoie les requêtes reçues, dans l'ordre.
 */
export function mockApi(replies: Record<string, unknown>): ApiRequest[] {
  const requests: ApiRequest[] = [];
  vi.stubGlobal(
    "fetch",
    vi.fn<typeof fetch>(async (input, init) => {
      const request: ApiRequest = {
        method: init?.method ?? "GET",
        path: String(input).replace(/^\/api/, ""),
        body: init?.body ? JSON.parse(String(init.body)) : undefined,
      };
      requests.push(request);
      const reply = replies[`${request.method} ${request.path}`];
      if (reply === undefined) {
        return Response.json(
          {
            detail: `Aucune réponse prévue pour ${request.method} ${request.path}`,
          },
          { status: 501 },
        );
      }
      return typeof reply === "function"
        ? (reply as Reply)(request)
        : Response.json(reply);
    }),
  );
  return requests;
}

/** Réponse d'erreur au format du backend (RFC 7807). */
export function problem(
  status: number,
  detail: string,
  errors?: Record<string, string>,
): Response {
  return Response.json({ status, detail, errors }, { status });
}
