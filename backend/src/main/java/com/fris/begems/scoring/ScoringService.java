package com.fris.begems.scoring;

import com.fris.begems.audit.AuditAction;
import com.fris.begems.audit.AuditEntityType;
import com.fris.begems.audit.AuditLogService;
import com.fris.begems.common.ApiException;
import com.fris.begems.evaluation.Evaluation;
import com.fris.begems.evaluation.EvaluationRepository;
import com.fris.begems.evaluation.EvaluationRespondent;
import com.fris.begems.evaluation.EvaluationRespondentRepository;
import com.fris.begems.evaluation.EvaluationStatus;
import com.fris.begems.framework.Dimension;
import com.fris.begems.framework.DimensionRepository;
import com.fris.begems.framework.EvaluationType;
import com.fris.begems.framework.Question;
import com.fris.begems.framework.QuestionRepository;
import com.fris.begems.response.Response;
import com.fris.begems.response.ResponseRepository;
import com.fris.begems.scoring.dto.ScoreRowSummary;
import com.fris.begems.security.AppUserPrincipal;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The core scoring algorithm — see the plan's "Scoring design" section.
 * BOARD evaluations get the full treatment (weighted dimension scores, overall
 * board score, governance maturity, BGEI); DIRECTOR_PEER evaluations get a
 * simpler unweighted director-overall score, since weighting/BGEI/maturity are
 * board-level governance metrics that don't apply to an individual.
 */
@Service
public class ScoringService {

    private final EvaluationRepository evaluationRepository;
    private final EvaluationRespondentRepository respondentRepository;
    private final ResponseRepository responseRepository;
    private final QuestionRepository questionRepository;
    private final DimensionRepository dimensionRepository;
    private final EvaluationScoreRepository evaluationScoreRepository;
    private final MaturityLevelRepository maturityLevelRepository;
    private final BgeiBandRepository bgeiBandRepository;
    private final AuditLogService auditLogService;

    public ScoringService(EvaluationRepository evaluationRepository,
            EvaluationRespondentRepository respondentRepository, ResponseRepository responseRepository,
            QuestionRepository questionRepository, DimensionRepository dimensionRepository,
            EvaluationScoreRepository evaluationScoreRepository, MaturityLevelRepository maturityLevelRepository,
            BgeiBandRepository bgeiBandRepository, AuditLogService auditLogService) {
        this.evaluationRepository = evaluationRepository;
        this.respondentRepository = respondentRepository;
        this.responseRepository = responseRepository;
        this.questionRepository = questionRepository;
        this.dimensionRepository = dimensionRepository;
        this.evaluationScoreRepository = evaluationScoreRepository;
        this.maturityLevelRepository = maturityLevelRepository;
        this.bgeiBandRepository = bgeiBandRepository;
        this.auditLogService = auditLogService;
    }

    public List<ScoreRowSummary> getScores(AppUserPrincipal principal, UUID evaluationId) {
        Evaluation evaluation = requireEvaluation(principal, evaluationId);
        List<EvaluationScore> scores = evaluationScoreRepository.findByEvaluationId(evaluationId);
        return enrich(scores, evaluation.getFrameworkId());
    }

    @Transactional
    public List<ScoreRowSummary> calculateScores(AppUserPrincipal principal, UUID evaluationId) {
        Evaluation evaluation = requireEvaluation(principal, evaluationId);
        if (evaluation.getStatus() != EvaluationStatus.CLOSED) {
            throw ApiException.conflict("The evaluation must be closed before scores can be calculated");
        }

        List<EvaluationRespondent> respondents = respondentRepository.findByEvaluationId(evaluationId);
        List<UUID> respondentIds = respondents.stream().map(EvaluationRespondent::getId).toList();
        List<Response> responses = respondentIds.isEmpty() ? List.of()
                : responseRepository.findByEvaluationRespondentIdIn(respondentIds);

        List<Question> questions = questionRepository.findByFrameworkIdAndEvaluationTypeOrderByDisplayOrder(
                evaluation.getFrameworkId(), evaluation.getEvaluationType());
        Map<UUID, Question> questionById = questions.stream().collect(Collectors.toMap(Question::getId, q -> q));

        Map<UUID, List<BigDecimal>> ratingsByQuestion = new HashMap<>();
        for (Response response : responses) {
            if (response.getRatingValue() == null || !questionById.containsKey(response.getQuestionId())) {
                continue;
            }
            ratingsByQuestion.computeIfAbsent(response.getQuestionId(), k -> new ArrayList<>())
                    .add(BigDecimal.valueOf(response.getRatingValue()));
        }

        Map<UUID, BigDecimal> questionScores = new HashMap<>();
        ratingsByQuestion.forEach((questionId, ratings) -> questionScores.put(questionId, average(ratings)));

        Map<UUID, List<BigDecimal>> scoresByDimension = new HashMap<>();
        for (Question question : questions) {
            BigDecimal questionScore = questionScores.get(question.getId());
            if (questionScore == null) {
                continue;
            }
            scoresByDimension.computeIfAbsent(question.getDimensionId(), k -> new ArrayList<>()).add(questionScore);
        }

        Map<UUID, BigDecimal> dimensionScores = new HashMap<>();
        scoresByDimension.forEach((dimensionId, scores) -> dimensionScores.put(dimensionId, average(scores)));

        List<Dimension> allDimensions = dimensionRepository
                .findByFrameworkIdOrderByDisplayOrder(evaluation.getFrameworkId());
        Map<UUID, Dimension> dimensionsById = allDimensions.stream()
                .collect(Collectors.toMap(Dimension::getId, d -> d));

        List<EvaluationScore> toSave = new ArrayList<>();
        boolean isBoard = evaluation.getEvaluationType() == EvaluationType.BOARD;

        // Accumulated unrounded, then rounded once at the end — rounding each
        // dimension's contribution first and summing the rounded values can drift
        // by a cent or two versus the true total (confirmed by a failing test).
        BigDecimal unroundedOverall = BigDecimal.ZERO;

        for (Map.Entry<UUID, BigDecimal> entry : dimensionScores.entrySet()) {
            UUID dimensionId = entry.getKey();
            BigDecimal score = entry.getValue();
            Dimension dimension = dimensionsById.get(dimensionId);
            BigDecimal weightedScore = null;
            if (isBoard && dimension != null) {
                BigDecimal unroundedWeighted = score.multiply(dimension.getDefaultWeightPct())
                        .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
                unroundedOverall = unroundedOverall.add(unroundedWeighted);
                weightedScore = round(unroundedWeighted);
            }
            Integer maturityLevel = classifyMaturity(score);
            toSave.add(EvaluationScore.create(evaluationId, ScoreScopeType.DIMENSION, dimensionId, null,
                    round(score), weightedScore, maturityLevel, null));
        }

        if (isBoard) {
            BigDecimal overall = round(unroundedOverall);
            toSave.add(EvaluationScore.create(evaluationId, ScoreScopeType.BOARD_OVERALL, null, null, overall,
                    null, classifyMaturity(overall), null));

            Map<String, List<UUID>> dimensionIdsByCategory = allDimensions.stream()
                    .filter(d -> d.getBgeiCategory() != null)
                    .collect(Collectors.groupingBy(Dimension::getBgeiCategory,
                            Collectors.mapping(Dimension::getId, Collectors.toList())));

            BigDecimal unroundedBgeiOverallPct = BigDecimal.ZERO;
            for (Map.Entry<String, List<UUID>> categoryEntry : dimensionIdsByCategory.entrySet()) {
                String category = categoryEntry.getKey();
                List<UUID> dimensionIds = categoryEntry.getValue();
                BigDecimal categoryWeightPct = dimensionIds.stream()
                        .map(id -> dimensionsById.get(id).getDefaultWeightPct())
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                List<BigDecimal> scoredDimensions = dimensionIds.stream()
                        .map(dimensionScores::get)
                        .filter(Objects::nonNull)
                        .toList();
                if (scoredDimensions.isEmpty()) {
                    continue;
                }
                BigDecimal categoryScore = average(scoredDimensions);
                BigDecimal categoryScorePct = categoryScore.multiply(BigDecimal.valueOf(20));
                BigDecimal unroundedContribution = categoryScorePct.multiply(categoryWeightPct)
                        .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
                unroundedBgeiOverallPct = unroundedBgeiOverallPct.add(unroundedContribution);
                toSave.add(EvaluationScore.create(evaluationId, ScoreScopeType.BGEI_CATEGORY, null, category,
                        round(categoryScore), round(unroundedContribution), null, null));
            }
            BigDecimal bgeiOverallPct = round(unroundedBgeiOverallPct);
            String bandLabel = bgeiBandRepository
                    .findFirstByMinPctLessThanEqualAndMaxPctGreaterThanEqual(bgeiOverallPct, bgeiOverallPct)
                    .map(BgeiBand::getLabel)
                    .orElse(null);
            toSave.add(EvaluationScore.create(evaluationId, ScoreScopeType.BGEI_OVERALL, null, null, null,
                    bgeiOverallPct, null, bandLabel));
        } else if (!dimensionScores.isEmpty()) {
            BigDecimal directorOverall = average(new ArrayList<>(dimensionScores.values()));
            toSave.add(EvaluationScore.create(evaluationId, ScoreScopeType.DIRECTOR_OVERALL, null, null,
                    round(directorOverall), null, null, null));
        }

        evaluationScoreRepository.deleteByEvaluationId(evaluationId);
        evaluationScoreRepository.saveAll(toSave);

        evaluation.setStatus(EvaluationStatus.SCORED);
        evaluationRepository.save(evaluation);

        auditLogService.record(principal, AuditAction.EVALUATION_SCORED, AuditEntityType.EVALUATION, evaluationId,
                "Calculated scores");

        return enrich(toSave, evaluation.getFrameworkId());
    }

    private List<ScoreRowSummary> enrich(List<EvaluationScore> scores, UUID frameworkId) {
        Map<UUID, String> dimensionNames = dimensionRepository.findByFrameworkIdOrderByDisplayOrder(frameworkId)
                .stream()
                .collect(Collectors.toMap(Dimension::getId, Dimension::getName));
        Map<Integer, String> maturityLabels = maturityLevelRepository.findAll().stream()
                .collect(Collectors.toMap(MaturityLevel::getLevel, MaturityLevel::getLabel));
        return scores.stream()
                .map(score -> ScoreRowSummary.from(score, dimensionNames.get(score.getDimensionId()),
                        score.getMaturityLevel() == null ? null : maturityLabels.get(score.getMaturityLevel())))
                .toList();
    }

    private Integer classifyMaturity(BigDecimal score) {
        return maturityLevelRepository.findFirstByMinScoreLessThanEqualAndMaxScoreGreaterThanEqual(score, score)
                .map(MaturityLevel::getLevel)
                .orElse(null);
    }

    private BigDecimal average(List<BigDecimal> values) {
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 6, RoundingMode.HALF_UP);
    }

    private BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private Evaluation requireEvaluation(AppUserPrincipal principal, UUID evaluationId) {
        return evaluationRepository.findByIdAndOrganizationId(evaluationId, principal.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("Evaluation not found"));
    }
}
