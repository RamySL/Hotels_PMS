import { render } from "@testing-library/react";
import { createMemoryRouter, RouterProvider } from "react-router";
import { vi } from "vitest";
import { routes } from "@/app/routes";
import { endSession, startSession } from "@/features/auth/session";
import type { Role, User } from "@/features/auth/types";

/** Les comptes de démo du backend (migration V2). */
export const DEMO_USERS: Record<Role, User> = {
  ADMIN: {
    id: "1cfd0fb0-2d26-4835-bec8-55f9317b0b14",
    email: "admin@pms.local",
    firstName: "Camille",
    lastName: "Roux",
    role: "ADMIN",
  },
  RECEPTIONIST: {
    id: "df376895-62fc-4376-a4bb-4de71365aa54",
    email: "reception@pms.local",
    firstName: "Lucas",
    lastName: "Bernard",
    role: "RECEPTIONIST",
  },
};

/** Ouvre une session pour ce rôle, puis affiche l'application à cette adresse. */
export function renderApp(path: string, role: Role = "RECEPTIONIST") {
  startSession({
    token: "jeton",
    user: DEMO_USERS[role],
    expiresAt: Date.now() + 60 * 60 * 1000,
  });
  const router = createMemoryRouter(routes, { initialEntries: [path] });
  render(<RouterProvider router={router} />);
  return router;
}

/** À appeler après chaque test : session fermée, stockage vidé, vrai fetch rétabli. */
export function resetApp() {
  endSession("logout");
  localStorage.clear();
  vi.unstubAllGlobals();
}
