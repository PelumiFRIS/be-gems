import { apiClient } from "./client";
import type { CreateDirectorPayload, DirectorSummary } from "./types";

export async function listDirectors(boardId: string): Promise<DirectorSummary[]> {
  const { data } = await apiClient.get<DirectorSummary[]>("/api/directors", { params: { boardId } });
  return data;
}

export async function createDirector(payload: CreateDirectorPayload): Promise<DirectorSummary> {
  const { data } = await apiClient.post<DirectorSummary>("/api/directors", payload);
  return data;
}
