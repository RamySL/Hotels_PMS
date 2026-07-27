import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { Alert } from "./Alert";

describe("Alert", () => {
  it("announces an error at once", () => {
    render(<Alert tone="error">La chambre 101 existe déjà</Alert>);

    expect(screen.getByRole("alert")).toHaveTextContent(
      "La chambre 101 existe déjà",
    );
  });

  it("announces a notice politely", () => {
    render(<Alert tone="notice">Votre session a expiré</Alert>);

    expect(screen.getByRole("status")).toHaveTextContent(
      "Votre session a expiré",
    );
  });
});
