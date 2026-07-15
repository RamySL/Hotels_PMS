import type { RouteObject } from "react-router";
import { LoginPage } from "@/features/auth/LoginPage";
import { HomePage } from "./HomePage";
import { Layout } from "./Layout";
import { NotFoundPage } from "./NotFoundPage";

export const routes: RouteObject[] = [
  // Page autonome, sans l'en-tête de l'application
  { path: "/login", element: <LoginPage /> },
  {
    element: <Layout />,
    children: [
      { index: true, element: <HomePage /> },
      { path: "*", element: <NotFoundPage /> },
    ],
  },
];
