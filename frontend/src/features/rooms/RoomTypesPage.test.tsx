import { screen, within } from "@testing-library/react";
import { afterEach, describe, expect, it } from "vitest";
import { mockApi } from "@/test/mockApi";
import { renderApp, resetApp } from "@/test/renderApp";
import { DOUBLE, SUITE } from "@/test/rooms";

function rowOf(typeName: string) {
  return within(screen.getByRole("cell", { name: typeName }).closest("tr")!);
}

afterEach(resetApp);

describe("RoomTypesPage", () => {
  it("lists the room types with their price and capacity", async () => {
    mockApi({ "GET /room-types": [DOUBLE, SUITE] });

    renderApp("/chambres/types");

    expect(
      await screen.findByRole("cell", { name: "Double" }),
    ).toBeInTheDocument();
    expect(rowOf("Double").getByText(/^89,90\s€$/)).toBeInTheDocument();
    expect(rowOf("Double").getByText("2 pers.")).toBeInTheDocument();
    expect(rowOf("Suite").getByText("Vue sur la mer")).toBeInTheDocument();
  });

  it("lets the administration create and edit types", async () => {
    mockApi({ "GET /room-types": [DOUBLE] });

    renderApp("/chambres/types", "ADMIN");

    expect(
      await screen.findByRole("link", { name: "Modifier le type Double" }),
    ).toHaveAttribute("href", "/chambres/types/type-double/modifier");
    expect(screen.getByRole("link", { name: "Nouveau type" })).toHaveAttribute(
      "href",
      "/chambres/types/nouveau",
    );
  });

  it("only shows the prices to the reception", async () => {
    mockApi({ "GET /room-types": [DOUBLE] });

    renderApp("/chambres/types", "RECEPTIONIST");

    await screen.findByRole("cell", { name: "Double" });
    expect(screen.queryByRole("link", { name: "Nouveau type" })).toBeNull();
    expect(
      screen.queryByRole("link", { name: "Modifier le type Double" }),
    ).toBeNull();
  });
});
