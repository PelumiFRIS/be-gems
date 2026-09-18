package com.fris.begems.evaluation.dto;

import com.fris.begems.framework.EvaluationType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateEvaluationRequest(
        @NotNull UUID boardId,
        @NotNull EvaluationType evaluationType,
        UUID subjectDirectorId,
        @Min(2000) int year) {
}
