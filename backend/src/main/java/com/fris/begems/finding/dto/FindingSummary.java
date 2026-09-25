package com.fris.begems.finding.dto;

import com.fris.begems.finding.Finding;
import com.fris.begems.finding.FindingSeverity;
import java.time.Instant;
import java.util.UUID;

public record FindingSummary(
        UUID id,
        UUID evaluationId,
        UUID dimensionId,
        String dimensionName,
        String description,
        FindingSeverity severity,
        String evidence,
        String regulatoryReference,
        String rootCause,
        String riskImplication,
        Instant createdAt) {

    public static FindingSummary from(Finding finding, String dimensionName) {
        return new FindingSummary(finding.getId(), finding.getEvaluationId(), finding.getDimensionId(),
                dimensionName, finding.getDescription(), finding.getSeverity(), finding.getEvidence(),
                finding.getRegulatoryReference(), finding.getRootCause(), finding.getRiskImplication(),
                finding.getCreatedAt());
    }
}
