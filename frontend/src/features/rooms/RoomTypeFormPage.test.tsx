import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it } from "vitest";
import { mockApi, problem } from "@/test/mockApi";
import { renderApp, resetApp } from "@/test/renderApp";
import { DOUBLE } from "@/test/rooms";

async function fillNewType() {
  const user = userEvent.setup();
  await user.type(screen.getByLabelText("Nom"), "Double");
  await user.type(screen.getByLabelText("Prix par nuit (€)"), "89.90");
  await user.type(screen.getByLabelText("Capacité (personnes)"), "2");
  await user.click(screen.getByRole("button", { name: "Enregistrer" }));
}

afterEach(resetApp);

describe("RoomTypeFormPage", () => {
  it("creates a type and goes back to the types", async () => {
    const requests = mockApi({
      "POST /room-types": DOUBLE,
      "GET /room-types": [DOUBLE],
    });
    renderApp("/chambres/types/nouveau", "ADMIN");

    await fillNewType();

    expect(
      await screen.findByRole("heading", { name: "Types et tarifs" }),
    ).toBeInTheDocument();
    expect(requests).toContainEqual({
      method: "POST",
      path: "/room-types",
      body: { name: "Double", description: null, basePrice: 89.9, capacity: 2 },
    });
  });

  it("edits a type from its current values", async () => {
    const requests = mockApi({
      "GET /room-types": [DOUBLE],
      "PUT /room-types/type-double": { ...DOUBLE, basePrice: 95 },
    });
    renderApp("/chambres/types/type-double/modifier", "ADMIN");

    const price = await screen.findByLabelText("Prix par nuit (€)");
    expect(price).toHaveValue(89.9);
    expect(screen.getByLabelText("Nom")).toHaveValue("Double");
    const user = userEvent.setup();
    await user.clear(price);
    await user.type(price, "95");
    await user.click(screen.getByRole("button", { name: "Enregistrer" }));

    await screen.findByRole("heading", { name: "Types et tarifs" });
    expect(requests).toContainEqual({
      method: "PUT",
      path: "/room-types/type-double",
      body: { name: "Double", description: null, basePrice: 95, capacity: 2 },
    });
  });

  it("shows that the name is already taken", async () => {
    mockApi({
      "POST /room-types": () =>
        problem(409, "Un type de chambre nommé « Double » existe déjà"),
    });
    renderApp("/chambres/types/nouveau", "ADMIN");

    await fillNewType();

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Un type de chambre nommé « Double » existe déjà",
    );
  });

  it("shows a field refused by the API under that field", async () => {
    mockApi({
      "POST /room-types": () =>
        problem(400, "Un ou plusieurs champs sont invalides", {
          capacity: "doit être supérieur à 0",
        }),
    });
    renderApp("/chambres/types/nouveau", "ADMIN");

    await fillNewType();

    expect(
      await screen.findByLabelText("Capacité (personnes)"),
    ).toHaveAccessibleDescription("doit être supérieur à 0");
  });
});
