package com.fris.begems.attachment;

import com.fris.begems.attachment.dto.AttachmentRow;
import com.fris.begems.attachment.dto.AttachmentSummary;
import com.fris.begems.audit.AuditAction;
import com.fris.begems.audit.AuditEntityType;
import com.fris.begems.audit.AuditLogService;
import com.fris.begems.common.ApiException;
import com.fris.begems.finding.Finding;
import com.fris.begems.finding.FindingRepository;
import com.fris.begems.security.AppUserPrincipal;
import com.fris.begems.user.User;
import com.fris.begems.user.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FindingAttachmentService {

    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024;

    private final FindingAttachmentRepository attachmentRepository;
    private final FindingRepository findingRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public FindingAttachmentService(FindingAttachmentRepository attachmentRepository,
            FindingRepository findingRepository, UserRepository userRepository, AuditLogService auditLogService) {
        this.attachmentRepository = attachmentRepository;
        this.findingRepository = findingRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    public List<AttachmentSummary> listForFinding(AppUserPrincipal principal, UUID findingId) {
        requireFinding(principal, findingId);
        List<AttachmentRow> rows = attachmentRepository.findRowsByFindingId(findingId);

        List<UUID> uploaderIds = rows.stream().map(AttachmentRow::uploadedBy).filter(Objects::nonNull).distinct()
                .toList();
        Map<UUID, String> uploaderNamesById = userRepository.findAllById(uploaderIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u.getFirstName() + " " + u.getLastName()));

        return rows.stream().map(row -> AttachmentSummary.from(row, uploaderNamesById.get(row.uploadedBy())))
                .toList();
    }

    @Transactional
    public AttachmentSummary upload(AppUserPrincipal principal, UUID findingId, MultipartFile file) {
        requireFinding(principal, findingId);

        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("Choose a file to upload");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw ApiException.badRequest("Evidence files must be 10MB or smaller");
        }

        byte[] fileData;
        try {
            fileData = file.getBytes();
        } catch (java.io.IOException e) {
            throw ApiException.badRequest("Could not read the uploaded file");
        }

        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "evidence";
        String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";

        FindingAttachment attachment = FindingAttachment.create(principal.getOrganizationId(), findingId, fileName,
                contentType, fileData, principal.getUserId());
        attachmentRepository.save(attachment);

        auditLogService.record(principal, AuditAction.ATTACHMENT_UPLOADED, AuditEntityType.FINDING, findingId,
                "Uploaded evidence file " + fileName);

        User uploader = userRepository.findById(principal.getUserId()).orElse(null);
        String uploaderName = uploader != null ? uploader.getFirstName() + " " + uploader.getLastName() : null;
        return AttachmentSummary.from(
                new AttachmentRow(attachment.getId(), attachment.getFindingId(), attachment.getFileName(),
                        attachment.getContentType(), attachment.getFileSize(), attachment.getUploadedBy(),
                        attachment.getCreatedAt()),
                uploaderName);
    }

    public FindingAttachment download(AppUserPrincipal principal, UUID attachmentId) {
        return attachmentRepository.findByIdAndOrganizationId(attachmentId, principal.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("Attachment not found"));
    }

    @Transactional
    public void delete(AppUserPrincipal principal, UUID attachmentId) {
        FindingAttachment attachment = attachmentRepository
                .findByIdAndOrganizationId(attachmentId, principal.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("Attachment not found"));
        attachmentRepository.delete(attachment);
    }

    private Finding requireFinding(AppUserPrincipal principal, UUID findingId) {
        return findingRepository.findByIdAndOrganizationId(findingId, principal.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("Finding not found"));
    }
}
