import { api } from "@/shared/lib/apiClient";
import type { Client, ClientRequest } from "./types";

export function createClient(request: ClientRequest): Promise<Client> {
  return api.post<Client>("/clients", request);
}

export function updateClient(
  id: string,
  request: ClientRequest,
): Promise<Client> {
  return api.put<Client>(`/clients/${id}`, request);
}
