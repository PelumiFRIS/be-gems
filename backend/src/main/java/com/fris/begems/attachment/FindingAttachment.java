package com.fris.begems.attachment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "finding_attachments")
@Getter
@Setter
@NoArgsConstructor
public class FindingAttachment {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "finding_id", nullable = false)
    private UUID findingId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "file_data", nullable = false)
    private byte[] fileData;

    @Column(name = "uploaded_by", nullable = false)
    private UUID uploadedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static FindingAttachment create(UUID organizationId, UUID findingId, String fileName, String contentType,
            byte[] fileData, UUID uploadedBy) {
        FindingAttachment attachment = new FindingAttachment();
        attachment.setId(UUID.randomUUID());
        attachment.setOrganizationId(organizationId);
        attachment.setFindingId(findingId);
        attachment.setFileName(fileName);
        attachment.setContentType(contentType);
        attachment.setFileSize(fileData.length);
        attachment.setFileData(fileData);
        attachment.setUploadedBy(uploadedBy);
        attachment.setCreatedAt(Instant.now());
        return attachment;
    }
}
