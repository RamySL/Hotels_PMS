import { describe, expect, it } from "vitest";
import { pageFromAddress } from "./page";

describe("pageFromAddress", () => {
  it("reads the page asked for in the address, counted from 1", () => {
    expect(pageFromAddress("2")).toBe(2);
  });

  it("falls back to the first page when the address holds no real page", () => {
    expect(pageFromAddress(null)).toBe(1);
    expect(pageFromAddress("0")).toBe(1);
    expect(pageFromAddress("-3")).toBe(1);
    expect(pageFromAddress("1.5")).toBe(1);
    expect(pageFromAddress("deux")).toBe(1);
  });
});
