import { screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it } from "vitest";
import { mockApi, problem } from "@/test/mockApi";
import { renderApp, resetApp } from "@/test/renderApp";
import type { Room, RoomStatus } from "./types";

const double = {
  id: "type-double",
  name: "Double",
  description: null,
  basePrice: 89.9,
  capacity: 2,
};

function room(roomNumber: string, status: RoomStatus): Room {
  return {
    id: `room-${roomNumber}`,
    roomNumber,
    floor: 1,
    status,
    roomType: double,
  };
}

function rowOf(roomNumber: string) {
  return within(screen.getByRole("cell", { name: roomNumber }).closest("tr")!);
}

afterEach(resetApp);

describe("RoomsPage", () => {
  it("lists the rooms with their type and status", async () => {
    mockApi({
      "GET /rooms": [
        room("101", "READY"),
        room("102", "CLEANING"),
        room("103", "OCCUPIED"),
      ],
    });

    renderApp("/chambres");

    expect(
      await screen.findByRole("cell", { name: "101" }),
    ).toBeInTheDocument();
    expect(rowOf("101").getByText("Double")).toBeInTheDocument();
    expect(rowOf("101").getByText("Prête")).toBeInTheDocument();
    expect(rowOf("102").getByText("À nettoyer")).toBeInTheDocument();
    expect(rowOf("103").getByText("Occupée")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Chambres" })).toHaveAttribute(
      "aria-current",
      "page",
    );
  });

  it("marks a room as ready once cleaned, then reloads the list", async () => {
    let cleaned = false;
    const requests = mockApi({
      "GET /rooms": () =>
        Response.json([room("102", cleaned ? "READY" : "CLEANING")]),
      "PATCH /rooms/room-102/status": () => {
        cleaned = true;
        return Response.json(room("102", "READY"));
      },
    });
    renderApp("/chambres");

    await userEvent.setup().click(
      await screen.findByRole("button", {
        name: "Ménage terminé, chambre 102",
      }),
    );

    expect(await rowOf("102").findByText("Prête")).toBeInTheDocument();
    expect(requests).toContainEqual({
      method: "PATCH",
      path: "/rooms/room-102/status",
      body: { status: "READY" },
    });
  });

  it("offers no action on an occupied room", async () => {
    mockApi({ "GET /rooms": [room("103", "OCCUPIED")] });

    renderApp("/chambres");

    await screen.findByRole("cell", { name: "103" });
    expect(rowOf("103").queryByRole("button")).toBeNull();
    expect(rowOf("103").getByText("Libérée au check-out")).toBeInTheDocument();
  });

  it("shows why the API refused a change", async () => {
    mockApi({
      "GET /rooms": [room("101", "READY")],
      "PATCH /rooms/room-101/status": () =>
        problem(
          409,
          "La chambre 101 est occupée : son statut changera au check-out",
        ),
    });
    renderApp("/chambres");

    await userEvent.setup().click(
      await screen.findByRole("button", {
        name: "Mettre hors service, chambre 101",
      }),
    );

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "La chambre 101 est occupée",
    );
  });

  it("filters the rooms by status and keeps the filter in the address", async () => {
    const requests = mockApi({
      "GET /rooms": [room("101", "READY"), room("102", "CLEANING")],
      "GET /rooms?status=CLEANING": [room("102", "CLEANING")],
    });
    const router = renderApp("/chambres");
    await screen.findByRole("cell", { name: "101" });

    await userEvent
      .setup()
      .selectOptions(screen.getByLabelText("Statut"), "CLEANING");

    await waitFor(() =>
      expect(screen.queryByRole("cell", { name: "101" })).toBeNull(),
    );
    expect(requests.at(-1)?.path).toBe("/rooms?status=CLEANING");
    expect(router.state.location.search).toBe("?statut=CLEANING");
  });

  it("takes the filter from the address", async () => {
    const requests = mockApi({ "GET /rooms?status=OUT_OF_SERVICE": [] });

    renderApp("/chambres?statut=OUT_OF_SERVICE");

    expect(
      await screen.findByText("Aucune chambre dans ce statut."),
    ).toBeInTheDocument();
    expect(screen.getByLabelText("Statut")).toHaveValue("OUT_OF_SERVICE");
    expect(requests.map((request) => request.path)).toEqual([
      "/rooms?status=OUT_OF_SERVICE",
    ]);
  });
});
