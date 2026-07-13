import { createBrowserRouter } from "react-router";
import { RouterProvider } from "react-router/dom";
import { routes } from "./routes";

const router = createBrowserRouter(routes);

/** Racine de l'application : les fournisseurs globaux viendront entourer le routeur. */
export function App() {
  return <RouterProvider router={router} />;
}
