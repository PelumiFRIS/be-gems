package com.fris.begems.benchmark.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** The upper bound depends on the measure (5.00 for a dimension, 100.00 for the BGEI), so the service checks it. */
public record BenchmarkRequest(@NotNull @DecimalMin("0.00") @Digits(integer = 3, fraction = 2) BigDecimal target,
        @Size(max = 200) String source) {
}
