import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { mockApi } from "@/test/mockApi";
import { pageOf } from "@/test/page";
import { renderApp, resetApp } from "@/test/renderApp";
import { DUPONT_STAY, invoiceFor } from "@/test/reservations";
import { room, SUITE } from "@/test/rooms";
import type { Reservation } from "./types";

const ROOMS = [room("101"), room("102"), room("201", "OUT_OF_SERVICE", SUITE)];

/** En séjour dans la 102 depuis le lundi 5 octobre, départ le jeudi 8. */
const MARTIN_STAY: Reservation = {
  ...DUPONT_STAY,
  id: "resa-martin",
  status: "CHECKED_IN",
  checkInDate: "2026-10-05",
  checkOutDate: "2026-10-08",
  nights: 3,
  client: { id: "client-martin", firstName: "Jean", lastName: "Martin" },
  room: { id: "room-102", roomNumber: "102", roomTypeName: "Double" },
};

const CANCELLED_STAY: Reservation = {
  ...DUPONT_STAY,
  id: "resa-durand",
  status: "CANCELLED",
  checkInDate: "2026-10-12",
  checkOutDate: "2026-10-14",
  client: { id: "client-durand", firstName: "Paul", lastName: "Durand" },
};

const FROM_TODAY = "GET /reservations?from=2026-10-07&to=2026-10-21&size=100";
const NEXT_WEEK = "GET /reservations?from=2026-10-14&to=2026-10-28&size=100";

beforeEach(() => {
  // Mercredi 7 octobre 2026
  vi.setSystemTime(new Date(2026, 9, 7, 10, 0));
});

afterEach(() => {
  resetApp();
  vi.useRealTimers();
});

describe("PlanningPage", () => {
  it("shows the rooms and their stays over two weeks from today", async () => {
    mockApi({
      "GET /rooms": ROOMS,
      [FROM_TODAY]: pageOf([MARTIN_STAY, DUPONT_STAY, CANCELLED_STAY]),
    });

    renderApp("/reservations");

    const dupont = await screen.findByRole("link", {
      name: "Hélène Dupont, confirmée, du mer. 7 oct. au ven. 9 oct.",
    });
    expect(dupont).toHaveAttribute("href", "/reservations/resa-dupont");
    expect(dupont.closest("td")).toHaveAttribute("colspan", "2");
    expect(
      screen
        .getByRole("link", {
          name: "Jean Martin, en séjour, du lun. 5 oct. au jeu. 8 oct.",
        })
        .closest("td"),
    ).toHaveAttribute("colspan", "1");
    expect(screen.queryByRole("link", { name: /Paul Durand/ })).toBeNull();

    expect(
      screen.getByText("Du mer. 7 oct. au mar. 20 oct. 2026"),
    ).toBeInTheDocument();
    const days = screen.getAllByRole("columnheader").slice(1);
    expect(days).toHaveLength(14);
    expect(days[0]).toHaveTextContent("mer. 7");
    expect(days[0]).toHaveAttribute("aria-current", "date");
    expect(days[13]).toHaveTextContent("mar. 20");
    expect(
      screen.getAllByRole("rowheader").map((header) => header.textContent),
    ).toEqual(["101 Double", "102 Double", "201 Suite Hors service"]);
    expect(screen.getByRole("link", { name: "Réservations" })).toHaveAttribute(
      "aria-current",
      "page",
    );
  });

  it("moves from week to week, keeping the first day in the address", async () => {
    const requests = mockApi({
      "GET /rooms": ROOMS,
      [FROM_TODAY]: pageOf([DUPONT_STAY]),
      [NEXT_WEEK]: pageOf([]),
    });
    const router = renderApp("/reservations?du=2026-10-14");
    const user = userEvent.setup();
    await screen.findByRole("table");

    await user.click(screen.getByRole("link", { name: "Semaine précédente" }));
    await screen.findByRole("link", { name: /^Hélène Dupont/ });
    expect(router.state.location.search).toBe("?du=2026-10-07");

    await user.click(screen.getByRole("link", { name: "Semaine suivante" }));
    await waitFor(() =>
      expect(screen.queryByRole("link", { name: /^Hélène Dupont/ })).toBeNull(),
    );
    expect(router.state.location.search).toBe("?du=2026-10-14");

    await user.click(screen.getByRole("link", { name: "Aujourd'hui" }));
    await screen.findByRole("link", { name: /^Hélène Dupont/ });
    expect(router.state.location.search).toBe("");

    expect(
      requests
        .filter((request) => request.path.startsWith("/reservations"))
        .map((request) => `GET ${request.path}`),
    ).toEqual([NEXT_WEEK, FROM_TODAY, NEXT_WEEK, FROM_TODAY]);
  });

  it("opens a reservation from its bar", async () => {
    mockApi({
      "GET /rooms": ROOMS,
      [FROM_TODAY]: pageOf([DUPONT_STAY]),
      "GET /reservations/resa-dupont": DUPONT_STAY,
      "GET /invoices?reservationId=resa-dupont": pageOf([
        invoiceFor(DUPONT_STAY),
      ]),
    });
    renderApp("/reservations");

    await userEvent
      .setup()
      .click(await screen.findByRole("link", { name: /^Hélène Dupont/ }));

    expect(
      await screen.findByRole("heading", { name: "Hélène Dupont" }),
    ).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Réservations" })).toHaveAttribute(
      "aria-current",
      "page",
    );
  });

  it("starts from today when the address holds no real day", async () => {
    const requests = mockApi({
      "GET /rooms": ROOMS,
      [FROM_TODAY]: pageOf([]),
    });

    renderApp("/reservations?du=2026-02-30");

    expect(await screen.findByRole("table")).toBeInTheDocument();
    expect(
      screen.getByText("Du mer. 7 oct. au mar. 20 oct. 2026"),
    ).toBeInTheDocument();
    expect(requests.map((request) => `GET ${request.path}`)).toContain(
      FROM_TODAY,
    );
  });

  it("warns when the period holds more reservations than it can show", async () => {
    mockApi({
      "GET /rooms": ROOMS,
      [FROM_TODAY]: pageOf([DUPONT_STAY], {
        totalPages: 2,
        totalElements: 101,
      }),
    });

    renderApp("/reservations");

    expect(await screen.findByRole("status")).toHaveTextContent(
      "seules les 100 premières arrivées sont affichées",
    );
  });
});
