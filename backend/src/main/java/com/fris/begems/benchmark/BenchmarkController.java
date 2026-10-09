package com.fris.begems.benchmark;

import com.fris.begems.benchmark.dto.BenchmarkComparison;
import com.fris.begems.benchmark.dto.BenchmarkRequest;
import com.fris.begems.benchmark.dto.BenchmarkSettings;
import com.fris.begems.security.AppUserPrincipal;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Targets are set by the Org Admin or Company Secretary; staff can view them and the comparison. */
@RestController
public class BenchmarkController {

    private final BenchmarkService benchmarkService;

    public BenchmarkController(BenchmarkService benchmarkService) {
        this.benchmarkService = benchmarkService;
    }

    @GetMapping("/api/benchmarks")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'COMPANY_SECRETARY', 'EVALUATOR')")
    public BenchmarkSettings settings(@AuthenticationPrincipal AppUserPrincipal principal) {
        return benchmarkService.settings(principal);
    }

    @PutMapping("/api/benchmarks/dimensions/{dimensionId}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'COMPANY_SECRETARY')")
    public BenchmarkSettings setDimensionTarget(@AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID dimensionId, @Valid @RequestBody BenchmarkRequest request) {
        return benchmarkService.setDimensionTarget(principal, dimensionId, request);
    }

    @DeleteMapping("/api/benchmarks/dimensions/{dimensionId}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'COMPANY_SECRETARY')")
    public BenchmarkSettings resetDimensionTarget(@AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID dimensionId) {
        return benchmarkService.resetDimensionTarget(principal, dimensionId);
    }

    @PutMapping("/api/benchmarks/bgei")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'COMPANY_SECRETARY')")
    public BenchmarkSettings setBgeiTarget(@AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody BenchmarkRequest request) {
        return benchmarkService.setBgeiTarget(principal, request);
    }

    @DeleteMapping("/api/benchmarks/bgei")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'COMPANY_SECRETARY')")
    public BenchmarkSettings resetBgeiTarget(@AuthenticationPrincipal AppUserPrincipal principal) {
        return benchmarkService.resetBgeiTarget(principal);
    }

    @GetMapping("/api/evaluations/{evaluationId}/benchmark")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'COMPANY_SECRETARY', 'EVALUATOR')")
    public BenchmarkComparison compare(@AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID evaluationId) {
        return benchmarkService.compare(principal, evaluationId);
    }
}
