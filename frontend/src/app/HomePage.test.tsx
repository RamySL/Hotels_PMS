import { render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { HomePage } from "./HomePage";

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("HomePage", () => {
  it("shows that the API is available when it answers", async () => {
    vi.stubGlobal(
      "fetch",
      vi
        .fn<typeof fetch>()
        .mockResolvedValue(Response.json({ status: "UP" }, { status: 200 })),
    );

    render(<HomePage />);

    expect(await screen.findByText("API disponible")).toBeInTheDocument();
  });

  it("shows that the API is unreachable when the server does not answer", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn<typeof fetch>().mockRejectedValue(new TypeError("Failed to fetch")),
    );

    render(<HomePage />);

    expect(await screen.findByText("API injoignable")).toBeInTheDocument();
  });
});
