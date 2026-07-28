import type { RouteObject } from "react-router";
import { LoginPage } from "@/features/auth/LoginPage";
import { RequireAuth } from "@/features/auth/RequireAuth";
import { RoomsPage } from "@/features/rooms/RoomsPage";
import { HomePage } from "./HomePage";
import { Layout } from "./Layout";
import { NotFoundPage } from "./NotFoundPage";

export const routes: RouteObject[] = [
  // Page autonome, sans l'en-tête de l'application
  { path: "/login", element: <LoginPage /> },
  {
    // Tout le reste, page 404 comprise, est réservé aux personnes connectées
    element: <RequireAuth />,
    children: [
      {
        element: <Layout />,
        children: [
          { index: true, element: <HomePage /> },
          { path: "chambres", element: <RoomsPage /> },
          { path: "*", element: <NotFoundPage /> },
        ],
      },
    ],
  },
];
