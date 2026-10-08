import { apiClient } from "./client";
import type {
  CreatedUserResponse,
  CreateUserPayload,
  Role,
  TemporaryPasswordResponse,
  UserStatus,
  UserSummary,
} from "./types";

export async function listUsers(): Promise<UserSummary[]> {
  const { data } = await apiClient.get<UserSummary[]>("/api/users");
  return data;
}

export async function createUser(payload: CreateUserPayload): Promise<CreatedUserResponse> {
  const { data } = await apiClient.post<CreatedUserResponse>("/api/users", payload);
  return data;
}

export async function changeUserRole(userId: string, role: Role): Promise<UserSummary> {
  const { data } = await apiClient.put<UserSummary>(`/api/users/${userId}/role`, { role });
  return data;
}

export async function changeUserStatus(userId: string, status: UserStatus): Promise<UserSummary> {
  const { data } = await apiClient.put<UserSummary>(`/api/users/${userId}/status`, { status });
  return data;
}

export async function changeCompanySecretaryAccess(userId: string, enabled: boolean): Promise<UserSummary> {
  const { data } = await apiClient.put<UserSummary>(`/api/users/${userId}/company-secretary-access`, { enabled });
  return data;
}

export async function resetUserPassword(userId: string): Promise<TemporaryPasswordResponse> {
  const { data } = await apiClient.post<TemporaryPasswordResponse>(`/api/users/${userId}/reset-password`);
  return data;
}

export async function changeOwnPassword(currentPassword: string, newPassword: string): Promise<void> {
  await apiClient.put("/api/users/me/password", { currentPassword, newPassword });
}
