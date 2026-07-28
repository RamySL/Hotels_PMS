import { act, renderHook, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { mockApi, problem } from "@/test/mockApi";
import { useApiData } from "./useApiData";

type Item = { id: string };

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("useApiData", () => {
  it("loads the data of its path", async () => {
    mockApi({ "GET /rooms": [{ id: "r1" }] });

    const { result } = renderHook(() => useApiData<Item[]>("/rooms"));

    expect(result.current.loading).toBe(true);
    await waitFor(() => expect(result.current.data).toEqual([{ id: "r1" }]));
    expect(result.current.loading).toBe(false);
    expect(result.current.error).toBeNull();
  });

  it("reloads on demand, keeping the previous data meanwhile", async () => {
    let calls = 0;
    mockApi({ "GET /rooms": () => Response.json([{ id: `r${++calls}` }]) });
    const { result } = renderHook(() => useApiData<Item[]>("/rooms"));
    await waitFor(() => expect(result.current.data).toEqual([{ id: "r1" }]));

    act(() => result.current.reload());

    expect(result.current.loading).toBe(true);
    expect(result.current.data).toEqual([{ id: "r1" }]);
    await waitFor(() => expect(result.current.data).toEqual([{ id: "r2" }]));
  });

  it("ignores a late answer for a previous path", async () => {
    let answerFirst: (response: Response) => void = () => {};
    mockApi({
      "GET /rooms?status=READY": () =>
        new Promise<Response>((resolve) => {
          answerFirst = resolve;
        }),
      "GET /rooms?status=CLEANING": [{ id: "r2" }],
    });
    const { result, rerender } = renderHook(
      ({ path }) => useApiData<Item[]>(path),
      { initialProps: { path: "/rooms?status=READY" } },
    );

    rerender({ path: "/rooms?status=CLEANING" });
    await waitFor(() => expect(result.current.data).toEqual([{ id: "r2" }]));
    await act(async () => answerFirst(Response.json([{ id: "r1" }])));

    expect(result.current.data).toEqual([{ id: "r2" }]);
  });

  it("reports the error of the API", async () => {
    mockApi({ "GET /rooms": () => problem(403, "Accès refusé") });

    const { result } = renderHook(() => useApiData<Item[]>("/rooms"));

    await waitFor(() =>
      expect(result.current.error?.message).toBe("Accès refusé"),
    );
    expect(result.current.loading).toBe(false);
  });
});
