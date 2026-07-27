import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { Button } from "./Button";

describe("Button", () => {
  it("does not submit a form unless asked to", () => {
    render(<Button>Annuler</Button>);

    expect(screen.getByRole("button", { name: "Annuler" })).toHaveAttribute(
      "type",
      "button",
    );
  });

  it("can be a submit button and be disabled", () => {
    render(
      <Button type="submit" disabled>
        Enregistrer
      </Button>,
    );

    const button = screen.getByRole("button", { name: "Enregistrer" });
    expect(button).toHaveAttribute("type", "submit");
    expect(button).toBeDisabled();
  });
});
