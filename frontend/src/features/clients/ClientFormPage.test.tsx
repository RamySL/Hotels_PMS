import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it } from "vitest";
import { HELENE_DUPONT, JEAN_MARTIN } from "@/test/clients";
import { mockApi, problem } from "@/test/mockApi";
import { pageOf } from "@/test/page";
import { renderApp, resetApp } from "@/test/renderApp";

afterEach(resetApp);

describe("ClientFormPage", () => {
  it("creates a client met at the desk, without email or phone", async () => {
    const requests = mockApi({
      "POST /clients": JEAN_MARTIN,
      "GET /clients?search=Martin&page=0&size=20": pageOf([JEAN_MARTIN]),
    });
    const router = renderApp("/clients/nouveau");
    const user = userEvent.setup();

    await user.type(screen.getByLabelText("Prénom"), "Jean");
    await user.type(screen.getByLabelText("Nom"), "Martin");
    await user.click(screen.getByRole("button", { name: "Enregistrer" }));

    expect(
      await screen.findByRole("cell", { name: "Martin" }),
    ).toBeInTheDocument();
    expect(router.state.location.search).toBe("?recherche=Martin");
    expect(requests).toContainEqual({
      method: "POST",
      path: "/clients",
      body: { firstName: "Jean", lastName: "Martin", email: null, phone: null },
    });
  });

  it("edits a client from its current details", async () => {
    const requests = mockApi({
      "GET /clients/client-dupont": HELENE_DUPONT,
      "PUT /clients/client-dupont": {
        ...HELENE_DUPONT,
        phone: "07 11 22 33 44",
      },
      "GET /clients?search=Dupont&page=0&size=20": pageOf([HELENE_DUPONT]),
    });
    renderApp("/clients/client-dupont/modifier");

    const phone = await screen.findByLabelText("Téléphone (facultatif)");
    expect(phone).toHaveValue("06 12 34 56 78");
    expect(screen.getByLabelText("Prénom")).toHaveValue("Hélène");
    const user = userEvent.setup();
    await user.clear(phone);
    await user.type(phone, "07 11 22 33 44");
    await user.click(screen.getByRole("button", { name: "Enregistrer" }));

    await screen.findByRole("cell", { name: "Dupont" });
    expect(requests).toContainEqual({
      method: "PUT",
      path: "/clients/client-dupont",
      body: {
        firstName: "Hélène",
        lastName: "Dupont",
        email: "helene.dupont@example.com",
        phone: "07 11 22 33 44",
      },
    });
  });

  it("shows a phone number refused by the API under that field", async () => {
    mockApi({
      "POST /clients": () =>
        problem(400, "Un ou plusieurs champs sont invalides", {
          phone: "ne doit contenir que des chiffres, des espaces et + . ( ) -",
        }),
    });
    renderApp("/clients/nouveau");
    const user = userEvent.setup();

    await user.type(screen.getByLabelText("Prénom"), "Jean");
    await user.type(screen.getByLabelText("Nom"), "Martin");
    await user.type(screen.getByLabelText("Téléphone (facultatif)"), "06-AB");
    await user.click(screen.getByRole("button", { name: "Enregistrer" }));

    expect(
      await screen.findByLabelText("Téléphone (facultatif)"),
    ).toHaveAccessibleDescription(
      "ne doit contenir que des chiffres, des espaces et + . ( ) -",
    );
  });

  it("says when the client does not exist", async () => {
    mockApi({
      "GET /clients/inconnu": () =>
        problem(404, "Client introuvable : inconnu"),
    });

    renderApp("/clients/inconnu/modifier");

    expect(
      await screen.findByText("Ce client n'existe pas."),
    ).toBeInTheDocument();
  });
});
