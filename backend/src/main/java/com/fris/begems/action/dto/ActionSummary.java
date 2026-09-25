package com.fris.begems.action.dto;

import com.fris.begems.action.ActionStatus;
import com.fris.begems.action.CorrectiveAction;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ActionSummary(
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
        Instant createdAt) {

    public static ActionSummary from(CorrectiveAction action) {
        boolean overdue = action.getDueDate() != null && action.getStatus() != ActionStatus.COMPLETED
                && action.getDueDate().isBefore(LocalDate.now());
        return new ActionSummary(action.getId(), action.getFindingId(), action.getDescription(), action.getOwner(),
                action.getApprover(), action.getDueDate(), action.getStatus(), action.getEvidence(),
                action.getClosureDate(), overdue, action.getCreatedAt());
    }
}
