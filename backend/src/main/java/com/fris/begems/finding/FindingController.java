package com.fris.begems.finding;

import com.fris.begems.finding.dto.CreateFindingRequest;
import com.fris.begems.finding.dto.FindingSummary;
import com.fris.begems.security.AppUserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FindingController {

    private final FindingService findingService;

    public FindingController(FindingService findingService) {
        this.findingService = findingService;
    }

    @GetMapping("/api/evaluations/{evaluationId}/findings")
    public List<FindingSummary> list(@AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID evaluationId) {
        return findingService.listForEvaluation(principal, evaluationId);
    }

    @GetMapping("/api/findings/{id}")
    public FindingSummary getOne(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id) {
        return findingService.getOne(principal, id);
    }

    @PostMapping("/api/evaluations/{evaluationId}/findings")
    @PreAuthorize("hasAnyRole('COMPANY_SECRETARY', 'EVALUATOR')")
    public ResponseEntity<FindingSummary> create(@AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID evaluationId, @Valid @RequestBody CreateFindingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(findingService.create(principal, evaluationId, request));
    }

    @PutMapping("/api/findings/{id}")
    @PreAuthorize("hasAnyRole('COMPANY_SECRETARY', 'EVALUATOR')")
    public FindingSummary update(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id,
            @Valid @RequestBody CreateFindingRequest request) {
        return findingService.update(principal, id, request);
    }
}
