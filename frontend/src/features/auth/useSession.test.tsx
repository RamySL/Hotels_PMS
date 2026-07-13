import { act, renderHook } from "@testing-library/react";
import { afterEach, describe, expect, it } from "vitest";
import { endSession, startSession } from "./session";
import { useSession } from "./useSession";

afterEach(() => {
  endSession("logout");
  localStorage.clear();
});

describe("useSession", () => {
  it("follows the session as it starts and ends", () => {
    const { result } = renderHook(() => useSession());
    expect(result.current).toBeNull();

    act(() =>
      startSession({
        token: "jeton",
        user: {
          id: "df376895-62fc-4376-a4bb-4de71365aa54",
          email: "reception@pms.local",
          firstName: "Lucas",
          lastName: "Bernard",
          role: "RECEPTIONIST",
        },
        expiresAt: Date.now() + 60_000,
      }),
    );
    expect(result.current?.user.firstName).toBe("Lucas");

    act(() => endSession("logout"));
    expect(result.current).toBeNull();
  });
});
