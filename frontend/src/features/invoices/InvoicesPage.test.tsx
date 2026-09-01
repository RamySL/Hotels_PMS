import { screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it } from "vitest";
import type { Reservation } from "@/features/reservations/types";
import { mockApi } from "@/test/mockApi";
import { pageOf } from "@/test/page";
import { renderApp, resetApp } from "@/test/renderApp";
import { DUPONT_STAY, invoiceFor, payment } from "@/test/reservations";

/** Annulée après un acompte de 50 € : la facture garde un trop-perçu. */
const MARTIN_CANCELLED: Reservation = {
  ...DUPONT_STAY,
  id: "resa-martin",
  status: "CANCELLED",
  client: { id: "client-martin", firstName: "Jean", lastName: "Martin" },
  room: { id: "room-102", roomNumber: "102", roomTypeName: "Double" },
};

const DURAND_STAY: Reservation = {
  ...DUPONT_STAY,
  id: "resa-durand",
  client: { id: "client-durand", firstName: "Paul", lastName: "Durand" },
};

const DUPONT_INVOICE = invoiceFor(DUPONT_STAY, [payment(100)]);
const MARTIN_INVOICE = invoiceFor(MARTIN_CANCELLED, [payment(50)]);
const DURAND_INVOICE = invoiceFor(DURAND_STAY);

function rowOf(clientName: string) {
  return within(screen.getByRole("link", { name: clientName }).closest("tr")!);
}

afterEach(resetApp);

describe("InvoicesPage", () => {
  it("lists the invoices with their stay and what remains to be paid", async () => {
    mockApi({
      "GET /invoices?page=0&size=20": pageOf([DUPONT_INVOICE, MARTIN_INVOICE]),
    });

    renderApp("/factures");

    expect(
      await screen.findByRole("link", { name: "Hélène Dupont" }),
    ).toHaveAttribute("href", "/reservations/resa-dupont");
    const dupont = rowOf("Hélène Dupont");
    expect(dupont.getByText("lun. 5 oct. 2026")).toBeInTheDocument();
    expect(dupont.getByText("101")).toBeInTheDocument();
    expect(dupont.getByText("mer. 7 oct. → ven. 9 oct.")).toBeInTheDocument();
    expect(dupont.getByText(/^179,80\s€$/)).toBeInTheDocument();
    expect(dupont.getByText(/^79,80\s€$/)).toBeInTheDocument();
    expect(dupont.getByText("Payée en partie")).toBeInTheDocument();
    const martin = rowOf("Jean Martin");
    expect(martin.getByText(/^Trop-perçu : 50,00\s€$/)).toBeInTheDocument();
    expect(martin.getByText("Annulée")).toBeInTheDocument();
    expect(screen.getByText("2 factures")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Factures" })).toHaveAttribute(
      "aria-current",
      "page",
    );
  });

  it("filters by status, starting again from the first page", async () => {
    const requests = mockApi({
      "GET /invoices?page=1&size=20": pageOf([DUPONT_INVOICE], {
        number: 1,
        totalPages: 2,
        totalElements: 21,
      }),
      "GET /invoices?status=UNPAID&page=0&size=20": pageOf([]),
    });
    const router = renderApp("/factures?page=2");
    await screen.findByRole("link", { name: "Hélène Dupont" });

    await userEvent
      .setup()
      .selectOptions(screen.getByLabelText("Statut"), "Non payée");

    expect(
      await screen.findByText("Aucune facture dans ce statut."),
    ).toBeInTheDocument();
    expect(router.state.location.search).toBe("?statut=UNPAID");
    expect(requests.at(-1)?.path).toBe(
      "/invoices?status=UNPAID&page=0&size=20",
    );
  });

  it("moves between pages, keeping the status", async () => {
    const requests = mockApi({
      "GET /invoices?status=UNPAID&page=0&size=20": pageOf([DURAND_INVOICE], {
        totalPages: 2,
        totalElements: 21,
      }),
      "GET /invoices?status=UNPAID&page=1&size=20": pageOf([DURAND_INVOICE], {
        number: 1,
        totalPages: 2,
        totalElements: 21,
      }),
    });
    const router = renderApp("/factures?statut=UNPAID");
    await screen.findByText("Page 1 sur 2");

    await userEvent
      .setup()
      .click(screen.getByRole("button", { name: "Page suivante" }));

    expect(await screen.findByText("Page 2 sur 2")).toBeInTheDocument();
    expect(router.state.location.search).toBe("?statut=UNPAID&page=2");
    expect(requests.at(-1)?.path).toBe(
      "/invoices?status=UNPAID&page=1&size=20",
    );
  });

  it("ignores a status that the API does not know", async () => {
    const requests = mockApi({
      "GET /invoices?page=0&size=20": pageOf([DUPONT_INVOICE]),
    });

    renderApp("/factures?statut=PERDUE");

    await screen.findByRole("link", { name: "Hélène Dupont" });
    expect(screen.getByLabelText("Statut")).toHaveValue("");
    expect(requests.map((request) => request.path)).toEqual([
      "/invoices?page=0&size=20",
    ]);
  });
});
