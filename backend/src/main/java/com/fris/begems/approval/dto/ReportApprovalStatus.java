package com.fris.begems.approval.dto;

import com.fris.begems.approval.ReportStage;
import java.util.List;
import java.util.UUID;

/**
 * What the signed-in user sees and may do at the current stage. {@code commentRequired} is set when a Company
 * Secretary records the Board's approval on its behalf, so the record says when and where it was given.
 */
public record ReportApprovalStatus(UUID evaluationId, int year, ReportStage stage, String stageLabel,
        List<StageSummary> stages, List<ApprovalEventSummary> history, boolean canApprove, boolean canReturn,
        boolean commentRequired, String approveLabel) {
}
