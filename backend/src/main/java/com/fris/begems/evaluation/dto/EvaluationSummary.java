package com.fris.begems.evaluation.dto;

import com.fris.begems.evaluation.Evaluation;
import com.fris.begems.evaluation.EvaluationStatus;
import com.fris.begems.framework.EvaluationType;
import java.time.LocalDate;
import java.util.UUID;

public record EvaluationSummary(
        UUID id,
        UUID boardId,
        EvaluationType evaluationType,
        UUID subjectDirectorId,
        String subjectDirectorName,
        int year,
        EvaluationStatus status,
        LocalDate startDate,
        LocalDate closeDate) {

    public static EvaluationSummary from(Evaluation evaluation, String subjectDirectorName) {
        return new EvaluationSummary(
                evaluation.getId(),
                evaluation.getBoardId(),
                evaluation.getEvaluationType(),
                evaluation.getSubjectDirectorId(),
                subjectDirectorName,
                evaluation.getYear(),
                evaluation.getStatus(),
                evaluation.getStartDate(),
                evaluation.getCloseDate());
    }
}
