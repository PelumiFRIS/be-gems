import { apiClient } from "./client";
import type { MyEvaluationSummary, QuestionWithAnswer, SaveResponsePayload } from "./types";

export async function listMyEvaluations(): Promise<MyEvaluationSummary[]> {
  const { data } = await apiClient.get<MyEvaluationSummary[]>("/api/my-evaluations");
  return data;
}

export async function getMyQuestions(evaluationId: string): Promise<QuestionWithAnswer[]> {
  const { data } = await apiClient.get<QuestionWithAnswer[]>(`/api/my-evaluations/${evaluationId}/questions`);
  return data;
}

export async function saveMyResponse(
  evaluationId: string,
  questionId: string,
  payload: SaveResponsePayload,
): Promise<QuestionWithAnswer> {
  const { data } = await apiClient.put<QuestionWithAnswer>(
    `/api/my-evaluations/${evaluationId}/responses/${questionId}`,
    payload,
  );
  return data;
}

export async function submitMyEvaluation(evaluationId: string): Promise<void> {
  await apiClient.post(`/api/my-evaluations/${evaluationId}/submit`);
}
