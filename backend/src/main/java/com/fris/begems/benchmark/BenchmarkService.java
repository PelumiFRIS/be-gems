package com.fris.begems.benchmark;

import com.fris.begems.audit.AuditAction;
import com.fris.begems.audit.AuditEntityType;
import com.fris.begems.audit.AuditLogService;
import com.fris.begems.benchmark.dto.BenchmarkComparison;
import com.fris.begems.benchmark.dto.BenchmarkRequest;
import com.fris.begems.benchmark.dto.BenchmarkSettings;
import com.fris.begems.benchmark.dto.BenchmarkTargetSummary;
import com.fris.begems.common.ApiException;
import com.fris.begems.evaluation.Evaluation;
import com.fris.begems.evaluation.EvaluationRepository;
import com.fris.begems.framework.Dimension;
import com.fris.begems.framework.DimensionRepository;
import com.fris.begems.framework.EvaluationType;
import com.fris.begems.framework.FrameworkService;
import com.fris.begems.framework.dto.DimensionSummary;
import com.fris.begems.scoring.EvaluationScore;
import com.fris.begems.scoring.EvaluationScoreRepository;
import com.fris.begems.scoring.MaturityLevel;
import com.fris.begems.scoring.MaturityLevelRepository;
import com.fris.begems.security.AppUserPrincipal;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BenchmarkService {

    /** Level 3 (Defined): practices formally documented and consistently applied. */
    private static final int DEFAULT_MATURITY_LEVEL = 3;
    /** Lower bound of the "Effective" BGEI band. */
    private static final BigDecimal DEFAULT_BGEI_PCT = new BigDecimal("70.00");
    private static final BigDecimal MAX_DIMENSION_TARGET = new BigDecimal("5.00");
    private static final BigDecimal MAX_BGEI_TARGET = new BigDecimal("100.00");

    private final GovernanceBenchmarkRepository benchmarkRepository;
    private final DimensionRepository dimensionRepository;
    private final FrameworkService frameworkService;
    private final MaturityLevelRepository maturityLevelRepository;
    private final EvaluationRepository evaluationRepository;
    private final EvaluationScoreRepository evaluationScoreRepository;
    private final AuditLogService auditLogService;

    public BenchmarkService(GovernanceBenchmarkRepository benchmarkRepository,
            DimensionRepository dimensionRepository, FrameworkService frameworkService,
            MaturityLevelRepository maturityLevelRepository, EvaluationRepository evaluationRepository,
            EvaluationScoreRepository evaluationScoreRepository, AuditLogService auditLogService) {
        this.benchmarkRepository = benchmarkRepository;
        this.dimensionRepository = dimensionRepository;
        this.frameworkService = frameworkService;
        this.maturityLevelRepository = maturityLevelRepository;
        this.evaluationRepository = evaluationRepository;
        this.evaluationScoreRepository = evaluationScoreRepository;
        this.auditLogService = auditLogService;
    }

    public BenchmarkSettings settings(AppUserPrincipal principal) {
        Benchmarks benchmarks = benchmarksFor(principal.getOrganizationId());
        List<BenchmarkTargetSummary> dimensions = frameworkService.getActiveFramework().dimensions().stream()
                .map(d -> summary(d, benchmarks.forDimension(d.id())))
                .toList();
        Benchmarks.Target bgei = benchmarks.bgei();
        return new BenchmarkSettings(dimensions,
                new BenchmarkTargetSummary(null, "BGEI", bgei.value(), bgei.source(), bgei.isDefault()),
                benchmarks.defaultDimension().value(), DEFAULT_BGEI_PCT);
    }

    /** Also used by the board report, which has already checked access. */
    public Benchmarks benchmarksFor(UUID organizationId) {
        Benchmarks.Target defaultDimension = defaultDimensionTarget();
        Benchmarks.Target bgei = new Benchmarks.Target(DEFAULT_BGEI_PCT,
                "BE-GEMS default: the lower bound of the Effective band", true);
        Map<UUID, Benchmarks.Target> dimensions = new HashMap<>();
        for (GovernanceBenchmark b : benchmarkRepository.findByOrganizationId(organizationId)) {
            Benchmarks.Target target = new Benchmarks.Target(b.getTarget(), b.getSource(), false);
            if (b.getDimensionId() == null) {
                bgei = target;
            } else {
                dimensions.put(b.getDimensionId(), target);
            }
        }
        return new Benchmarks(dimensions, defaultDimension, bgei);
    }

    public BenchmarkComparison compare(AppUserPrincipal principal, UUID evaluationId) {
        Evaluation evaluation = evaluationRepository
                .findByIdAndOrganizationId(evaluationId, principal.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("Evaluation not found"));
        if (evaluation.getEvaluationType() != EvaluationType.BOARD) {
            throw ApiException.conflict("Regulatory benchmarks apply to Board evaluations only");
        }
        List<EvaluationScore> scores = evaluationScoreRepository.findByEvaluationId(evaluationId);
        if (scores.isEmpty()) {
            throw ApiException.conflict("Scores have not been calculated for this evaluation yet");
        }
        List<Dimension> dimensions = dimensionRepository.findByFrameworkIdOrderByDisplayOrder(
                evaluation.getFrameworkId());
        return BenchmarkAnalysis.compare(dimensions, scores, benchmarksFor(principal.getOrganizationId()));
    }

    @Transactional
    public BenchmarkSettings setDimensionTarget(AppUserPrincipal principal, UUID dimensionId,
            BenchmarkRequest request) {
        Dimension dimension = dimensionRepository.findById(dimensionId)
                .orElseThrow(() -> ApiException.notFound("Dimension not found"));
        if (request.target().compareTo(MAX_DIMENSION_TARGET) > 0) {
            throw ApiException.badRequest("A dimension benchmark must be a score between 0.00 and 5.00");
        }
        GovernanceBenchmark benchmark = benchmarkRepository
                .findByOrganizationIdAndDimensionId(principal.getOrganizationId(), dimensionId)
                .orElseGet(() -> GovernanceBenchmark.create(principal.getOrganizationId(), dimensionId));
        save(benchmark, request);
        auditLogService.record(principal, AuditAction.BENCHMARK_UPDATED, AuditEntityType.BENCHMARK, dimensionId,
                "Set the " + dimension.getName() + " benchmark to " + benchmark.getTarget().toPlainString()
                        + sourceSuffix(benchmark.getSource()));
        return settings(principal);
    }

    @Transactional
    public BenchmarkSettings resetDimensionTarget(AppUserPrincipal principal, UUID dimensionId) {
        Dimension dimension = dimensionRepository.findById(dimensionId)
                .orElseThrow(() -> ApiException.notFound("Dimension not found"));
        benchmarkRepository.findByOrganizationIdAndDimensionId(principal.getOrganizationId(), dimensionId)
                .ifPresent(b -> {
                    benchmarkRepository.delete(b);
                    auditLogService.record(principal, AuditAction.BENCHMARK_RESET, AuditEntityType.BENCHMARK,
                            dimensionId, "Reset the " + dimension.getName() + " benchmark to the default");
                });
        return settings(principal);
    }

    @Transactional
    public BenchmarkSettings setBgeiTarget(AppUserPrincipal principal, BenchmarkRequest request) {
        if (request.target().compareTo(MAX_BGEI_TARGET) > 0) {
            throw ApiException.badRequest("The BGEI benchmark must be a percentage between 0.00 and 100.00");
        }
        GovernanceBenchmark benchmark = benchmarkRepository
                .findByOrganizationIdAndDimensionIdIsNull(principal.getOrganizationId())
                .orElseGet(() -> GovernanceBenchmark.create(principal.getOrganizationId(), null));
        save(benchmark, request);
        auditLogService.record(principal, AuditAction.BENCHMARK_UPDATED, AuditEntityType.BENCHMARK, null,
                "Set the BGEI benchmark to " + benchmark.getTarget().toPlainString() + "%"
                        + sourceSuffix(benchmark.getSource()));
        return settings(principal);
    }

    @Transactional
    public BenchmarkSettings resetBgeiTarget(AppUserPrincipal principal) {
        benchmarkRepository.findByOrganizationIdAndDimensionIdIsNull(principal.getOrganizationId())
                .ifPresent(b -> {
                    benchmarkRepository.delete(b);
                    auditLogService.record(principal, AuditAction.BENCHMARK_RESET, AuditEntityType.BENCHMARK, null,
                            "Reset the BGEI benchmark to the default");
                });
        return settings(principal);
    }

    private void save(GovernanceBenchmark benchmark, BenchmarkRequest request) {
        benchmark.setTarget(request.target().setScale(2, RoundingMode.HALF_UP));
        String source = request.source() == null ? null : request.source().trim();
        benchmark.setSource(source == null || source.isEmpty() ? null : source);
        benchmark.setUpdatedAt(Instant.now());
        benchmarkRepository.save(benchmark);
    }

    private Benchmarks.Target defaultDimensionTarget() {
        MaturityLevel level = maturityLevelRepository.findById(DEFAULT_MATURITY_LEVEL)
                .orElseThrow(() -> new IllegalStateException("Maturity level " + DEFAULT_MATURITY_LEVEL
                        + " is not configured"));
        return new Benchmarks.Target(level.getMinScore(),
                "BE-GEMS default: maturity Level " + level.getLevel() + " (" + level.getLabel() + ")", true);
    }

    private static BenchmarkTargetSummary summary(DimensionSummary dimension, Benchmarks.Target target) {
        return new BenchmarkTargetSummary(dimension.id(), dimension.name(), target.value(), target.source(),
                target.isDefault());
    }

    private static String sourceSuffix(String source) {
        return source == null ? "" : " (" + source + ")";
    }
}
