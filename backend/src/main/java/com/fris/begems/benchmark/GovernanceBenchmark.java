package com.fris.begems.benchmark;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** An organisation's own benchmark for one dimension, or for the BGEI when {@code dimensionId} is null. */
@Entity
@Table(name = "governance_benchmarks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GovernanceBenchmark {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "dimension_id")
    private UUID dimensionId;

    @Column(nullable = false)
    private BigDecimal target;

    private String source;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static GovernanceBenchmark create(UUID organizationId, UUID dimensionId) {
        GovernanceBenchmark benchmark = new GovernanceBenchmark();
        benchmark.setId(UUID.randomUUID());
        benchmark.setOrganizationId(organizationId);
        benchmark.setDimensionId(dimensionId);
        return benchmark;
    }
}
