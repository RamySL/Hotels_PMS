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
    // À l'heure de l'hôtel, quelle que soit la machine : à Paris, le jour UTC diffère
    // du jour local entre minuit et 2 h, ce qui rend visibles les erreurs de fuseau
    env: { TZ: "Europe/Paris" },
  },
});
