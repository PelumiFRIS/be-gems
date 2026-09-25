package com.fris.begems.action.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record CreateActionRequest(
        @NotBlank String description,
        String owner,
        String approver,
        LocalDate dueDate,
        String evidence) {
}
