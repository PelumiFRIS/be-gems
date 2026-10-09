package com.fris.begems.approval;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** The Final Report exactly as approved, served instead of regenerating it. */
@Entity
@Table(name = "final_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FinalReport {

    @Id
    @Column(name = "evaluation_id")
    private UUID evaluationId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(nullable = false, columnDefinition = "text")
    private String html;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    public static FinalReport create(UUID evaluationId, UUID organizationId, String fileName, String html) {
        return new FinalReport(evaluationId, organizationId, fileName, html, Instant.now());
    }
}
