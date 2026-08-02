import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import { describe, expect, it } from "vitest";
import { ButtonLink } from "./ButtonLink";

describe("ButtonLink", () => {
  it("navigates like a link while looking like a button", () => {
    render(
      <MemoryRouter>
        <ButtonLink to="/chambres/nouvelle">Nouvelle chambre</ButtonLink>
      </MemoryRouter>,
    );

    expect(
      screen.getByRole("link", { name: "Nouvelle chambre" }),
    ).toHaveAttribute("href", "/chambres/nouvelle");
  });
});
