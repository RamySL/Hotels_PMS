import "@testing-library/jest-dom/vitest";
import { cleanup } from "@testing-library/react";
import { afterEach } from "vitest";

// Sans les globals de Vitest, Testing Library ne démonte pas le rendu tout seul
afterEach(() => {
  cleanup();
});
