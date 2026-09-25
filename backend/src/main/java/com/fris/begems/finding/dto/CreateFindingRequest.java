package com.fris.begems.finding.dto;

import com.fris.begems.finding.FindingSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateFindingRequest(
        UUID dimensionId,
        @NotBlank String description,
        @NotNull FindingSeverity severity,
        String evidence,
        String regulatoryReference,
        String rootCause,
        String riskImplication) {
}
