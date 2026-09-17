package com.fris.begems.director.dto;

import com.fris.begems.director.DirectorClassification;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record CreateDirectorRequest(
        @NotNull UUID boardId,
        @NotBlank String name,
        String email,
        @NotNull DirectorClassification classification,
        LocalDate appointmentDate,
        LocalDate termExpirationDate) {
}
