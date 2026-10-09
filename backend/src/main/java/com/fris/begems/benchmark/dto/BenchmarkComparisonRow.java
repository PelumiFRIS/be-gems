package com.fris.begems.benchmark.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** {@code dimensionId} is null for the BGEI row, whose values are percentages rather than scores out of 5. */
public record BenchmarkComparisonRow(UUID dimensionId, String measure, BigDecimal actual, BigDecimal benchmark,
        BigDecimal variance, boolean belowBenchmark, String source, boolean defaultBenchmark) {
}
