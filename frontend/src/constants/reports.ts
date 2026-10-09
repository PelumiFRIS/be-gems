import type { ReportStage } from "../api/types";

export const REPORT_STAGE_LABELS: Record<ReportStage, string> = {
  EVALUATOR_REVIEW: "Evaluator Review",
  DRAFT_REPORT: "Draft Report",
  QUALITY_REVIEW: "Quality Review",
  CS_REVIEW: "Company Secretary Review",
  BOARD_APPROVAL: "Chairman/Board Approval",
  FINAL: "Final Report",
};
