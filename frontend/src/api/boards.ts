import { apiClient } from "./client";
import type { BoardSummary, CreateBoardPayload } from "./types";

export async function listBoards(): Promise<BoardSummary[]> {
  const { data } = await apiClient.get<BoardSummary[]>("/api/boards");
  return data;
}

export async function createBoard(payload: CreateBoardPayload): Promise<BoardSummary> {
  const { data } = await apiClient.post<BoardSummary>("/api/boards", payload);
  return data;
}
