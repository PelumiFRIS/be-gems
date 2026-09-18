package com.fris.begems.scoring;

import com.fris.begems.scoring.dto.ScoreRowSummary;
import com.fris.begems.security.AppUserPrincipal;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/evaluations/{evaluationId}/scores")
public class ScoringController {

    private final ScoringService scoringService;

    public ScoringController(ScoringService scoringService) {
        this.scoringService = scoringService;
    }

    @GetMapping
    public List<ScoreRowSummary> getScores(@AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID evaluationId) {
        return scoringService.getScores(principal, evaluationId);
    }

    @PostMapping("/calculate")
    @PreAuthorize("hasAnyRole('COMPANY_SECRETARY', 'EVALUATOR')")
    public List<ScoreRowSummary> calculate(@AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID evaluationId) {
        return scoringService.calculateScores(principal, evaluationId);
    }
}
