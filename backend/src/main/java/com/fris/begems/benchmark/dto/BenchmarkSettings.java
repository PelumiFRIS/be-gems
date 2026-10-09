package com.fris.begems.benchmark.dto;

import java.math.BigDecimal;
import java.util.List;

public record BenchmarkSettings(List<BenchmarkTargetSummary> dimensions, BenchmarkTargetSummary bgei,
        BigDecimal defaultDimensionTarget, BigDecimal defaultBgeiTarget) {
}
