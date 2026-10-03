import { apiClient } from "./client";
import type {
  CreateDirectorPayload,
  DirectorCvSummary,
  DirectorDetail,
  DirectorProfilePayload,
  DirectorSummary,
  InviteDirectorResponse,
} from "./types";

export async function listDirectors(boardId: string): Promise<DirectorSummary[]> {
  const { data } = await apiClient.get<DirectorSummary[]>("/api/directors", { params: { boardId } });
  return data;
}

export async function getDirector(directorId: string): Promise<DirectorDetail> {
  const { data } = await apiClient.get<DirectorDetail>(`/api/directors/${directorId}`);
  return data;
}

export async function createDirector(payload: CreateDirectorPayload): Promise<DirectorSummary> {
  const { data } = await apiClient.post<DirectorSummary>("/api/directors", payload);
  return data;
}

export async function updateDirector(directorId: string, payload: DirectorProfilePayload): Promise<DirectorDetail> {
  const { data } = await apiClient.put<DirectorDetail>(`/api/directors/${directorId}`, payload);
  return data;
}

export async function deleteDirector(directorId: string): Promise<void> {
  await apiClient.delete(`/api/directors/${directorId}`);
}

export async function inviteDirector(directorId: string): Promise<InviteDirectorResponse> {
  const { data } = await apiClient.post<InviteDirectorResponse>(`/api/directors/${directorId}/invite`);
  return data;
}

export async function uploadDirectorCv(directorId: string, file: File): Promise<DirectorCvSummary> {
  const formData = new FormData();
  formData.append("file", file);
  const { data } = await apiClient.post<DirectorCvSummary>(`/api/directors/${directorId}/cv`, formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return data;
}

export async function deleteDirectorCv(directorId: string): Promise<void> {
  await apiClient.delete(`/api/directors/${directorId}/cv`);
}

export async function downloadDirectorCv(directorId: string, fileName: string): Promise<void> {
  const { data } = await apiClient.get<Blob>(`/api/directors/${directorId}/cv`, { responseType: "blob" });
  const url = window.URL.createObjectURL(data);
  const link = document.createElement("a");
  link.href = url;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
}
