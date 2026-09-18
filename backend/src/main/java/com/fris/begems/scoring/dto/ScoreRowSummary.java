package com.fris.begems.scoring.dto;

import com.fris.begems.scoring.EvaluationScore;
import com.fris.begems.scoring.ScoreScopeType;
import java.math.BigDecimal;
import java.util.UUID;

public record ScoreRowSummary(
        ScoreScopeType scopeType,
        UUID dimensionId,
        String dimensionName,
        String bgeiCategory,
        BigDecimal rawScore,
        BigDecimal weightedScore,
        Integer maturityLevel,
        String maturityLabel,
        String bgeiBandLabel) {

    public static ScoreRowSummary from(EvaluationScore score, String dimensionName, String maturityLabel) {
        return new ScoreRowSummary(score.getScopeType(), score.getDimensionId(), dimensionName,
                score.getBgeiCategory(), score.getRawScore(), score.getWeightedScore(), score.getMaturityLevel(),
                maturityLabel, score.getBgeiBandLabel());
    }
}
