import { afterEach, describe, expect, it, vi } from "vitest";
import { formatDate, parseDate, toIsoDate, today } from "./dates";

// Les tests tournent à l'heure de Paris (vite.config.ts)

afterEach(() => {
  vi.useRealTimers();
});

describe("dates", () => {
  it("reads a date of the API as local midnight", () => {
    const date = parseDate("2026-10-07");

    expect(date.getDate()).toBe(7);
    expect(date.getHours()).toBe(0);
  });

  it("writes the local day of a date, not its UTC day", () => {
    // 0 h 30 à Paris : il est encore 22 h 30 le 6 octobre en UTC
    expect(toIsoDate(new Date(2026, 9, 7, 0, 30))).toBe("2026-10-07");
    expect(toIsoDate(new Date(2026, 0, 5))).toBe("2026-01-05");
  });

  it("gives today's date from the local clock", () => {
    vi.setSystemTime(new Date(2026, 9, 7, 0, 30));

    expect(today()).toBe("2026-10-07");
  });

  it("shows a day the French way", () => {
    expect(formatDate("2026-10-07")).toBe("mer. 7 oct. 2026");
    expect(formatDate(new Date("2026-10-05T12:00:00Z"))).toBe(
      "lun. 5 oct. 2026",
    );
  });
});
