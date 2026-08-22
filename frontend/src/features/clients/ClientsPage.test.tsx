import { act, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it } from "vitest";
import { HELENE_DUPONT, JEAN_MARTIN } from "@/test/clients";
import { mockApi } from "@/test/mockApi";
import { pageOf } from "@/test/page";
import { renderApp, resetApp } from "@/test/renderApp";

function rowOf(lastName: string) {
  return within(screen.getByRole("cell", { name: lastName }).closest("tr")!);
}

afterEach(resetApp);

describe("ClientsPage", () => {
  it("lists the clients of the first page", async () => {
    mockApi({
      "GET /clients?page=0&size=20": pageOf([HELENE_DUPONT, JEAN_MARTIN]),
    });

    renderApp("/clients");

    expect(
      await screen.findByRole("cell", { name: "Dupont" }),
    ).toBeInTheDocument();
    expect(rowOf("Dupont").getByText("06 12 34 56 78")).toBeInTheDocument();
    expect(rowOf("Martin").getAllByText("—")).toHaveLength(2);
    expect(screen.getByText("2 clients")).toBeInTheDocument();
    expect(screen.getByText("Page 1 sur 1")).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Page précédente" }),
    ).toBeDisabled();
    expect(
      screen.getByRole("button", { name: "Page suivante" }),
    ).toBeDisabled();
    expect(screen.getByRole("link", { name: "Clients" })).toHaveAttribute(
      "aria-current",
      "page",
    );
  });

  it("searches once the typing pauses, starting again from the first page", async () => {
    const requests = mockApi({
      "GET /clients?page=1&size=20": pageOf([JEAN_MARTIN], {
        number: 1,
        totalPages: 2,
        totalElements: 21,
      }),
      "GET /clients?search=helene&page=0&size=20": pageOf([HELENE_DUPONT]),
    });
    const router = renderApp("/clients?page=2");
    await screen.findByRole("cell", { name: "Martin" });

    await userEvent.setup().type(screen.getByLabelText("Rechercher"), "helene");

    expect(
      await screen.findByRole("cell", { name: "Dupont" }),
    ).toBeInTheDocument();
    expect(
      requests
        .map((request) => request.path)
        .filter((path) => path.includes("search=")),
    ).toEqual(["/clients?search=helene&page=0&size=20"]);
    expect(router.state.location.search).toBe("?recherche=helene");
  });

  it("moves between pages", async () => {
    const requests = mockApi({
      "GET /clients?page=0&size=20": pageOf([HELENE_DUPONT], {
        totalPages: 2,
        totalElements: 21,
      }),
      "GET /clients?page=1&size=20": pageOf([JEAN_MARTIN], {
        number: 1,
        totalPages: 2,
        totalElements: 21,
      }),
    });
    const router = renderApp("/clients");
    await screen.findByText("Page 1 sur 2");

    await userEvent
      .setup()
      .click(screen.getByRole("button", { name: "Page suivante" }));

    expect(
      await screen.findByRole("cell", { name: "Martin" }),
    ).toBeInTheDocument();
    expect(screen.getByText("Page 2 sur 2")).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Page suivante" }),
    ).toBeDisabled();
    expect(router.state.location.search).toBe("?page=2");
    expect(requests.at(-1)?.path).toBe("/clients?page=1&size=20");
  });

  it("restores the search and the page from the address", async () => {
    const requests = mockApi({
      "GET /clients?search=dupont&page=1&size=20": pageOf([HELENE_DUPONT], {
        number: 1,
        totalPages: 2,
        totalElements: 21,
      }),
    });

    renderApp("/clients?recherche=dupont&page=2");

    expect(
      await screen.findByRole("cell", { name: "Dupont" }),
    ).toBeInTheDocument();
    expect(screen.getByLabelText("Rechercher")).toHaveValue("dupont");
    expect(requests.map((request) => request.path)).toEqual([
      "/clients?search=dupont&page=1&size=20",
    ]);
  });

  it("leads to the creation and the edition of clients", async () => {
    mockApi({ "GET /clients?page=0&size=20": pageOf([HELENE_DUPONT]) });

    renderApp("/clients");

    expect(
      await screen.findByRole("link", { name: "Modifier Hélène Dupont" }),
    ).toHaveAttribute("href", "/clients/client-dupont/modifier");
    expect(
      screen.getByRole("link", { name: "Nouveau client" }),
    ).toHaveAttribute("href", "/clients/nouveau");
  });

  it("says when no client matches the search", async () => {
    mockApi({ "GET /clients?search=zzz&page=0&size=20": pageOf([]) });

    renderApp("/clients?recherche=zzz");

    expect(
      await screen.findByText("Aucun client ne correspond à « zzz »."),
    ).toBeInTheDocument();
  });

  it("follows the address when it changes without the search field", async () => {
    mockApi({
      "GET /clients?page=0&size=20": pageOf([HELENE_DUPONT, JEAN_MARTIN]),
      "GET /clients?search=martin&page=0&size=20": pageOf([JEAN_MARTIN]),
    });
    const router = renderApp("/clients");
    await screen.findByRole("cell", { name: "Dupont" });

    await act(() => router.navigate("/clients?recherche=martin"));

    expect(screen.getByLabelText("Rechercher")).toHaveValue("martin");
    await waitFor(() =>
      expect(screen.queryByRole("cell", { name: "Dupont" })).toBeNull(),
    );
  });
});
