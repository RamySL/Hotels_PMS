/// <reference types="vitest/config" />
import tailwindcss from "@tailwindcss/vite";
import react from "@vitejs/plugin-react";
import { fileURLToPath } from "node:url";
import { defineConfig } from "vite";

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    // @/… désigne src/… (déclaré aussi dans tsconfig.app.json pour TypeScript)
    alias: { "@": fileURLToPath(new URL("./src", import.meta.url)) },
  },
  server: {
    // /api est relayé au backend : le front et l'API partagent une seule
    // origine, donc pas de CORS (vite preview reprend ce proxy)
    proxy: { "/api": "http://localhost:8080" },
  },
  test: {
    environment: "jsdom",
    setupFiles: "./src/setupTests.ts",
  },
});
