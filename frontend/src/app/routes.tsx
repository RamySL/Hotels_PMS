import type { RouteObject } from "react-router";
import { LoginPage } from "@/features/auth/LoginPage";
import { RequireAdmin } from "@/features/auth/RequireAdmin";
import { RequireAuth } from "@/features/auth/RequireAuth";
import { ClientFormPage } from "@/features/clients/ClientFormPage";
import { ClientsPage } from "@/features/clients/ClientsPage";
import { PlanningPage } from "@/features/reservations/PlanningPage";
import { ReservationFormPage } from "@/features/reservations/ReservationFormPage";
import { ReservationPage } from "@/features/reservations/ReservationPage";
import { RoomFormPage } from "@/features/rooms/RoomFormPage";
import { RoomsPage } from "@/features/rooms/RoomsPage";
import { RoomTypeFormPage } from "@/features/rooms/RoomTypeFormPage";
import { RoomTypesPage } from "@/features/rooms/RoomTypesPage";
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
          { path: "chambres/types", element: <RoomTypesPage /> },
          { path: "clients", element: <ClientsPage /> },
          { path: "clients/nouveau", element: <ClientFormPage /> },
          { path: "clients/:id/modifier", element: <ClientFormPage /> },
          { path: "reservations", element: <PlanningPage /> },
          { path: "reservations/nouvelle", element: <ReservationFormPage /> },
          { path: "reservations/:id", element: <ReservationPage /> },
          {
            element: <RequireAdmin />,
            children: [
              { path: "chambres/nouvelle", element: <RoomFormPage /> },
              { path: "chambres/:id/modifier", element: <RoomFormPage /> },
              { path: "chambres/types/nouveau", element: <RoomTypeFormPage /> },
              {
                path: "chambres/types/:id/modifier",
                element: <RoomTypeFormPage />,
              },
            ],
          },
          { path: "*", element: <NotFoundPage /> },
        ],
      },
    ],
  },
];
