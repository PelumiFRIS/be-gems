package com.fris.begems.evaluation;

import com.fris.begems.evaluation.dto.AddRespondentRequest;
import com.fris.begems.evaluation.dto.CreateEvaluationRequest;
import com.fris.begems.evaluation.dto.EvaluationDetail;
import com.fris.begems.evaluation.dto.EvaluationSummary;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/evaluations")
public class EvaluationController {

    private final EvaluationService evaluationService;

    public EvaluationController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @GetMapping
    public List<EvaluationSummary> list(@AuthenticationPrincipal AppUserPrincipal principal,
            @RequestParam UUID boardId) {
        return evaluationService.listForBoard(principal, boardId);
    }

    @GetMapping("/{id}")
    public EvaluationDetail getDetail(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id) {
        return evaluationService.getDetail(principal, id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('COMPANY_SECRETARY', 'EVALUATOR')")
    public ResponseEntity<EvaluationSummary> create(@AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody CreateEvaluationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(evaluationService.create(principal, request));
    }

    @PostMapping("/{id}/respondents")
    @PreAuthorize("hasAnyRole('COMPANY_SECRETARY', 'EVALUATOR')")
    public EvaluationDetail addRespondent(@AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID id, @Valid @RequestBody AddRespondentRequest request) {
        return evaluationService.addRespondent(principal, id, request);
    }

    @PostMapping("/{id}/launch")
    @PreAuthorize("hasAnyRole('COMPANY_SECRETARY', 'EVALUATOR')")
    public EvaluationSummary launch(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id) {
        return evaluationService.launch(principal, id);
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('COMPANY_SECRETARY', 'EVALUATOR')")
    public EvaluationSummary close(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id) {
        return evaluationService.close(principal, id);
    }
}
