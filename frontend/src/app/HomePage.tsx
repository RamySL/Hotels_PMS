import { useEffect, useState } from "react";
import { api } from "@/shared/lib/apiClient";

type ApiStatus = "checking" | "up" | "down";

const API_STATUS: Record<ApiStatus, { label: string; className: string }> = {
  checking: { label: "Vérification de l'API…", className: "text-slate-500" },
  up: { label: "API disponible", className: "text-green-700" },
  down: { label: "API injoignable", className: "text-red-700" },
};

/** Accueil provisoire, en attendant le tableau de bord du jour. */
export function HomePage() {
  const [apiStatus, setApiStatus] = useState<ApiStatus>("checking");

  useEffect(() => {
    let active = true;
    api.get("/ping").then(
      () => {
        if (active) {
          setApiStatus("up");
        }
      },
      () => {
        if (active) {
          setApiStatus("down");
        }
      },
    );
    return () => {
      active = false;
    };
  }, []);

  return (
    <>
      <h1 className="text-3xl font-bold text-slate-900">Accueil</h1>
      <p className="mt-2 text-slate-600">
        Réservations, chambres et facturation de l'hôtel.
      </p>
      <p
        role="status"
        className={`mt-6 text-sm ${API_STATUS[apiStatus].className}`}
      >
        {API_STATUS[apiStatus].label}
      </p>
    </>
  );
}
