package com.fris.begems.action.dto;

import com.fris.begems.action.ActionStatus;
import com.fris.begems.action.CorrectiveAction;
import com.fris.begems.finding.FindingSeverity;
import com.fris.begems.framework.EvaluationType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** One row of the org-wide "live action register" (memo, Corrective Action Plan). */
public record ActionRegisterRow(
        UUID id,
        UUID findingId,
        String description,
        String owner,
        String approver,
        LocalDate dueDate,
        ActionStatus status,
        String evidence,
        LocalDate closureDate,
        boolean overdue,
        Instant createdAt,
        UUID evaluationId,
        int evaluationYear,
        EvaluationType evaluationType,
        String findingDescription,
        FindingSeverity findingSeverity) {

    public static ActionRegisterRow from(CorrectiveAction action, UUID evaluationId, int evaluationYear,
            EvaluationType evaluationType, String findingDescription, FindingSeverity findingSeverity) {
        boolean overdue = action.getDueDate() != null && action.getStatus() != ActionStatus.COMPLETED
                && action.getDueDate().isBefore(LocalDate.now());
        return new ActionRegisterRow(action.getId(), action.getFindingId(), action.getDescription(),
                action.getOwner(), action.getApprover(), action.getDueDate(), action.getStatus(),
                action.getEvidence(), action.getClosureDate(), overdue, action.getCreatedAt(), evaluationId,
                evaluationYear, evaluationType, findingDescription, findingSeverity);
    }
}
