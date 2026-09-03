import { describe, expect, it } from "vitest";
import { formatBalance } from "./balance";

describe("formatBalance", () => {
  it("shows what remains to be paid, or what must be refunded", () => {
    expect(formatBalance(79.8)).toMatch(/^79,80\s€$/);
    expect(formatBalance(0)).toMatch(/^0,00\s€$/);
    expect(formatBalance(-50)).toMatch(/^Trop-perçu : 50,00\s€$/);
  });
});
