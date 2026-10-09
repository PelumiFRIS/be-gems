import { apiClient } from "./client";
import type { MyBoardReport, ReportApprovalStatus } from "./types";

export async function getReportApproval(evaluationId: string): Promise<ReportApprovalStatus> {
  const { data } = await apiClient.get<ReportApprovalStatus>(`/api/evaluations/${evaluationId}/report-approval`);
  return data;
}

export async function approveReport(evaluationId: string, comment: string): Promise<ReportApprovalStatus> {
  const { data } = await apiClient.post<ReportApprovalStatus>(
    `/api/evaluations/${evaluationId}/report-approval/approve`,
    { comment },
  );
  return data;
}

export async function returnReport(evaluationId: string, comment: string): Promise<ReportApprovalStatus> {
  const { data } = await apiClient.post<ReportApprovalStatus>(
    `/api/evaluations/${evaluationId}/report-approval/return`,
    { comment },
  );
  return data;
}

export async function listMyBoardReports(): Promise<MyBoardReport[]> {
  const { data } = await apiClient.get<MyBoardReport[]>("/api/my-board-reports");
  return data;
}
