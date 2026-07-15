import { Navigate, Outlet, useLocation } from "react-router";
import { useSession } from "./useSession";

/**
 * Réserve les routes enfants aux personnes connectées. Les autres passent par la page de
 * connexion, qui les ramène ensuite à la page demandée.
 */
export function RequireAuth() {
  const session = useSession();
  const location = useLocation();

  if (!session) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }
  return <Outlet />;
}
