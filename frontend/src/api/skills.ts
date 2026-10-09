import { apiClient } from "./client";
import type { SkillPayload, SkillsMatrix } from "./types";

export async function getSkillsMatrix(boardId: string): Promise<SkillsMatrix> {
  const { data } = await apiClient.get<SkillsMatrix>(`/api/boards/${boardId}/skills-matrix`);
  return data;
}

export async function addSkill(boardId: string, payload: SkillPayload): Promise<SkillsMatrix> {
  const { data } = await apiClient.post<SkillsMatrix>(`/api/boards/${boardId}/skills`, payload);
  return data;
}

export async function updateSkill(skillId: string, payload: SkillPayload): Promise<SkillsMatrix> {
  const { data } = await apiClient.put<SkillsMatrix>(`/api/skills/${skillId}`, payload);
  return data;
}

export async function removeSkill(skillId: string): Promise<SkillsMatrix> {
  const { data } = await apiClient.delete<SkillsMatrix>(`/api/skills/${skillId}`);
  return data;
}

/** A null rating clears the director's rating for the skill. */
export async function rateSkill(skillId: string, directorId: string, rating: number | null): Promise<SkillsMatrix> {
  const { data } = await apiClient.put<SkillsMatrix>(`/api/skills/${skillId}/ratings/${directorId}`, { rating });
  return data;
}
