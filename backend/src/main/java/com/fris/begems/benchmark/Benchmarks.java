package com.fris.begems.benchmark;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/** One organisation's benchmarks, with the BE-GEMS defaults filling any dimension it hasn't set. */
public record Benchmarks(Map<UUID, Target> dimensions, Target defaultDimension, Target bgei) {

    public record Target(BigDecimal value, String source, boolean isDefault) {
    }

    public Target forDimension(UUID dimensionId) {
        return dimensions.getOrDefault(dimensionId, defaultDimension);
    }
}
