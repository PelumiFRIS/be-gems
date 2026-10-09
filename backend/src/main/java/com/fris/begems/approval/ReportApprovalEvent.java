package com.fris.begems.approval;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "report_approval_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReportApprovalEvent {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "evaluation_id", nullable = false)
    private UUID evaluationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_stage", nullable = false)
    private ReportStage fromStage;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_stage", nullable = false)
    private ReportStage toStage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalDecision decision;

    private String comment;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Column(name = "actor_name", nullable = false)
    private String actorName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public static ReportApprovalEvent create(UUID organizationId, UUID evaluationId, ReportStage fromStage,
            ReportStage toStage, ApprovalDecision decision, String comment, UUID actorUserId, String actorName) {
        ReportApprovalEvent event = new ReportApprovalEvent();
        event.setId(UUID.randomUUID());
        event.setOrganizationId(organizationId);
        event.setEvaluationId(evaluationId);
        event.setFromStage(fromStage);
        event.setToStage(toStage);
        event.setDecision(decision);
        event.setComment(comment);
        event.setActorUserId(actorUserId);
        event.setActorName(actorName);
        event.setCreatedAt(Instant.now());
        return event;
    }
}
