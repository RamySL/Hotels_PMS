import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  api,
  ApiError,
  setTokenProvider,
  setUnauthorizedHandler,
} from "./apiClient";

const fetchMock = vi.fn<typeof fetch>();

function jsonResponse(status: number, body: unknown) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

beforeEach(() => {
  vi.stubGlobal("fetch", fetchMock);
});

afterEach(() => {
  fetchMock.mockReset();
  vi.unstubAllGlobals();
  setTokenProvider(() => null);
  setUnauthorizedHandler(() => {});
});

describe("api", () => {
  it("reports a refused token, but not a refused login", async () => {
    const onUnauthorized = vi.fn();
    setUnauthorizedHandler(onUnauthorized);
    fetchMock.mockImplementation(async () =>
      jsonResponse(401, { title: "Unauthorized", status: 401 }),
    );

    await api.post("/auth/login", {}).catch(() => {});
    expect(onUnauthorized).not.toHaveBeenCalled();

    setTokenProvider(() => "jeton");
    await api.get("/rooms").catch(() => {});
    expect(onUnauthorized).toHaveBeenCalledOnce();
  });

  it("sends and reads JSON under /api", async () => {
    fetchMock.mockResolvedValue(jsonResponse(201, { id: "42" }));

    const created = await api.post<{ id: string }>("/clients", {
      firstName: "Jean",
    });

    expect(created).toEqual({ id: "42" });
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("/api/clients");
    expect(init?.method).toBe("POST");
    expect(init?.body).toBe(JSON.stringify({ firstName: "Jean" }));
    expect(init?.headers).toMatchObject({ "Content-Type": "application/json" });
  });

  it("adds the JWT only when a user is signed in", async () => {
    fetchMock.mockImplementation(async () => jsonResponse(200, {}));

    await api.get("/rooms");
    setTokenProvider(() => "jeton");
    await api.get("/rooms");

    expect(fetchMock.mock.calls[0][1]?.headers).not.toHaveProperty(
      "Authorization",
    );
    expect(fetchMock.mock.calls[1][1]?.headers).toMatchObject({
      Authorization: "Bearer jeton",
    });
  });

  it("turns a validation error into an ApiError with the field errors", async () => {
    fetchMock.mockResolvedValue(
      jsonResponse(400, {
        title: "Bad Request",
        status: 400,
        detail: "Requête invalide",
        errors: { lastName: "ne doit pas être vide" },
      }),
    );

    const error = await api.post("/clients", {}).catch((e: unknown) => e);

    expect(error).toBeInstanceOf(ApiError);
    expect(error).toMatchObject({
      status: 400,
      message: "Requête invalide",
      errors: { lastName: "ne doit pas être vide" },
    });
  });

  it("keeps the business message of a conflict", async () => {
    fetchMock.mockResolvedValue(
      jsonResponse(409, {
        title: "Conflict",
        status: 409,
        detail: "La chambre 101 est déjà réservée sur cette période",
      }),
    );

    await expect(api.post("/reservations", {})).rejects.toMatchObject({
      status: 409,
      message: "La chambre 101 est déjà réservée sur cette période",
    });
  });

  it("reports an unreachable server as an ApiError without status", async () => {
    fetchMock.mockRejectedValue(new TypeError("Failed to fetch"));

    await expect(api.get("/ping")).rejects.toMatchObject({
      status: 0,
      title: "Serveur injoignable",
    });
  });

  it("reads a response without content", async () => {
    fetchMock.mockResolvedValue(new Response(null, { status: 204 }));

    await expect(
      api.patch("/rooms/1/status", { status: "READY" }),
    ).resolves.toBeUndefined();
  });
});
