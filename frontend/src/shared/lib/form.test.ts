import { describe, expect, it } from "vitest";
import { toNumberOrNull, toTextOrNull } from "./form";

describe("toNumberOrNull", () => {
  it("reads the number typed in a field", () => {
    expect(toNumberOrNull("12")).toBe(12);
    expect(toNumberOrNull("89.90")).toBe(89.9);
  });

  it("gives null for an empty field", () => {
    expect(toNumberOrNull("")).toBeNull();
  });
});

describe("toTextOrNull", () => {
  it("trims the text typed in a field", () => {
    expect(toTextOrNull("  Vue sur la mer ")).toBe("Vue sur la mer");
  });

  it("gives null for a blank field", () => {
    expect(toTextOrNull("   ")).toBeNull();
  });
});
