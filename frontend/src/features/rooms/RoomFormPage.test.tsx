import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it } from "vitest";
import { mockApi, problem } from "@/test/mockApi";
import { renderApp, resetApp } from "@/test/renderApp";
import { DOUBLE, room, SUITE } from "@/test/rooms";

async function fillNewRoom() {
  const user = userEvent.setup();
  await user.type(await screen.findByLabelText("Numéro"), "101");
  await user.type(screen.getByLabelText("Étage"), "1");
  await user.selectOptions(screen.getByLabelText("Type"), "type-double");
  await user.click(screen.getByRole("button", { name: "Enregistrer" }));
}

afterEach(resetApp);

describe("RoomFormPage", () => {
  it("creates a room of the chosen type and goes back to the rooms", async () => {
    const requests = mockApi({
      "GET /room-types": [DOUBLE, SUITE],
      "POST /rooms": room("101"),
      "GET /rooms": [room("101")],
    });
    renderApp("/chambres/nouvelle", "ADMIN");

    await fillNewRoom();

    expect(
      await screen.findByRole("heading", { name: "Chambres" }),
    ).toBeInTheDocument();
    expect(requests).toContainEqual({
      method: "POST",
      path: "/rooms",
      body: { roomNumber: "101", floor: 1, roomTypeId: "type-double" },
    });
  });

  it("offers each type with its price and capacity", async () => {
    mockApi({ "GET /room-types": [DOUBLE] });

    renderApp("/chambres/nouvelle", "ADMIN");

    expect(
      await screen.findByRole("option", {
        name: /^Double — 89,90\s€ — 2 pers\.$/,
      }),
    ).toBeInTheDocument();
  });

  it("edits a room from its current values", async () => {
    const requests = mockApi({
      "GET /room-types": [DOUBLE, SUITE],
      "GET /rooms": [room("101")],
      "PUT /rooms/room-101": room("101", "READY", SUITE),
    });
    renderApp("/chambres/room-101/modifier", "ADMIN");

    expect(await screen.findByLabelText("Numéro")).toHaveValue("101");
    expect(screen.getByLabelText("Étage")).toHaveValue(1);
    expect(screen.getByLabelText("Type")).toHaveValue("type-double");
    const user = userEvent.setup();
    await user.selectOptions(screen.getByLabelText("Type"), "type-suite");
    await user.click(screen.getByRole("button", { name: "Enregistrer" }));

    await screen.findByRole("heading", { name: "Chambres" });
    expect(requests).toContainEqual({
      method: "PUT",
      path: "/rooms/room-101",
      body: { roomNumber: "101", floor: 1, roomTypeId: "type-suite" },
    });
  });

  it("asks for a room type first when there is none", async () => {
    mockApi({ "GET /room-types": [] });

    renderApp("/chambres/nouvelle", "ADMIN");

    expect(
      await screen.findByRole("link", { name: "Créer un type de chambre" }),
    ).toHaveAttribute("href", "/chambres/types/nouveau");
  });

  it("shows that the room number is already taken", async () => {
    mockApi({
      "GET /room-types": [DOUBLE],
      "POST /rooms": () => problem(409, "La chambre 101 existe déjà"),
    });
    renderApp("/chambres/nouvelle", "ADMIN");

    await fillNewRoom();

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "La chambre 101 existe déjà",
    );
  });

  it("is kept from the reception", () => {
    mockApi({});

    renderApp("/chambres/nouvelle", "RECEPTIONIST");

    expect(
      screen.getByRole("heading", { name: "Page réservée à l'administration" }),
    ).toBeInTheDocument();
  });
});
