package com.fris.begems.committee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateCommitteeRequest(
        @NotNull UUID boardId,
        @NotBlank String name,
        String meetingFrequency,
        String mandate) {
}
