import { apiClient } from "./client";
import type { FrameworkDetail } from "./types";

export async function getFramework(): Promise<FrameworkDetail> {
  const { data } = await apiClient.get<FrameworkDetail>("/api/frameworks/active");
  return data;
}
