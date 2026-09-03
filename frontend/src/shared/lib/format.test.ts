import { describe, expect, it } from "vitest";
import { formatEuros, formatPercent } from "./format";

describe("formatEuros", () => {
  it("writes an amount the French way", () => {
    expect(formatEuros(89.9)).toMatch(/^89,90\s€$/);
    expect(formatEuros(1234.5)).toMatch(/^1\s234,50\s€$/);
  });
});

describe("formatPercent", () => {
  it("writes a rate out of 100 the French way, to one decimal at most", () => {
    expect(formatPercent(66.7)).toMatch(/^66,7\s%$/);
    expect(formatPercent(50)).toMatch(/^50\s%$/);
    expect(formatPercent(0)).toMatch(/^0\s%$/);
  });
});
