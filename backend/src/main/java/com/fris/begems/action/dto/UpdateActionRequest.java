package com.fris.begems.action.dto;

import com.fris.begems.action.ActionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record UpdateActionRequest(
        @NotBlank String description,
        String owner,
        String approver,
        LocalDate dueDate,
        @NotNull ActionStatus status,
        String evidence) {
}
