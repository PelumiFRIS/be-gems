package com.fris.begems.report;

import com.fris.begems.report.ReportService.GeneratedReport;
import com.fris.begems.security.AppUserPrincipal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/evaluations/{evaluationId}/report")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'COMPANY_SECRETARY', 'EVALUATOR', 'DIRECTOR')")
    public ResponseEntity<byte[]> report(@AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID evaluationId) {
        GeneratedReport report = reportService.generate(principal, evaluationId);
        String encodedName = URLEncoder.encode(report.fileName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/html;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encodedName)
                .cacheControl(CacheControl.noStore())
                .body(report.html());
    }
}
