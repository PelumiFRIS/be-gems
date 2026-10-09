package com.fris.begems.benchmark.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** {@code dimensionId} is null for the BGEI target, which is a percentage rather than a score out of 5. */
public record BenchmarkTargetSummary(UUID dimensionId, String name, BigDecimal target, String source,
        boolean isDefault) {
}
