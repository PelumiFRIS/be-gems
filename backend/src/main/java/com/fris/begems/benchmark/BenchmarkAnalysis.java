package com.fris.begems.benchmark;

import com.fris.begems.benchmark.dto.BenchmarkComparison;
import com.fris.begems.benchmark.dto.BenchmarkComparisonRow;
import com.fris.begems.framework.Dimension;
import com.fris.begems.scoring.EvaluationScore;
import com.fris.begems.scoring.ScoreScopeType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Actual versus benchmark for each scored dimension of a Board evaluation, then the BGEI. */
public final class BenchmarkAnalysis {

    private BenchmarkAnalysis() {
    }

    public static BenchmarkComparison compare(List<Dimension> dimensions, List<EvaluationScore> scores,
            Benchmarks benchmarks) {
        Map<UUID, EvaluationScore> dimensionScores = scores.stream()
                .filter(s -> s.getScopeType() == ScoreScopeType.DIMENSION && s.getDimensionId() != null)
                .collect(Collectors.toMap(EvaluationScore::getDimensionId, Function.identity()));
        List<BenchmarkComparisonRow> rows = new ArrayList<>();
        dimensions.stream()
                .sorted(Comparator.comparingInt(Dimension::getDisplayOrder))
                .filter(d -> dimensionScores.containsKey(d.getId()))
                .forEach(d -> rows.add(row(d.getId(), d.getName(), dimensionScores.get(d.getId()).getRawScore(),
                        benchmarks.forDimension(d.getId()))));
        scores.stream()
                .filter(s -> s.getScopeType() == ScoreScopeType.BGEI_OVERALL && s.getWeightedScore() != null)
                .findFirst()
                .ifPresent(s -> rows.add(row(null, "BGEI", s.getWeightedScore(), benchmarks.bgei())));
        return new BenchmarkComparison(rows, rows.stream().filter(BenchmarkComparisonRow::belowBenchmark).count());
    }

    private static BenchmarkComparisonRow row(UUID dimensionId, String measure, BigDecimal actual,
            Benchmarks.Target target) {
        BigDecimal variance = actual.subtract(target.value());
        return new BenchmarkComparisonRow(dimensionId, measure, actual, target.value(), variance,
                variance.signum() < 0, target.source(), target.isDefault());
    }
}
