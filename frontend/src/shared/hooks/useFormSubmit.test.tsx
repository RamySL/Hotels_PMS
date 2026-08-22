import { act, renderHook } from "@testing-library/react";
import type { FormEvent } from "react";
import { describe, expect, it, vi } from "vitest";
import { ApiError } from "@/shared/lib/apiClient";
import { useFormSubmit, type FormSubmit } from "./useFormSubmit";

function submitEvent() {
  return { preventDefault: vi.fn() };
}

async function submit(
  form: { current: FormSubmit },
  save: () => Promise<void>,
) {
  const event = submitEvent();
  await act(() =>
    form.current.handleSubmit(save)(
      event as unknown as FormEvent<HTMLFormElement>,
    ),
  );
  return event;
}

const invalidName = new ApiError(
  400,
  "Requête invalide",
  "Un ou plusieurs champs sont invalides",
  { name: "ne doit pas être vide" },
);

describe("useFormSubmit", () => {
  it("stays on the page and marks the form as saving until the save ends", async () => {
    let endSave: () => void = () => {};
    const onError = vi.fn();
    const { result } = renderHook(() => useFormSubmit({ onError }));
    const event = submitEvent();

    let submitting = Promise.resolve();
    act(() => {
      submitting = result.current.handleSubmit(
        () =>
          new Promise<void>((resolve) => {
            endSave = resolve;
          }),
      )(event as unknown as FormEvent<HTMLFormElement>);
    });

    expect(event.preventDefault).toHaveBeenCalled();
    expect(result.current.saving).toBe(true);
    await act(async () => {
      endSave();
      await submitting;
    });
    expect(result.current.saving).toBe(false);
    expect(result.current.error).toBeNull();
    expect(onError).not.toHaveBeenCalled();
  });

  it("shows the message and the field errors of the API, then calls onError", async () => {
    const onError = vi.fn();
    const { result } = renderHook(() => useFormSubmit({ onError }));

    await submit(result, () => Promise.reject(invalidName));

    expect(result.current.error).toBe("Un ou plusieurs champs sont invalides");
    expect(result.current.fieldErrors).toEqual({
      name: "ne doit pas être vide",
    });
    expect(result.current.saving).toBe(false);
    expect(onError).toHaveBeenCalledOnce();
  });

  it("falls back to a general message for an error that does not come from the API", async () => {
    const failure = () => Promise.reject(new SyntaxError("Unexpected token"));
    const byDefault = renderHook(() => useFormSubmit()).result;
    const custom = renderHook(() =>
      useFormSubmit({ unexpectedError: "Une erreur inattendue est survenue." }),
    ).result;

    await submit(byDefault, failure);
    await submit(custom, failure);

    expect(byDefault.current.error).toBe("L'enregistrement a échoué.");
    expect(byDefault.current.fieldErrors).toEqual({});
    expect(custom.current.error).toBe("Une erreur inattendue est survenue.");
  });

  it("clears the errors of the previous attempt", async () => {
    const { result } = renderHook(() => useFormSubmit());
    await submit(result, () => Promise.reject(invalidName));

    await submit(result, () => Promise.resolve());

    expect(result.current.error).toBeNull();
    expect(result.current.fieldErrors).toEqual({});
  });
});
