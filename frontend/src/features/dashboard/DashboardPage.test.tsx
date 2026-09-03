import { screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it } from "vitest";
import { mockApi, problem } from "@/test/mockApi";
import { renderApp, resetApp } from "@/test/renderApp";
import { room } from "@/test/rooms";
import type { Dashboard, DayStay } from "./types";

function stay(
  reservationId: string,
  clientName: string,
  roomNumber: string,
  checkInDate: string,
  checkOutDate: string,
  balance: number,
): DayStay {
  return {
    reservationId,
    clientName,
    roomNumber,
    checkInDate,
    checkOutDate,
    numberOfGuests: 2,
    balance,
  };
}

/** Mercredi 7 octobre 2026, date de l'hôtel. */
const DAY: Dashboard = {
  date: "2026-10-07",
  arrivals: [
    stay(
      "resa-dupont",
      "Hélène Dupont",
      "101",
      "2026-10-07",
      "2026-10-09",
      179.8,
    ),
    // Attendu hier : son check-in reste possible jusqu'à son départ
    stay("resa-durand", "Paul Durand", "201", "2026-10-06", "2026-10-09", 0),
  ],
  departures: [
    // Départ prévu hier, sans check-out, et pas encore soldé
    stay("resa-martin", "Jean Martin", "102", "2026-10-04", "2026-10-06", 79.8),
    stay("resa-petit", "Léa Petit", "103", "2026-10-05", "2026-10-07", 0),
  ],
  occupancy: { occupiedRooms: 2, roomsInService: 3, rate: 66.7 },
  roomsToClean: [room("104", "CLEANING")],
};

/** Valeur affichée sous ce libellé, dans les chiffres du jour. */
function tile(label: string) {
  return screen.getByText(label, { selector: "dt" }).nextElementSibling;
}

function rowOf(clientName: string) {
  return within(screen.getByRole("link", { name: clientName }).closest("tr")!);
}

afterEach(resetApp);

describe("DashboardPage", () => {
  it("shows the day of the hotel at a glance", async () => {
    mockApi({ "GET /dashboard/today": DAY });

    renderApp("/");

    expect(
      screen.getByRole("heading", { level: 1, name: "Aujourd'hui" }),
    ).toBeInTheDocument();
    expect(
      await screen.findByText("mercredi 7 octobre 2026"),
    ).toBeInTheDocument();
    expect(tile("Arrivées")).toHaveTextContent("2");
    expect(tile("Départs")).toHaveTextContent("2");
    expect(tile("Occupation")).toHaveTextContent(/^66,7\s%$/);
    expect(screen.getByText("2 chambres sur 3 ce soir")).toBeInTheDocument();
    expect(tile("À nettoyer")).toHaveTextContent("1");
    expect(screen.getByRole("link", { name: "Accueil" })).toHaveAttribute(
      "aria-current",
      "page",
    );
  });

  it("lists the arrivals and departures, flagging the late ones", async () => {
    mockApi({ "GET /dashboard/today": DAY });

    renderApp("/");

    const arrivals = within(
      await screen.findByRole("region", { name: "Arrivées" }),
    );
    expect(
      arrivals.getByRole("link", { name: "Hélène Dupont" }),
    ).toHaveAttribute("href", "/reservations/resa-dupont");
    expect(rowOf("Hélène Dupont").queryByText("en retard")).toBeNull();
    expect(rowOf("Hélène Dupont").getByText(/^179,80\s€$/)).toBeInTheDocument();
    expect(rowOf("Paul Durand").getByText("en retard")).toBeInTheDocument();
    const departures = within(screen.getByRole("region", { name: "Départs" }));
    expect(
      departures.getByRole("link", { name: "Jean Martin" }),
    ).toHaveAttribute("href", "/reservations/resa-martin");
    expect(rowOf("Jean Martin").getByText("en retard")).toBeInTheDocument();
    expect(
      rowOf("Jean Martin").getByText(/^79,80\s€ à encaisser$/),
    ).toBeInTheDocument();
    expect(rowOf("Léa Petit").queryByText("en retard")).toBeNull();
    expect(rowOf("Léa Petit").getByText(/^0,00\s€$/)).toBeInTheDocument();
  });

  it("marks a room as cleaned, which then leaves the list", async () => {
    let cleaned = false;
    const requests = mockApi({
      "GET /dashboard/today": () =>
        Response.json(cleaned ? { ...DAY, roomsToClean: [] } : DAY),
      "PATCH /rooms/room-104/status": () => {
        cleaned = true;
        return Response.json(room("104", "READY"));
      },
    });
    renderApp("/");

    await userEvent.setup().click(
      await screen.findByRole("button", {
        name: "Ménage terminé, chambre 104",
      }),
    );

    expect(
      await screen.findByText("Aucune chambre à nettoyer."),
    ).toBeInTheDocument();
    expect(tile("À nettoyer")).toHaveTextContent("0");
    expect(requests).toContainEqual({
      method: "PATCH",
      path: "/rooms/room-104/status",
      body: { status: "READY" },
    });
  });

  it("shows why a room could not be marked as cleaned", async () => {
    mockApi({
      "GET /dashboard/today": DAY,
      "PATCH /rooms/room-104/status": () => {
        throw new TypeError("Failed to fetch");
      },
    });
    renderApp("/");

    await userEvent.setup().click(
      await screen.findByRole("button", {
        name: "Ménage terminé, chambre 104",
      }),
    );

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Le serveur ne répond pas",
    );
    expect(
      screen.getByRole("button", { name: "Ménage terminé, chambre 104" }),
    ).toBeEnabled();
  });

  it("says when nothing is left to do", async () => {
    mockApi({
      "GET /dashboard/today": {
        ...DAY,
        arrivals: [],
        departures: [],
        occupancy: { occupiedRooms: 0, roomsInService: 3, rate: 0 },
        roomsToClean: [],
      },
    });

    renderApp("/");

    expect(
      await screen.findByText("Aucune arrivée prévue aujourd'hui."),
    ).toBeInTheDocument();
    expect(
      screen.getByText("Aucun départ prévu aujourd'hui."),
    ).toBeInTheDocument();
    expect(screen.getByText("Aucune chambre à nettoyer.")).toBeInTheDocument();
    expect(screen.getByText("0 chambre sur 3 ce soir")).toBeInTheDocument();
  });

  it("shows why the day could not be loaded", async () => {
    mockApi({
      "GET /dashboard/today": () =>
        problem(500, "Une erreur inattendue est survenue côté serveur"),
    });

    renderApp("/");

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Une erreur inattendue est survenue côté serveur",
    );
    expect(
      screen.getByRole("heading", { name: "Aujourd'hui" }),
    ).toBeInTheDocument();
  });
});
