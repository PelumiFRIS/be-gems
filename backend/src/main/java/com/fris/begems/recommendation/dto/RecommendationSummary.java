package com.fris.begems.recommendation.dto;

import com.fris.begems.recommendation.Recommendation;
import com.fris.begems.recommendation.RecommendationPriority;
import com.fris.begems.recommendation.RecommendationStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record RecommendationSummary(
        UUID id,
        UUID findingId,
        String recommendedAction,
        String responsiblePerson,
        String committeeResponsible,
        LocalDate targetDate,
        RecommendationPriority priority,
        RecommendationStatus status,
        Instant createdAt) {

    public static RecommendationSummary from(Recommendation recommendation) {
        return new RecommendationSummary(recommendation.getId(), recommendation.getFindingId(),
                recommendation.getRecommendedAction(), recommendation.getResponsiblePerson(),
                recommendation.getCommitteeResponsible(), recommendation.getTargetDate(),
                recommendation.getPriority(), recommendation.getStatus(), recommendation.getCreatedAt());
    }
}
