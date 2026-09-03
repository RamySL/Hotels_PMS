import { afterEach, describe, expect, it, vi } from "vitest";
import {
  addDays,
  daysBetween,
  formatDate,
  formatDateTime,
  formatLongDate,
  formatShortDate,
  formatWeekday,
  isIsoDate,
  parseDate,
  toIsoDate,
  today,
} from "./dates";

// Les tests tournent à l'heure de Paris (vite.config.ts). En 2026, on y passe à l'heure
// d'été le 29 mars et à l'heure d'hiver le 25 octobre.

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

  it("only accepts days that exist, in the format of the API", () => {
    expect(isIsoDate("2026-10-07")).toBe(true);
    expect(isIsoDate("2026-02-30")).toBe(false);
    expect(isIsoDate("07/10/2026")).toBe(false);
    expect(isIsoDate(null)).toBe(false);
  });

  it("moves by whole days, across months and changes of time", () => {
    expect(addDays("2026-10-31", 1)).toBe("2026-11-01");
    expect(addDays("2026-10-07", -7)).toBe("2026-09-30");
    expect(addDays("2026-10-24", 2)).toBe("2026-10-26");
    expect(addDays("2026-03-28", 2)).toBe("2026-03-30");
  });

  it("counts the days between two dates, across changes of time", () => {
    expect(daysBetween("2026-10-07", "2026-10-09")).toBe(2);
    expect(daysBetween("2026-10-24", "2026-10-26")).toBe(2);
    expect(daysBetween("2026-03-28", "2026-03-30")).toBe(2);
  });

  it("shows a day the French way", () => {
    expect(formatDate("2026-10-07")).toBe("mer. 7 oct. 2026");
    expect(formatDate(new Date("2026-10-05T12:00:00Z"))).toBe(
      "lun. 5 oct. 2026",
    );
    expect(formatLongDate("2026-10-07")).toBe("mercredi 7 octobre 2026");
    expect(formatShortDate("2026-10-07")).toBe("mer. 7 oct.");
    expect(formatWeekday("2026-10-07")).toBe("mer.");
    // 8 h 32 UTC, 10 h 32 à Paris en heure d'été
    expect(formatDateTime("2026-10-07T08:32:00Z")).toBe(
      "mer. 7 oct. 2026, 10:32",
    );
  });
});
