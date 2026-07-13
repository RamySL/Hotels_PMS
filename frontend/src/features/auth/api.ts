import { api } from "@/shared/lib/apiClient";
import { endSession, startSession } from "./session";
import type { LoginResponse, User } from "./types";

/** Connecte la personne et ouvre sa session ; un refus remonte en ApiError (401, 400…). */
export async function login(email: string, password: string): Promise<User> {
  const response = await api.post<LoginResponse>("/auth/login", {
    email,
    password,
  });
  startSession({
    token: response.accessToken,
    user: response.user,
    expiresAt: Date.now() + response.expiresIn * 1000,
  });
  return response.user;
}

export function logout(): void {
  endSession("logout");
}
