package com.fris.begems.action;

import com.fris.begems.action.dto.ActionRegisterRow;
import com.fris.begems.action.dto.ActionSummary;
import com.fris.begems.action.dto.CreateActionRequest;
import com.fris.begems.action.dto.UpdateActionRequest;
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
public class CorrectiveActionController {

    private final CorrectiveActionService actionService;

    public CorrectiveActionController(CorrectiveActionService actionService) {
        this.actionService = actionService;
    }

    @GetMapping("/api/findings/{findingId}/actions")
    public List<ActionSummary> list(@AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID findingId) {
        return actionService.listForFinding(principal, findingId);
    }

    @GetMapping("/api/actions")
    public List<ActionRegisterRow> register(@AuthenticationPrincipal AppUserPrincipal principal) {
        return actionService.listRegister(principal);
    }

    @PostMapping("/api/findings/{findingId}/actions")
    @PreAuthorize("hasAnyRole('COMPANY_SECRETARY', 'EVALUATOR')")
    public ResponseEntity<ActionSummary> create(@AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID findingId, @Valid @RequestBody CreateActionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(actionService.create(principal, findingId, request));
    }

    @PutMapping("/api/actions/{id}")
    @PreAuthorize("hasAnyRole('COMPANY_SECRETARY', 'EVALUATOR')")
    public ActionSummary update(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id,
            @Valid @RequestBody UpdateActionRequest request) {
        return actionService.update(principal, id, request);
    }
}
