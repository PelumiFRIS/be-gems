package com.fris.begems.evaluation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "evaluation_respondents")
@Getter
@Setter
@NoArgsConstructor
public class EvaluationRespondent {

    @Id
    private UUID id;

    @Column(name = "evaluation_id", nullable = false)
    private UUID evaluationId;

    @Column(name = "director_id", nullable = false)
    private UUID directorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "confidentiality_mode", nullable = false)
    private ConfidentialityMode confidentialityMode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RespondentStatus status;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static EvaluationRespondent create(UUID evaluationId, UUID directorId,
            ConfidentialityMode confidentialityMode) {
        EvaluationRespondent respondent = new EvaluationRespondent();
        respondent.setId(UUID.randomUUID());
        respondent.setEvaluationId(evaluationId);
        respondent.setDirectorId(directorId);
        respondent.setConfidentialityMode(confidentialityMode);
        respondent.setStatus(RespondentStatus.INVITED);
        respondent.setCreatedAt(Instant.now());
        return respondent;
    }
}
