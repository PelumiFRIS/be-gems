package com.fris.begems.attachment.dto;

import java.time.Instant;
import java.util.UUID;

public record AttachmentSummary(UUID id, UUID findingId, String fileName, String contentType, long fileSize,
        String uploadedByName, Instant createdAt) {

    public static AttachmentSummary from(AttachmentRow row, String uploadedByName) {
        return new AttachmentSummary(row.id(), row.findingId(), row.fileName(), row.contentType(), row.fileSize(),
                uploadedByName, row.createdAt());
    }
}
