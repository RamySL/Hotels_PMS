import { act, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { createMemoryRouter, RouterProvider } from "react-router";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { endSession, getSession, startSession } from "@/features/auth/session";
import type { User } from "@/features/auth/types";
import { routes } from "./routes";

const receptionist: User = {
  id: "df376895-62fc-4376-a4bb-4de71365aa54",
  email: "reception@pms.local",
  firstName: "Lucas",
  lastName: "Bernard",
  role: "RECEPTIONIST",
};

// Seule la connexion obtient une réponse : l'appel du tableau de bord à l'API reste en attente
beforeEach(() => {
  vi.stubGlobal(
    "fetch",
    vi.fn<typeof fetch>(async (input) =>
      String(input).endsWith("/auth/login")
        ? Response.json({
            accessToken: "jeton",
            tokenType: "Bearer",
            expiresIn: 28800,
            user: receptionist,
          })
        : new Promise<Response>(() => {}),
    ),
  );
});

afterEach(() => {
  endSession("logout");
  localStorage.clear();
  vi.unstubAllGlobals();
});

function signedIn() {
  startSession({
    token: "jeton",
    user: receptionist,
    expiresAt: Date.now() + 60_000,
  });
}

function renderAt(path: string) {
  render(
    <RouterProvider
      router={createMemoryRouter(routes, { initialEntries: [path] })}
    />,
  );
}

describe("routes", () => {
  it("shows the home page inside the layout, with the person signed in", () => {
    signedIn();

    renderAt("/");

    expect(screen.getByRole("link", { name: "PMS Hôtel" })).toHaveAttribute(
      "href",
      "/",
    );
    expect(
      screen.getByRole("heading", { name: "Aujourd'hui" }),
    ).toBeInTheDocument();
    expect(screen.getByText("Lucas Bernard")).toBeInTheDocument();
    expect(screen.getByText("Réception")).toBeInTheDocument();
  });

  it("shows a not-found page leading back home for an unknown address", () => {
    signedIn();

    renderAt("/adresse-inconnue");

    expect(
      screen.getByRole("heading", { name: "Page introuvable" }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: "Retour à l'accueil" }),
    ).toHaveAttribute("href", "/");
  });

  it("sends a visitor to the login page, then back to the page asked for", async () => {
    renderAt("/adresse-inconnue");

    const user = userEvent.setup();
    await user.type(screen.getByLabelText("Email"), "reception@pms.local");
    await user.type(screen.getByLabelText("Mot de passe"), "Reception123!");
    await user.click(screen.getByRole("button", { name: "Se connecter" }));

    expect(
      await screen.findByRole("heading", { name: "Page introuvable" }),
    ).toBeInTheDocument();
  });

  it("leads back to the login page after logging out", async () => {
    signedIn();
    renderAt("/");

    await userEvent
      .setup()
      .click(screen.getByRole("button", { name: "Se déconnecter" }));

    expect(
      await screen.findByRole("button", { name: "Se connecter" }),
    ).toBeInTheDocument();
    expect(getSession()).toBeNull();
    expect(localStorage.getItem("pms.session")).toBeNull();
  });

  it("leads to the login page with a notice when the session expires", async () => {
    signedIn();
    renderAt("/");

    act(() => endSession("expired"));

    expect(await screen.findByRole("status")).toHaveTextContent(
      "Votre session a expiré",
    );
  });
});
