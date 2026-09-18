package com.fris.begems.evaluation.dto;

import com.fris.begems.evaluation.ConfidentialityMode;
import com.fris.begems.evaluation.EvaluationRespondent;
import com.fris.begems.evaluation.RespondentStatus;
import java.time.Instant;
import java.util.UUID;

public record RespondentSummary(
        UUID id,
        UUID directorId,
        String directorName,
        ConfidentialityMode confidentialityMode,
        RespondentStatus status,
        Instant submittedAt) {

    public static RespondentSummary from(EvaluationRespondent respondent, String directorName) {
        return new RespondentSummary(respondent.getId(), respondent.getDirectorId(), directorName,
                respondent.getConfidentialityMode(), respondent.getStatus(), respondent.getSubmittedAt());
    }
}
