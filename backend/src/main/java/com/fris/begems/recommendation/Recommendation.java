package com.fris.begems.recommendation;

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
@Table(name = "recommendations")
@Getter
@Setter
@NoArgsConstructor
public class Recommendation {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "finding_id", nullable = false)
    private UUID findingId;

    @Column(name = "recommended_action", nullable = false)
    private String recommendedAction;

    @Column(name = "responsible_person")
    private String responsiblePerson;

    @Column(name = "committee_responsible")
    private String committeeResponsible;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecommendationPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecommendationStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static Recommendation create(UUID organizationId, UUID findingId, String recommendedAction,
            String responsiblePerson, String committeeResponsible, LocalDate targetDate,
            RecommendationPriority priority, RecommendationStatus status) {
        Recommendation recommendation = new Recommendation();
        recommendation.setId(UUID.randomUUID());
        recommendation.setOrganizationId(organizationId);
        recommendation.setFindingId(findingId);
        recommendation.setRecommendedAction(recommendedAction);
        recommendation.setResponsiblePerson(responsiblePerson);
        recommendation.setCommitteeResponsible(committeeResponsible);
        recommendation.setTargetDate(targetDate);
        recommendation.setPriority(priority);
        recommendation.setStatus(status);
        recommendation.setCreatedAt(Instant.now());
        return recommendation;
    }
}
