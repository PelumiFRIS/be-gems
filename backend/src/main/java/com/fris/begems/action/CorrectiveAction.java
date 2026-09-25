package com.fris.begems.action;

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
@Table(name = "corrective_actions")
@Getter
@Setter
@NoArgsConstructor
public class CorrectiveAction {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "finding_id", nullable = false)
    private UUID findingId;

    @Column(nullable = false)
    private String description;

    private String owner;

    private String approver;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionStatus status;

    private String evidence;

    @Column(name = "closure_date")
    private LocalDate closureDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static CorrectiveAction create(UUID organizationId, UUID findingId, String description, String owner,
            String approver, LocalDate dueDate, String evidence) {
        CorrectiveAction action = new CorrectiveAction();
        action.setId(UUID.randomUUID());
        action.setOrganizationId(organizationId);
        action.setFindingId(findingId);
        action.setDescription(description);
        action.setOwner(owner);
        action.setApprover(approver);
        action.setDueDate(dueDate);
        action.setStatus(ActionStatus.NOT_STARTED);
        action.setEvidence(evidence);
        action.setCreatedAt(Instant.now());
        return action;
    }
}
