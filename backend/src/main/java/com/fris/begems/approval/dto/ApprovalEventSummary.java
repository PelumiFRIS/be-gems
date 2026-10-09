package com.fris.begems.approval.dto;

import com.fris.begems.approval.ApprovalDecision;
import com.fris.begems.approval.ReportApprovalEvent;
import com.fris.begems.approval.ReportStage;
import java.time.Instant;

public record ApprovalEventSummary(ReportStage fromStage, String fromStageLabel, ReportStage toStage,
        String toStageLabel, ApprovalDecision decision, String comment, String actorName, Instant createdAt) {

    public static ApprovalEventSummary from(ReportApprovalEvent event) {
        return new ApprovalEventSummary(event.getFromStage(), event.getFromStage().label(), event.getToStage(),
                event.getToStage().label(), event.getDecision(), event.getComment(), event.getActorName(),
                event.getCreatedAt());
    }
}
