package com.fris.begems.attachment;

import com.fris.begems.attachment.dto.AttachmentSummary;
import com.fris.begems.security.AppUserPrincipal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class FindingAttachmentController {

    private final FindingAttachmentService attachmentService;

    public FindingAttachmentController(FindingAttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @GetMapping("/api/findings/{findingId}/attachments")
    public List<AttachmentSummary> list(@AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID findingId) {
        return attachmentService.listForFinding(principal, findingId);
    }

    @PostMapping("/api/findings/{findingId}/attachments")
    @PreAuthorize("hasAnyRole('COMPANY_SECRETARY', 'EVALUATOR')")
    public ResponseEntity<AttachmentSummary> upload(@AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID findingId, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(attachmentService.upload(principal, findingId, file));
    }

    @GetMapping("/api/attachments/{id}/download")
    public ResponseEntity<byte[]> download(@AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID id) {
        FindingAttachment attachment = attachmentService.download(principal, id);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(attachment.getFileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(attachment.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(attachment.getFileData());
    }

    @DeleteMapping("/api/attachments/{id}")
    @PreAuthorize("hasAnyRole('COMPANY_SECRETARY', 'EVALUATOR')")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id) {
        attachmentService.delete(principal, id);
        return ResponseEntity.noContent().build();
    }
}
