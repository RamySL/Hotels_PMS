import type { Client } from "@/features/clients/types";

export const HELENE_DUPONT: Client = {
  id: "client-dupont",
  firstName: "Hélène",
  lastName: "Dupont",
  email: "helene.dupont@example.com",
  phone: "06 12 34 56 78",
};

/** Client venu au comptoir, sans email ni téléphone. */
export const JEAN_MARTIN: Client = {
  id: "client-martin",
  firstName: "Jean",
  lastName: "Martin",
  email: null,
  phone: null,
};
