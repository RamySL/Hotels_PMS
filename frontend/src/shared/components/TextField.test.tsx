import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { TextField } from "./TextField";

describe("TextField", () => {
  it("links the label to its input", () => {
    render(<TextField label="Nom" defaultValue="Dupont" />);

    expect(screen.getByLabelText("Nom")).toHaveValue("Dupont");
  });

  it("marks the input invalid and describes it with its error", () => {
    render(<TextField label="Email" error="Adresse invalide" />);

    const input = screen.getByLabelText("Email");
    expect(input).toHaveAttribute("aria-invalid", "true");
    expect(input).toHaveAccessibleDescription("Adresse invalide");
  });

  it("leaves a valid input unmarked", () => {
    render(<TextField label="Email" />);

    const input = screen.getByLabelText("Email");
    expect(input).not.toHaveAttribute("aria-invalid");
    expect(input).not.toHaveAttribute("aria-describedby");
  });
});
