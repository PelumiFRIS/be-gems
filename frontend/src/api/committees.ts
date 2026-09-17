import { apiClient } from "./client";
import type { CommitteeMemberRole, CommitteeSummary, CreateCommitteePayload } from "./types";

export async function listCommittees(boardId: string): Promise<CommitteeSummary[]> {
  const { data } = await apiClient.get<CommitteeSummary[]>("/api/committees", { params: { boardId } });
  return data;
}

export async function createCommittee(payload: CreateCommitteePayload): Promise<CommitteeSummary> {
  const { data } = await apiClient.post<CommitteeSummary>("/api/committees", payload);
  return data;
}

export async function addCommitteeMember(
  committeeId: string,
  directorId: string,
  role: CommitteeMemberRole,
): Promise<CommitteeSummary> {
  const { data } = await apiClient.post<CommitteeSummary>(`/api/committees/${committeeId}/members`, {
    directorId,
    role,
  });
  return data;
}
