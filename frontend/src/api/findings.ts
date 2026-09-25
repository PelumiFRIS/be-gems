import { apiClient } from "./client";
import type { CreateFindingPayload, FindingSummary } from "./types";

export async function listFindings(evaluationId: string): Promise<FindingSummary[]> {
  const { data } = await apiClient.get<FindingSummary[]>(`/api/evaluations/${evaluationId}/findings`);
  return data;
}

export async function getFinding(findingId: string): Promise<FindingSummary> {
  const { data } = await apiClient.get<FindingSummary>(`/api/findings/${findingId}`);
  return data;
}

export async function createFinding(evaluationId: string, payload: CreateFindingPayload): Promise<FindingSummary> {
  const { data } = await apiClient.post<FindingSummary>(`/api/evaluations/${evaluationId}/findings`, payload);
  return data;
}

export async function updateFinding(findingId: string, payload: CreateFindingPayload): Promise<FindingSummary> {
  const { data } = await apiClient.put<FindingSummary>(`/api/findings/${findingId}`, payload);
  return data;
}
