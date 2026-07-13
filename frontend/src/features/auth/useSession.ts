import { useSyncExternalStore } from "react";
import { getSession, subscribeToSession } from "./session";

/** Session courante : le composant se met à jour à la connexion, à la déconnexion et à l'expiration. */
export function useSession() {
  return useSyncExternalStore(subscribeToSession, getSession);
}
