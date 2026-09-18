package com.fris.begems.evaluation.dto;

import com.fris.begems.evaluation.ConfidentialityMode;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AddRespondentRequest(@NotNull UUID directorId, @NotNull ConfidentialityMode confidentialityMode) {
}
