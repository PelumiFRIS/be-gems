import { apiClient } from "./client";
import type { CreateRecommendationPayload, RecommendationSummary } from "./types";

export async function listRecommendations(findingId: string): Promise<RecommendationSummary[]> {
  const { data } = await apiClient.get<RecommendationSummary[]>(`/api/findings/${findingId}/recommendations`);
  return data;
}

export async function createRecommendation(
  findingId: string,
  payload: CreateRecommendationPayload,
): Promise<RecommendationSummary> {
  const { data } = await apiClient.post<RecommendationSummary>(
    `/api/findings/${findingId}/recommendations`,
    payload,
  );
  return data;
}

export async function updateRecommendation(
  recommendationId: string,
  payload: CreateRecommendationPayload,
): Promise<RecommendationSummary> {
  const { data } = await apiClient.put<RecommendationSummary>(`/api/recommendations/${recommendationId}`, payload);
  return data;
}
