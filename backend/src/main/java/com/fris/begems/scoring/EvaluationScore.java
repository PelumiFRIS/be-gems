package com.fris.begems.scoring;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "evaluation_scores")
@Getter
@Setter
@NoArgsConstructor
public class EvaluationScore {

    @Id
    private UUID id;

    @Column(name = "evaluation_id", nullable = false)
    private UUID evaluationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false)
    private ScoreScopeType scopeType;

    @Column(name = "dimension_id")
    private UUID dimensionId;

    @Column(name = "bgei_category")
    private String bgeiCategory;

    @Column(name = "raw_score")
    private BigDecimal rawScore;

    @Column(name = "weighted_score")
    private BigDecimal weightedScore;

    @Column(name = "maturity_level")
    private Integer maturityLevel;

    @Column(name = "bgei_band_label")
    private String bgeiBandLabel;

    @Column(name = "computed_at", nullable = false)
    private Instant computedAt;

    public static EvaluationScore create(UUID evaluationId, ScoreScopeType scopeType, UUID dimensionId,
            String bgeiCategory, BigDecimal rawScore, BigDecimal weightedScore, Integer maturityLevel,
            String bgeiBandLabel) {
        EvaluationScore score = new EvaluationScore();
        score.setId(UUID.randomUUID());
        score.setEvaluationId(evaluationId);
        score.setScopeType(scopeType);
        score.setDimensionId(dimensionId);
        score.setBgeiCategory(bgeiCategory);
        score.setRawScore(rawScore);
        score.setWeightedScore(weightedScore);
        score.setMaturityLevel(maturityLevel);
        score.setBgeiBandLabel(bgeiBandLabel);
        score.setComputedAt(Instant.now());
        return score;
    }
}
