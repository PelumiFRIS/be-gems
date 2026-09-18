package com.fris.begems.response.dto;

import com.fris.begems.evaluation.EvaluationStatus;
import com.fris.begems.evaluation.RespondentStatus;
import com.fris.begems.framework.EvaluationType;
import java.util.UUID;

public record MyEvaluationSummary(
        UUID evaluationId,
        EvaluationType evaluationType,
        String subjectDirectorName,
        int year,
        EvaluationStatus evaluationStatus,
        RespondentStatus myStatus,
        int totalQuestions,
        int answeredQuestions) {
}
