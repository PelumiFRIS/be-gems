import { apiClient } from "./client";
import type { AuthResponse, LoginPayload, SignupPayload, UserSummary } from "./types";

export async function signup(payload: SignupPayload): Promise<AuthResponse> {
  const { data } = await apiClient.post<AuthResponse>("/api/organizations/signup", payload);
  return data;
}

export async function login(payload: LoginPayload): Promise<AuthResponse> {
  const { data } = await apiClient.post<AuthResponse>("/api/auth/login", payload);
  return data;
}

export async function fetchCurrentUser(): Promise<UserSummary> {
  const { data } = await apiClient.get<UserSummary>("/api/users/me");
  return data;
}
