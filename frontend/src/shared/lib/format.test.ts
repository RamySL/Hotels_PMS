import { describe, expect, it } from "vitest";
import { formatEuros } from "./format";

describe("formatEuros", () => {
  it("writes an amount the French way", () => {
    expect(formatEuros(89.9)).toMatch(/^89,90\s€$/);
    expect(formatEuros(1234.5)).toMatch(/^1\s234,50\s€$/);
  });
});
