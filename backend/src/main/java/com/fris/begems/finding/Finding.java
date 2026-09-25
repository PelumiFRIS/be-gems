package com.fris.begems.finding;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "findings")
@Getter
@Setter
@NoArgsConstructor
public class Finding {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "evaluation_id", nullable = false)
    private UUID evaluationId;

    @Column(name = "dimension_id")
    private UUID dimensionId;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FindingSeverity severity;

    private String evidence;

    @Column(name = "regulatory_reference")
    private String regulatoryReference;

    @Column(name = "root_cause")
    private String rootCause;

    @Column(name = "risk_implication")
    private String riskImplication;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static Finding create(UUID organizationId, UUID evaluationId, UUID dimensionId, String description,
            FindingSeverity severity, String evidence, String regulatoryReference, String rootCause,
            String riskImplication) {
        Finding finding = new Finding();
        finding.setId(UUID.randomUUID());
        finding.setOrganizationId(organizationId);
        finding.setEvaluationId(evaluationId);
        finding.setDimensionId(dimensionId);
        finding.setDescription(description);
        finding.setSeverity(severity);
        finding.setEvidence(evidence);
        finding.setRegulatoryReference(regulatoryReference);
        finding.setRootCause(rootCause);
        finding.setRiskImplication(riskImplication);
        finding.setCreatedAt(Instant.now());
        return finding;
    }
}
