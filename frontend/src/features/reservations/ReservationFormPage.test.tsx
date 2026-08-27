import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { HELENE_DUPONT, JEAN_MARTIN } from "@/test/clients";
import { mockApi, problem, type ApiRequest } from "@/test/mockApi";
import { pageOf } from "@/test/page";
import { renderApp, resetApp } from "@/test/renderApp";
import { DUPONT_STAY, invoiceFor } from "@/test/reservations";
import type { AvailableRoom, Reservation, ReservationRequest } from "./types";

function available(
  roomNumber: string,
  roomTypeName: string,
  capacity: number,
  pricePerNight: number,
  nights = 1,
): AvailableRoom {
  return {
    roomId: `room-${roomNumber}`,
    roomNumber,
    floor: 1,
    roomTypeName,
    capacity,
    pricePerNight,
    nights,
    totalPrice: pricePerNight * nights,
  };
}

// Aujourd'hui, mercredi 7 octobre : le formulaire propose une nuit, jusqu'au jeudi 8
const ONE_NIGHT =
  "GET /rooms/availability?checkIn=2026-10-07&checkOut=2026-10-08";
const TWO_NIGHTS =
  "GET /rooms/availability?checkIn=2026-10-07&checkOut=2026-10-09";

/** La fiche qui s'ouvre une fois la réservation enregistrée. */
function fileOf(reservation: Reservation) {
  return {
    [`GET /reservations/${reservation.id}`]: reservation,
    [`GET /invoices?reservationId=${reservation.id}`]: pageOf([
      invoiceFor(reservation),
    ]),
  };
}

beforeEach(() => {
  vi.setSystemTime(new Date(2026, 9, 7, 10, 0));
});

afterEach(() => {
  resetApp();
  vi.useRealTimers();
});

describe("ReservationFormPage", () => {
  it("books a room for a client found by name, then opens the reservation", async () => {
    const requests = mockApi({
      [ONE_NIGHT]: [available("101", "Double", 2, 89.9)],
      [TWO_NIGHTS]: [available("101", "Double", 2, 89.9, 2)],
      "GET /clients?search=dupont&page=0&size=5": pageOf([HELENE_DUPONT]),
      "POST /reservations": () => Response.json(DUPONT_STAY, { status: 201 }),
      ...fileOf(DUPONT_STAY),
    });
    renderApp("/reservations/nouvelle");
    const user = userEvent.setup();

    const departure = screen.getByLabelText("Départ");
    await user.clear(departure);
    await user.type(departure, "2026-10-09");
    await user.clear(screen.getByLabelText("Personnes"));
    await user.type(screen.getByLabelText("Personnes"), "2");
    await user.click(
      await screen.findByRole("radio", { name: /^Chambre 101 .*179,80/ }),
    );
    await user.type(screen.getByLabelText("Rechercher un client"), "dupont");
    await user.click(
      await screen.findByRole("button", { name: /^Hélène Dupont/ }),
    );
    await user.click(screen.getByRole("button", { name: "Réserver" }));

    expect(
      await screen.findByRole("heading", { name: "Hélène Dupont" }),
    ).toBeInTheDocument();
    expect(requests).toContainEqual({
      method: "POST",
      path: "/reservations",
      body: {
        clientId: "client-dupont",
        roomId: "room-101",
        checkInDate: "2026-10-07",
        checkOutDate: "2026-10-09",
        numberOfGuests: 2,
      },
    });
  });

  it("only offers the free rooms large enough, with the price of the stay", async () => {
    mockApi({
      [ONE_NIGHT]: [
        available("101", "Double", 2, 89.9),
        available("201", "Suite", 4, 189),
      ],
    });
    renderApp("/reservations/nouvelle");
    const user = userEvent.setup();

    expect(
      await screen.findByRole("radio", { name: /^Chambre 101/ }),
    ).toHaveAccessibleName(
      /^Chambre 101 Double · 2 pers\. 89,90\s€ 1 nuit × 89,90\s€$/,
    );
    await user.clear(screen.getByLabelText("Personnes"));
    await user.type(screen.getByLabelText("Personnes"), "3");

    expect(screen.getAllByRole("radio")).toHaveLength(1);
    expect(screen.getByRole("radio")).toHaveAccessibleName(
      /^Chambre 201 Suite · 4 pers\. 189,00\s€/,
    );
  });

  it("creates the new client only once, even when the booking is tried again", async () => {
    let freeRooms = [
      available("101", "Double", 2, 89.9),
      available("102", "Double", 2, 89.9),
    ];
    const martinStay: Reservation = {
      ...DUPONT_STAY,
      id: "resa-martin",
      checkOutDate: "2026-10-08",
      nights: 1,
      totalPrice: 89.9,
      client: { id: JEAN_MARTIN.id, firstName: "Jean", lastName: "Martin" },
      room: { id: "room-102", roomNumber: "102", roomTypeName: "Double" },
    };
    const requests = mockApi({
      [ONE_NIGHT]: () => Response.json(freeRooms),
      "POST /clients": () => Response.json(JEAN_MARTIN, { status: 201 }),
      "POST /reservations": (request: ApiRequest) => {
        if ((request.body as ReservationRequest).roomId === "room-101") {
          // Une autre réception vient de prendre la 101
          freeRooms = freeRooms.filter((room) => room.roomNumber !== "101");
          return problem(
            409,
            "La chambre 101 est déjà réservée sur cette période",
          );
        }
        return Response.json(martinStay, { status: 201 });
      },
      ...fileOf(martinStay),
    });
    renderApp("/reservations/nouvelle");
    const user = userEvent.setup();

    await user.click(
      await screen.findByRole("radio", { name: /^Chambre 101/ }),
    );
    await user.click(screen.getByRole("button", { name: "Nouveau client" }));
    await user.type(screen.getByLabelText("Prénom"), "Jean");
    await user.type(screen.getByLabelText("Nom"), "Martin");
    await user.click(screen.getByRole("button", { name: "Réserver" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "La chambre 101 est déjà réservée sur cette période",
    );
    await waitFor(() =>
      expect(screen.queryByRole("radio", { name: /^Chambre 101/ })).toBeNull(),
    );
    expect(screen.getByText("Jean Martin")).toBeInTheDocument();
    await user.click(screen.getByRole("radio", { name: /^Chambre 102/ }));
    await user.click(screen.getByRole("button", { name: "Réserver" }));

    expect(
      await screen.findByRole("heading", { name: "Jean Martin" }),
    ).toBeInTheDocument();
    const posts = requests.filter((request) => request.method === "POST");
    expect(posts.map((request) => request.path)).toEqual([
      "/clients",
      "/reservations",
      "/reservations",
    ]);
    expect(posts[0].body).toEqual({
      firstName: "Jean",
      lastName: "Martin",
      email: null,
      phone: null,
    });
    expect(posts[2].body).toMatchObject({
      clientId: "client-martin",
      roomId: "room-102",
    });
  });

  it("waits for a departure after the arrival before looking for rooms", async () => {
    const requests = mockApi({
      [ONE_NIGHT]: [available("101", "Double", 2, 89.9)],
    });
    renderApp("/reservations/nouvelle");
    const user = userEvent.setup();
    await screen.findByRole("radio", { name: /^Chambre 101/ });

    const departure = screen.getByLabelText("Départ");
    await user.clear(departure);
    await user.type(departure, "2026-10-07");

    expect(departure).toHaveAccessibleDescription(
      "doit être postérieure à la date d'arrivée",
    );
    expect(screen.queryByRole("radio")).toBeNull();
    expect(screen.getByRole("button", { name: "Réserver" })).toBeDisabled();
    expect(requests.map((request) => request.path)).toEqual([
      "/rooms/availability?checkIn=2026-10-07&checkOut=2026-10-08",
    ]);
  });

  it("starts from the room and the day picked in the planning", async () => {
    mockApi({
      "GET /rooms/availability?checkIn=2026-10-09&checkOut=2026-10-10": [
        available("101", "Double", 2, 89.9),
        available("102", "Double", 2, 89.9),
      ],
    });

    renderApp("/reservations/nouvelle?chambre=room-102&arrivee=2026-10-09");

    expect(
      await screen.findByRole("radio", { name: /^Chambre 102/ }),
    ).toBeChecked();
    expect(screen.getByLabelText("Arrivée")).toHaveValue("2026-10-09");
    expect(screen.getByLabelText("Départ")).toHaveValue("2026-10-10");
  });

  it("starts from today when the day in the address has passed", async () => {
    mockApi({ [ONE_NIGHT]: [available("101", "Double", 2, 89.9)] });

    renderApp("/reservations/nouvelle?arrivee=2026-10-01");

    await screen.findByRole("radio", { name: /^Chambre 101/ });
    expect(screen.getByLabelText("Arrivée")).toHaveValue("2026-10-07");
  });
});
