import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { mockApi, problem } from "@/test/mockApi";
import { pageOf } from "@/test/page";
import { renderApp, resetApp } from "@/test/renderApp";
import { DUPONT_STAY, invoiceFor } from "@/test/reservations";
import type { Reservation, ReservationStatus } from "./types";

const FILE = "/reservations/resa-dupont";

/**
 * Fausse API pour la fiche de resa-dupont : chaque action change le statut comme le
 * ferait le backend, sans en vérifier les règles. replies remplace des réponses.
 */
function serveStay(
  initial: Reservation,
  amountPaid = 0,
  replies: Record<string, unknown> = {},
) {
  let stay = initial;
  const moveTo = (status: ReservationStatus) => () => {
    stay = { ...stay, status };
    return Response.json(stay);
  };
  return mockApi({
    "GET /reservations/resa-dupont": () => Response.json(stay),
    "GET /invoices?reservationId=resa-dupont": () =>
      Response.json(pageOf([invoiceFor(stay, amountPaid)])),
    "PATCH /reservations/resa-dupont/check-in": moveTo("CHECKED_IN"),
    "PATCH /reservations/resa-dupont/check-out": moveTo("CHECKED_OUT"),
    "PATCH /reservations/resa-dupont/cancel": moveTo("CANCELLED"),
    ...replies,
  });
}

/** Valeur affichée sous ce libellé de la fiche. */
function detail(label: string) {
  return screen.getByText(label, { selector: "dt" }).nextElementSibling;
}

beforeEach(() => {
  // Mercredi 7 octobre 2026, jour d'arrivée d'Hélène Dupont
  vi.setSystemTime(new Date(2026, 9, 7, 10, 0));
});

afterEach(() => {
  resetApp();
  vi.useRealTimers();
});

describe("ReservationPage", () => {
  it("shows the stay, what remains to be paid and who booked it", async () => {
    serveStay(DUPONT_STAY, 100);

    renderApp(FILE);

    expect(
      await screen.findByRole("heading", { name: "Hélène Dupont" }),
    ).toBeInTheDocument();
    expect(screen.getByText("Confirmée")).toBeInTheDocument();
    expect(screen.getByText("Chambre 101 · Double")).toBeInTheDocument();
    expect(detail("Arrivée")).toHaveTextContent("mer. 7 oct. 2026");
    expect(detail("Départ")).toHaveTextContent("ven. 9 oct. 2026");
    expect(detail("Durée")).toHaveTextContent("2 nuits");
    expect(detail("Personnes")).toHaveTextContent("2");
    expect(detail("Prix du séjour")).toHaveTextContent(/^179,80\s€$/);
    expect(detail("Reste à payer")).toHaveTextContent(/^79,80\s€$/);
    expect(
      screen.getByText("Réservée le lun. 5 oct. 2026 par Lucas Bernard"),
    ).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Modifier" })).toHaveAttribute(
      "href",
      "/reservations/resa-dupont/modifier",
    );
  });

  it("checks in on the arrival day, then shows the stay in progress", async () => {
    const requests = serveStay(DUPONT_STAY);
    renderApp(FILE);

    await userEvent
      .setup()
      .click(await screen.findByRole("button", { name: "Check-in" }));

    expect(await screen.findByText("En séjour")).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Check-out" }),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "Annuler la réservation" }),
    ).toBeNull();
    expect(requests).toContainEqual({
      method: "PATCH",
      path: "/reservations/resa-dupont/check-in",
      body: undefined,
    });
  });

  it("shows why the check-in was refused", async () => {
    serveStay(DUPONT_STAY, 0, {
      "PATCH /reservations/resa-dupont/check-in": () =>
        problem(409, "La chambre 101 n'est pas prête (CLEANING)"),
    });
    renderApp(FILE);

    await userEvent
      .setup()
      .click(await screen.findByRole("button", { name: "Check-in" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "La chambre 101 n'est pas prête (CLEANING)",
    );
    expect(screen.getByText("Confirmée")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Check-in" })).toBeEnabled();
  });

  it("waits for the arrival day to check in", async () => {
    serveStay({
      ...DUPONT_STAY,
      checkInDate: "2026-10-08",
      checkOutDate: "2026-10-10",
    });

    renderApp(FILE);

    const checkInButton = await screen.findByRole("button", {
      name: "Check-in",
    });
    expect(checkInButton).toBeDisabled();
    expect(checkInButton).toHaveAccessibleDescription(
      "Le check-in sera possible à partir du jeu. 8 oct. 2026.",
    );
    expect(
      screen.getByRole("button", { name: "Annuler la réservation" }),
    ).toBeEnabled();
  });

  it("no longer checks in on the departure day", async () => {
    serveStay({
      ...DUPONT_STAY,
      checkInDate: "2026-10-05",
      checkOutDate: "2026-10-07",
    });

    renderApp(FILE);

    const checkInButton = await screen.findByRole("button", {
      name: "Check-in",
    });
    expect(checkInButton).toBeDisabled();
    expect(checkInButton).toHaveAccessibleDescription(
      "La date de départ est passée : le check-in n'est plus possible.",
    );
  });

  it("keeps the check-out disabled while something remains to be paid", async () => {
    serveStay({ ...DUPONT_STAY, status: "CHECKED_IN" }, 100);

    renderApp(FILE);

    const checkOutButton = await screen.findByRole("button", {
      name: "Check-out",
    });
    expect(checkOutButton).toBeDisabled();
    expect(checkOutButton).toHaveAccessibleDescription(
      /^Il reste 79,80\s€ à payer : le check-out sera possible une fois la facture soldée\.$/,
    );
  });

  it("checks out once the invoice is settled, then offers no more action", async () => {
    const requests = serveStay({ ...DUPONT_STAY, status: "CHECKED_IN" }, 179.8);
    renderApp(FILE);

    await userEvent
      .setup()
      .click(await screen.findByRole("button", { name: "Check-out" }));

    expect(await screen.findByText("Terminée")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Check-out" })).toBeNull();
    expect(screen.queryByRole("link", { name: "Modifier" })).toBeNull();
    expect(requests).toContainEqual({
      method: "PATCH",
      path: "/reservations/resa-dupont/check-out",
      body: undefined,
    });
  });

  it("cancels only once the cancellation is confirmed", async () => {
    const requests = serveStay(DUPONT_STAY);
    renderApp(FILE);
    const user = userEvent.setup();

    await user.click(
      await screen.findByRole("button", { name: "Annuler la réservation" }),
    );
    expect(
      screen.getByText(/La chambre 101 sera libérée pour ces dates/),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Garder la réservation" }),
    ).toHaveFocus();
    await user.click(
      screen.getByRole("button", { name: "Garder la réservation" }),
    );
    await user.click(
      screen.getByRole("button", { name: "Annuler la réservation" }),
    );
    await user.click(
      screen.getByRole("button", { name: "Confirmer l'annulation" }),
    );

    expect(await screen.findByText("Annulée")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Check-in" })).toBeNull();
    expect(requests.filter((request) => request.method === "PATCH")).toEqual([
      {
        method: "PATCH",
        path: "/reservations/resa-dupont/cancel",
        body: undefined,
      },
    ]);
  });

  it("says when the reservation does not exist", async () => {
    mockApi({
      "GET /reservations/inconnue": () =>
        problem(404, "Réservation introuvable : inconnue"),
      "GET /invoices?reservationId=inconnue": pageOf([]),
    });

    renderApp("/reservations/inconnue");

    expect(
      await screen.findByText("Cette réservation n'existe pas."),
    ).toBeInTheDocument();
  });
});
