package com.fris.begems.approval.dto;

import com.fris.begems.approval.ReportStage;
import java.util.UUID;

public record MyBoardReport(UUID evaluationId, int year, String boardName, ReportStage stage, String stageLabel,
        boolean awaitingMyApproval) {
}
