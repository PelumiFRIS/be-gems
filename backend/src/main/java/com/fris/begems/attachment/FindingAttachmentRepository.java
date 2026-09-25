package com.fris.begems.attachment;

import com.fris.begems.attachment.dto.AttachmentRow;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FindingAttachmentRepository extends JpaRepository<FindingAttachment, UUID> {

    @Query("select new com.fris.begems.attachment.dto.AttachmentRow(a.id, a.findingId, a.fileName, a.contentType, "
            + "a.fileSize, a.uploadedBy, a.createdAt) from FindingAttachment a where a.findingId = :findingId "
            + "order by a.createdAt desc")
    List<AttachmentRow> findRowsByFindingId(@Param("findingId") UUID findingId);

    Optional<FindingAttachment> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
