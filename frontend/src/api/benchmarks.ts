import { apiClient } from "./client";
import type { BenchmarkComparison, BenchmarkPayload, BenchmarkSettings } from "./types";

export async function getBenchmarks(): Promise<BenchmarkSettings> {
  const { data } = await apiClient.get<BenchmarkSettings>("/api/benchmarks");
  return data;
}

/** A null `dimensionId` targets the BGEI benchmark. */
export async function setBenchmark(dimensionId: string | null, payload: BenchmarkPayload): Promise<BenchmarkSettings> {
  const path = dimensionId ? `/api/benchmarks/dimensions/${dimensionId}` : "/api/benchmarks/bgei";
  const { data } = await apiClient.put<BenchmarkSettings>(path, payload);
  return data;
}

export async function resetBenchmark(dimensionId: string | null): Promise<BenchmarkSettings> {
  const path = dimensionId ? `/api/benchmarks/dimensions/${dimensionId}` : "/api/benchmarks/bgei";
  const { data } = await apiClient.delete<BenchmarkSettings>(path);
  return data;
}

export async function getBenchmarkComparison(evaluationId: string): Promise<BenchmarkComparison> {
  const { data } = await apiClient.get<BenchmarkComparison>(`/api/evaluations/${evaluationId}/benchmark`);
  return data;
}
