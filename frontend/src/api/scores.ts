import { apiClient } from "./client";
import type { ScoreRowSummary } from "./types";

export async function getScores(evaluationId: string): Promise<ScoreRowSummary[]> {
  const { data } = await apiClient.get<ScoreRowSummary[]>(`/api/evaluations/${evaluationId}/scores`);
  return data;
}

export async function calculateScores(evaluationId: string): Promise<ScoreRowSummary[]> {
  const { data } = await apiClient.post<ScoreRowSummary[]>(`/api/evaluations/${evaluationId}/scores/calculate`);
  return data;
}
