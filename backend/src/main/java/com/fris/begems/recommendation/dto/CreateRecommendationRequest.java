package com.fris.begems.recommendation.dto;

import com.fris.begems.recommendation.RecommendationPriority;
import com.fris.begems.recommendation.RecommendationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateRecommendationRequest(
        @NotBlank String recommendedAction,
        String responsiblePerson,
        String committeeResponsible,
        LocalDate targetDate,
        @NotNull RecommendationPriority priority,
        @NotNull RecommendationStatus status) {
}
