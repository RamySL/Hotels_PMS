import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { api } from "@/shared/lib/apiClient";
import { login } from "./api";
import {
  endSession,
  getLastEndReason,
  getSession,
  initSession,
  startSession,
  type Session,
} from "./session";
import type { User } from "./types";

const STORAGE_KEY = "pms.session";
const NOW = new Date("2026-10-05T08:00:00Z").getTime();
const EIGHT_HOURS = 8 * 60 * 60 * 1000;

const receptionist: User = {
  id: "df376895-62fc-4376-a4bb-4de71365aa54",
  email: "reception@pms.local",
  firstName: "Lucas",
  lastName: "Bernard",
  role: "RECEPTIONIST",
};

const fetchMock = vi.fn<typeof fetch>();

function session(expiresAt = NOW + EIGHT_HOURS): Session {
  return { token: "jeton", user: receptionist, expiresAt };
}

function store(value: unknown) {
  localStorage.setItem(
    STORAGE_KEY,
    typeof value === "string" ? value : JSON.stringify(value),
  );
}

function unauthorized(detail: string) {
  return Response.json(
    { title: "Unauthorized", status: 401, detail },
    { status: 401 },
  );
}

beforeEach(() => {
  // La lecture des réponses fetch s'appuie sur d'autres minuteurs : ils restent réels
  vi.useFakeTimers({
    now: NOW,
    toFake: ["setTimeout", "clearTimeout", "Date"],
  });
  vi.stubGlobal("fetch", fetchMock);
  initSession();
});

afterEach(() => {
  endSession("logout");
  localStorage.clear();
  fetchMock.mockReset();
  vi.unstubAllGlobals();
  vi.useRealTimers();
});

describe("session", () => {
  it("starts with the login and sends the token with the next requests", async () => {
    fetchMock.mockResolvedValueOnce(
      Response.json({
        accessToken: "jeton",
        tokenType: "Bearer",
        expiresIn: 28800,
        user: receptionist,
      }),
    );
    fetchMock.mockResolvedValueOnce(Response.json([]));

    await login("reception@pms.local", "Reception123!");
    await api.get("/rooms");

    expect(getSession()).toEqual(session());
    expect(JSON.parse(localStorage.getItem(STORAGE_KEY) ?? "null")).toEqual(
      session(),
    );
    expect(fetchMock.mock.calls[1][1]?.headers).toMatchObject({
      Authorization: "Bearer jeton",
    });
  });

  it("is restored when the application opens again", () => {
    store(session());

    initSession();

    expect(getSession()).toEqual(session());
  });

  it("ignores a stored session that has expired or cannot be read", () => {
    store(session(NOW - 1));
    initSession();
    expect(getSession()).toBeNull();
    expect(localStorage.getItem(STORAGE_KEY)).toBeNull();

    store("pas du JSON");
    initSession();
    expect(getSession()).toBeNull();

    store({ ...session(), user: { ...receptionist, role: "MANAGER" } });
    initSession();
    expect(getSession()).toBeNull();
  });

  it("ends by itself when the token expires", () => {
    startSession(session(NOW + 60_000));

    vi.advanceTimersByTime(59_999);
    expect(getSession()).not.toBeNull();
    vi.advanceTimersByTime(1);

    expect(getSession()).toBeNull();
    expect(getLastEndReason()).toBe("expired");
  });

  it("ends when the API refuses its token", async () => {
    startSession(session());
    fetchMock.mockResolvedValue(unauthorized("Jeton invalide ou expiré"));

    await expect(api.get("/rooms")).rejects.toMatchObject({ status: 401 });

    expect(getSession()).toBeNull();
    expect(getLastEndReason()).toBe("expired");
    expect(localStorage.getItem(STORAGE_KEY)).toBeNull();
  });

  it("is not opened by a refused login", async () => {
    fetchMock.mockResolvedValue(
      unauthorized("Email ou mot de passe incorrect"),
    );

    await expect(
      login("reception@pms.local", "mauvais mot de passe"),
    ).rejects.toMatchObject({ message: "Email ou mot de passe incorrect" });

    expect(getSession()).toBeNull();
    expect(getLastEndReason()).not.toBe("expired");
  });

  it("follows a logout and a login made in another tab", () => {
    startSession(session());

    localStorage.removeItem(STORAGE_KEY);
    window.dispatchEvent(
      new StorageEvent("storage", { key: STORAGE_KEY, newValue: null }),
    );
    expect(getSession()).toBeNull();
    expect(getLastEndReason()).toBe("logout");

    store(session());
    window.dispatchEvent(
      new StorageEvent("storage", {
        key: STORAGE_KEY,
        newValue: JSON.stringify(session()),
      }),
    );
    expect(getSession()).toEqual(session());
  });
});
