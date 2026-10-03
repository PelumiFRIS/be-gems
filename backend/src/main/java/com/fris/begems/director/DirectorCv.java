package com.fris.begems.director;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One CV per director, keyed by the director's id; uploading again replaces it. */
@Entity
@Table(name = "director_cvs")
@Getter
@Setter
@NoArgsConstructor
public class DirectorCv {

    @Id
    @Column(name = "director_id")
    private UUID directorId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

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

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    public void replaceFile(String fileName, String contentType, byte[] fileData, UUID uploadedBy) {
        setFileName(fileName);
        setContentType(contentType);
        setFileSize(fileData.length);
        setFileData(fileData);
        setUploadedBy(uploadedBy);
        setUploadedAt(Instant.now());
    }
}
