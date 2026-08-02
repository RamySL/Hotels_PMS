import { useSession } from "./useSession";

/** La personne connectée fait-elle partie de l'administration ? */
export function useIsAdmin(): boolean {
  return useSession()?.user.role === "ADMIN";
}
