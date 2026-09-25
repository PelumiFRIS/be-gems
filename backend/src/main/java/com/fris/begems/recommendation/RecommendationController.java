package com.fris.begems.recommendation;

import com.fris.begems.recommendation.dto.CreateRecommendationRequest;
import com.fris.begems.recommendation.dto.RecommendationSummary;
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
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping("/api/findings/{findingId}/recommendations")
    public List<RecommendationSummary> list(@AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID findingId) {
        return recommendationService.listForFinding(principal, findingId);
    }

    @PostMapping("/api/findings/{findingId}/recommendations")
    @PreAuthorize("hasAnyRole('COMPANY_SECRETARY', 'EVALUATOR')")
    public ResponseEntity<RecommendationSummary> create(@AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID findingId, @Valid @RequestBody CreateRecommendationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(recommendationService.create(principal, findingId, request));
    }

    @PutMapping("/api/recommendations/{id}")
    @PreAuthorize("hasAnyRole('COMPANY_SECRETARY', 'EVALUATOR')")
    public RecommendationSummary update(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id,
            @Valid @RequestBody CreateRecommendationRequest request) {
        return recommendationService.update(principal, id, request);
    }
}
