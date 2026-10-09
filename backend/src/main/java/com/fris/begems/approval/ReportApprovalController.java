package com.fris.begems.approval;

import com.fris.begems.approval.dto.ApprovalRequest;
import com.fris.begems.approval.dto.MyBoardReport;
import com.fris.begems.approval.dto.ReportApprovalStatus;
import com.fris.begems.security.AppUserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Directors reach these too; which stage each person may act on is decided in the service. */
@RestController
public class ReportApprovalController {

    private final ReportApprovalService approvalService;

    public ReportApprovalController(ReportApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @GetMapping("/api/evaluations/{evaluationId}/report-approval")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'COMPANY_SECRETARY', 'EVALUATOR', 'DIRECTOR')")
    public ReportApprovalStatus status(@AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID evaluationId) {
        return approvalService.status(principal, evaluationId);
    }

    @PostMapping("/api/evaluations/{evaluationId}/report-approval/approve")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'COMPANY_SECRETARY', 'EVALUATOR', 'DIRECTOR')")
    public ReportApprovalStatus approve(@AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID evaluationId, @Valid @RequestBody ApprovalRequest request) {
        return approvalService.approve(principal, evaluationId, request);
    }

    @PostMapping("/api/evaluations/{evaluationId}/report-approval/return")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'COMPANY_SECRETARY', 'EVALUATOR', 'DIRECTOR')")
    public ReportApprovalStatus returnForChanges(@AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID evaluationId, @Valid @RequestBody ApprovalRequest request) {
        return approvalService.returnForChanges(principal, evaluationId, request);
    }

    @GetMapping("/api/my-board-reports")
    public List<MyBoardReport> myBoardReports(@AuthenticationPrincipal AppUserPrincipal principal) {
        return approvalService.myBoardReports(principal);
    }
}
