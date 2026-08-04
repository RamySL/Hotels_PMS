export interface Client {
  id: string;
  firstName: string;
  lastName: string;
  email: string | null;
  phone: string | null;
}

/** Corps envoyé pour créer ou modifier un client ; email et téléphone sont facultatifs. */
export interface ClientRequest {
  firstName: string;
  lastName: string;
  email: string | null;
  phone: string | null;
}
