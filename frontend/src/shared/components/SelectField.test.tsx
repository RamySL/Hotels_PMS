import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { SelectField } from "./SelectField";

const OPTIONS = [
  { value: "t1", label: "Double" },
  { value: "t2", label: "Suite" },
];

describe("SelectField", () => {
  it("links the label to its list of options", () => {
    render(<SelectField label="Type" options={OPTIONS} defaultValue="t2" />);

    expect(screen.getByLabelText("Type")).toHaveValue("t2");
    expect(
      screen.getAllByRole("option").map((option) => option.textContent),
    ).toEqual(["Double", "Suite"]);
  });

  it("describes the list with its error", () => {
    render(
      <SelectField label="Type" options={OPTIONS} error="Choisissez un type" />,
    );

    expect(screen.getByLabelText("Type")).toHaveAccessibleDescription(
      "Choisissez un type",
    );
  });
});
