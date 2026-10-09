package com.fris.begems.report;

import com.fris.begems.action.CorrectiveAction;
import com.fris.begems.approval.ReportApprovalEvent;
import com.fris.begems.benchmark.Benchmarks;
import com.fris.begems.board.Board;
import com.fris.begems.committee.Committee;
import com.fris.begems.committee.CommitteeMember;
import com.fris.begems.director.Director;
import com.fris.begems.evaluation.Evaluation;
import com.fris.begems.evaluation.EvaluationRespondent;
import com.fris.begems.finding.Finding;
import com.fris.begems.finding.FindingSeverity;
import com.fris.begems.framework.Dimension;
import com.fris.begems.framework.Framework;
import com.fris.begems.framework.Question;
import com.fris.begems.organization.Organization;
import com.fris.begems.recommendation.Recommendation;
import com.fris.begems.response.Response;
import com.fris.begems.scoring.BgeiBand;
import com.fris.begems.scoring.EvaluationScore;
import com.fris.begems.scoring.MaturityLevel;
import com.fris.begems.scoring.ScoreScopeType;
import com.fris.begems.skill.dto.SkillsMatrix;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Everything a report needs, loaded once by ReportService so the writers do no I/O. */
record ReportContext(
        Organization organization,
        Board board,
        Framework framework,
        Evaluation evaluation,
        List<Director> directors,
        List<Committee> committees,
        List<CommitteeMember> committeeMembers,
        List<Dimension> dimensions,
        List<EvaluationScore> scores,
        List<MaturityLevel> maturityLevels,
        List<BgeiBand> bgeiBands,
        List<Question> questions,
        List<EvaluationRespondent> respondents,
        List<Response> responses,
        List<Finding> findings,
        List<Recommendation> recommendations,
        List<CorrectiveAction> actions,
        List<BigDecimal> peerDirectorScores,
        SkillsMatrix skills,
        Benchmarks benchmarks,
        List<ReportApprovalEvent> approvalHistory,
        LocalDate generatedOn) {

    Map<UUID, Director> directorsById() {
        return directors.stream().collect(Collectors.toMap(Director::getId, Function.identity()));
    }

    Map<UUID, Dimension> dimensionsById() {
        return dimensions.stream().collect(Collectors.toMap(Dimension::getId, Function.identity()));
    }

    Optional<Dimension> dimensionByCode(String code) {
        return dimensions.stream().filter(d -> d.getCode().equals(code)).findFirst();
    }

    String dimensionName(UUID dimensionId) {
        if (dimensionId == null) {
            return "Board-wide";
        }
        Dimension dimension = dimensionsById().get(dimensionId);
        return dimension == null ? "Board-wide" : dimension.getName();
    }

    List<EvaluationScore> scoresOf(ScoreScopeType scopeType) {
        return scores.stream().filter(s -> s.getScopeType() == scopeType).toList();
    }

    Optional<EvaluationScore> scoreOf(ScoreScopeType scopeType) {
        return scoresOf(scopeType).stream().findFirst();
    }

    Optional<MaturityLevel> maturityLevel(Integer level) {
        return level == null ? Optional.empty()
                : maturityLevels.stream().filter(m -> m.getLevel() == level).findFirst();
    }

    /** Classifies a derived (not persisted) score, rounding first exactly as ScoringService does. */
    Optional<MaturityLevel> maturityFor(BigDecimal score) {
        if (score == null) {
            return Optional.empty();
        }
        BigDecimal rounded = ReportFormat.round(score);
        return maturityLevels.stream()
                .filter(m -> m.getMinScore().compareTo(rounded) <= 0 && m.getMaxScore().compareTo(rounded) >= 0)
                .findFirst();
    }

    String maturityLabelFor(BigDecimal score) {
        return maturityFor(score).map(m -> "Level " + m.getLevel() + " – " + m.getLabel()).orElse(null);
    }

    List<Response> responsesFrom(Collection<EvaluationRespondent> subset) {
        Set<UUID> ids = subset.stream().map(EvaluationRespondent::getId).collect(Collectors.toSet());
        return responses.stream().filter(r -> ids.contains(r.getEvaluationRespondentId())).toList();
    }

    /** Most severe first, then oldest first — the order findings are numbered F1, F2… in reports. */
    List<Finding> findingsInReportOrder() {
        return findings.stream()
                .sorted(Comparator.comparing(Finding::getSeverity, Comparator.comparingInt(FindingSeverity::ordinal))
                        .thenComparing(Finding::getCreatedAt))
                .toList();
    }
}
