package com.fris.begems.evaluation;

import com.fris.begems.framework.EvaluationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "evaluations")
@Getter
@Setter
@NoArgsConstructor
public class Evaluation {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "board_id", nullable = false)
    private UUID boardId;

    @Column(name = "framework_id", nullable = false)
    private UUID frameworkId;

    @Enumerated(EnumType.STRING)
    @Column(name = "evaluation_type", nullable = false)
    private EvaluationType evaluationType;

    @Column(name = "subject_director_id")
    private UUID subjectDirectorId;

    @Column(nullable = false)
    private int year;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EvaluationStatus status;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "close_date")
    private LocalDate closeDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static Evaluation create(UUID organizationId, UUID boardId, UUID frameworkId,
            EvaluationType evaluationType, UUID subjectDirectorId, int year) {
        Evaluation evaluation = new Evaluation();
        evaluation.setId(UUID.randomUUID());
        evaluation.setOrganizationId(organizationId);
        evaluation.setBoardId(boardId);
        evaluation.setFrameworkId(frameworkId);
        evaluation.setEvaluationType(evaluationType);
        evaluation.setSubjectDirectorId(subjectDirectorId);
        evaluation.setYear(year);
        evaluation.setStatus(EvaluationStatus.DRAFT);
        evaluation.setCreatedAt(Instant.now());
        return evaluation;
    }
}
