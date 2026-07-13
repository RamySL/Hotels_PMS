import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { App } from "@/app/App";
import { initSession } from "@/features/auth/session";
import "./index.css";

// Avant le premier rendu : les toutes premières requêtes des pages portent déjà le jeton
initSession();

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
