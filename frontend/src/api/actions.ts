import { apiClient } from "./client";
import type { ActionRegisterRow, ActionSummary, CreateActionPayload, UpdateActionPayload } from "./types";

export async function listActions(findingId: string): Promise<ActionSummary[]> {
  const { data } = await apiClient.get<ActionSummary[]>(`/api/findings/${findingId}/actions`);
  return data;
}

export async function createAction(findingId: string, payload: CreateActionPayload): Promise<ActionSummary> {
  const { data } = await apiClient.post<ActionSummary>(`/api/findings/${findingId}/actions`, payload);
  return data;
}

export async function updateAction(actionId: string, payload: UpdateActionPayload): Promise<ActionSummary> {
  const { data } = await apiClient.put<ActionSummary>(`/api/actions/${actionId}`, payload);
  return data;
}

export async function getActionRegister(): Promise<ActionRegisterRow[]> {
  const { data } = await apiClient.get<ActionRegisterRow[]>("/api/actions");
  return data;
}
