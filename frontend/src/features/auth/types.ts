export type Role = "ADMIN" | "RECEPTIONIST";

export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: Role;
}

/** Réponse de POST /api/auth/login ; expiresIn est en secondes. */
export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}
