package com.fris.begems.benchmark.dto;

import java.util.List;

public record BenchmarkComparison(List<BenchmarkComparisonRow> rows, long belowCount) {
}
