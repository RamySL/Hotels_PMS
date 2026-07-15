import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { createMemoryRouter, RouterProvider } from "react-router";
import { afterEach, describe, expect, it, vi } from "vitest";
import { LoginPage } from "./LoginPage";
import { endSession, startSession } from "./session";
import type { User } from "./types";

const receptionist: User = {
  id: "df376895-62fc-4376-a4bb-4de71365aa54",
  email: "reception@pms.local",
  firstName: "Lucas",
  lastName: "Bernard",
  role: "RECEPTIONIST",
};

function renderLoginPage(state?: unknown) {
  const router = createMemoryRouter(
    [
      { path: "/login", element: <LoginPage /> },
      { path: "/", element: <p>Page d'accueil</p> },
      { path: "/reservations", element: <p>Page des réservations</p> },
    ],
    { initialEntries: [{ pathname: "/login", state }] },
  );
  render(<RouterProvider router={router} />);
}

function answer(response: Response | Promise<Response>) {
  vi.stubGlobal(
    "fetch",
    vi.fn<typeof fetch>(async () => response),
  );
}

async function signIn(password = "Reception123!") {
  const user = userEvent.setup();
  await user.type(screen.getByLabelText("Email"), "reception@pms.local");
  await user.type(screen.getByLabelText("Mot de passe"), password);
  await user.click(screen.getByRole("button", { name: "Se connecter" }));
}

afterEach(() => {
  endSession("logout");
  localStorage.clear();
  vi.unstubAllGlobals();
});

describe("LoginPage", () => {
  it("signs in and goes on to the page asked for at first", async () => {
    answer(
      Response.json({
        accessToken: "jeton",
        tokenType: "Bearer",
        expiresIn: 28800,
        user: receptionist,
      }),
    );
    renderLoginPage({ from: { pathname: "/reservations" } });

    await signIn();

    expect(
      await screen.findByText("Page des réservations"),
    ).toBeInTheDocument();
  });

  it("shows why the sign-in was refused and keeps the email", async () => {
    answer(
      Response.json(
        { status: 401, detail: "Email ou mot de passe incorrect" },
        { status: 401 },
      ),
    );
    renderLoginPage();

    await signIn("mauvais mot de passe");

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Email ou mot de passe incorrect",
    );
    expect(screen.getByLabelText("Email")).toHaveValue("reception@pms.local");
    expect(screen.getByLabelText("Mot de passe")).toHaveValue("");
  });

  it("explains that the server cannot be reached", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn<typeof fetch>().mockRejectedValue(new TypeError("Failed to fetch")),
    );
    renderLoginPage();

    await signIn();

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Le serveur ne répond pas",
    );
  });

  it("shows the field errors returned by the API", async () => {
    answer(
      Response.json(
        {
          status: 400,
          detail: "Un ou plusieurs champs sont invalides",
          errors: { email: "doit être une adresse électronique valide" },
        },
        { status: 400 },
      ),
    );
    renderLoginPage();

    await signIn();

    expect(await screen.findByLabelText("Email")).toHaveAccessibleDescription(
      "doit être une adresse électronique valide",
    );
  });

  it("disables the button while signing in", async () => {
    answer(new Promise<Response>(() => {}));
    renderLoginPage();

    await signIn();

    expect(screen.getByRole("button", { name: "Connexion…" })).toBeDisabled();
  });

  it("tells that the previous session has expired", () => {
    endSession("expired");

    renderLoginPage();

    expect(screen.getByRole("status")).toHaveTextContent(
      "Votre session a expiré",
    );
  });

  it("sends someone already signed in to the home page", () => {
    startSession({
      token: "jeton",
      user: receptionist,
      expiresAt: Date.now() + 60_000,
    });

    renderLoginPage();

    expect(screen.getByText("Page d'accueil")).toBeInTheDocument();
  });
});
