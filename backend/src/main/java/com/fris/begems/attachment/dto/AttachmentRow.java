package com.fris.begems.attachment.dto;

import java.time.Instant;
import java.util.UUID;

public record AttachmentRow(UUID id, UUID findingId, String fileName, String contentType, long fileSize,
        UUID uploadedBy, Instant createdAt) {
}
