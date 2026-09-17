package com.fris.begems.committee;

import com.fris.begems.committee.dto.AddCommitteeMemberRequest;
import com.fris.begems.committee.dto.CommitteeSummary;
import com.fris.begems.committee.dto.CreateCommitteeRequest;
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
@RequestMapping("/api/committees")
public class CommitteeController {

    private final CommitteeService committeeService;

    public CommitteeController(CommitteeService committeeService) {
        this.committeeService = committeeService;
    }

    @GetMapping
    public List<CommitteeSummary> list(@AuthenticationPrincipal AppUserPrincipal principal,
            @RequestParam UUID boardId) {
        return committeeService.listForBoard(principal, boardId);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'COMPANY_SECRETARY')")
    public ResponseEntity<CommitteeSummary> create(@AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody CreateCommitteeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(committeeService.create(principal, request));
    }

    @PostMapping("/{id}/members")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'COMPANY_SECRETARY')")
    public CommitteeSummary addMember(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id,
            @Valid @RequestBody AddCommitteeMemberRequest request) {
        return committeeService.addMember(principal, id, request);
    }
}
