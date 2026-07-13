import {
  setTokenProvider,
  setUnauthorizedHandler,
} from "@/shared/lib/apiClient";
import type { Role, User } from "./types";

/** Session de la personne connectée : son jeton, son profil et la fin de validité du jeton. */
export interface Session {
  token: string;
  user: User;
  /** Fin de validité du jeton, en millisecondes depuis l'époque Unix. */
  expiresAt: number;
}

/** Pourquoi la dernière session s'est terminée : la page de connexion l'explique. */
export type EndReason = "logout" | "expired";

/** Clé du localStorage : les risques de ce stockage sont décrits dans l'ADR 0008. */
const STORAGE_KEY = "pms.session";

const ROLES: readonly Role[] = ["ADMIN", "RECEPTIONIST"];

let current: Session | null = null;
let lastEndReason: EndReason | null = null;
let expiryTimer: ReturnType<typeof setTimeout> | undefined;
let initialized = false;
const listeners = new Set<() => void>();

export function getSession(): Session | null {
  return current;
}

export function getLastEndReason(): EndReason | null {
  return lastEndReason;
}

export function subscribeToSession(listener: () => void): () => void {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
}

/** Ouvre la session après une connexion réussie, et la garde pour les rechargements et les autres onglets. */
export function startSession(session: Session): void {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
  adopt(session);
}

/** Termine la session : déconnexion, ou jeton expiré ou refusé par l'API. */
export function endSession(reason: EndReason): void {
  localStorage.removeItem(STORAGE_KEY);
  drop(reason);
}

/**
 * À l'ouverture de l'application : reprend la session gardée, puis la relie (une seule
 * fois) au client HTTP et aux autres onglets.
 */
export function initSession(): void {
  const stored = readStoredSession();
  if (stored) {
    adopt(stored);
  } else {
    localStorage.removeItem(STORAGE_KEY);
    clearTimeout(expiryTimer);
    current = null;
    notify();
  }

  if (initialized) {
    return;
  }
  initialized = true;
  window.addEventListener("storage", syncFromOtherTab);
  setTokenProvider(() => current?.token ?? null);
  setUnauthorizedHandler(() => endSession("expired"));
}

function adopt(session: Session): void {
  clearTimeout(expiryTimer);
  current = session;
  lastEndReason = null;
  expiryTimer = setTimeout(
    () => endSession("expired"),
    session.expiresAt - Date.now(),
  );
  notify();
}

function drop(reason: EndReason): void {
  clearTimeout(expiryTimer);
  current = null;
  lastEndReason = reason;
  notify();
}

function notify(): void {
  listeners.forEach((listener) => listener());
}

// Une connexion ou une déconnexion dans un autre onglet s'applique aussi à celui-ci
function syncFromOtherTab(event: StorageEvent): void {
  if (event.key !== STORAGE_KEY && event.key !== null) {
    return;
  }
  const stored = readStoredSession();
  if (stored) {
    adopt(stored);
  } else if (current) {
    drop(Date.now() >= current.expiresAt ? "expired" : "logout");
  }
}

/** Session gardée, si elle est lisible et pas encore expirée. */
function readStoredSession(): Session | null {
  const raw = localStorage.getItem(STORAGE_KEY);
  if (raw === null) {
    return null;
  }
  try {
    const value: unknown = JSON.parse(raw);
    return isSession(value) && value.expiresAt > Date.now() ? value : null;
  } catch {
    return null;
  }
}

function isSession(value: unknown): value is Session {
  if (typeof value !== "object" || value === null) {
    return false;
  }
  const { token, user, expiresAt } = value as Partial<Session>;
  return (
    typeof token === "string" &&
    typeof expiresAt === "number" &&
    typeof user === "object" &&
    user !== null &&
    typeof user.id === "string" &&
    typeof user.email === "string" &&
    typeof user.firstName === "string" &&
    typeof user.lastName === "string" &&
    ROLES.includes(user.role)
  );
}
