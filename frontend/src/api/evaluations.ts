import { apiClient } from "./client";
import type {
  AddRespondentPayload,
  CreateEvaluationPayload,
  EvaluationDetail,
  EvaluationSummary,
} from "./types";

export async function listEvaluations(boardId: string): Promise<EvaluationSummary[]> {
  const { data } = await apiClient.get<EvaluationSummary[]>("/api/evaluations", { params: { boardId } });
  return data;
}

export async function getEvaluation(evaluationId: string): Promise<EvaluationDetail> {
  const { data } = await apiClient.get<EvaluationDetail>(`/api/evaluations/${evaluationId}`);
  return data;
}

export async function createEvaluation(payload: CreateEvaluationPayload): Promise<EvaluationSummary> {
  const { data } = await apiClient.post<EvaluationSummary>("/api/evaluations", payload);
  return data;
}

export async function addRespondent(evaluationId: string, payload: AddRespondentPayload): Promise<EvaluationDetail> {
  const { data } = await apiClient.post<EvaluationDetail>(`/api/evaluations/${evaluationId}/respondents`, payload);
  return data;
}

export async function launchEvaluation(evaluationId: string): Promise<EvaluationSummary> {
  const { data } = await apiClient.post<EvaluationSummary>(`/api/evaluations/${evaluationId}/launch`);
  return data;
}

export async function closeEvaluation(evaluationId: string): Promise<EvaluationSummary> {
  const { data } = await apiClient.post<EvaluationSummary>(`/api/evaluations/${evaluationId}/close`);
  return data;
}
