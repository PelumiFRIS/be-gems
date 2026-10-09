package com.fris.begems.benchmark;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GovernanceBenchmarkRepository extends JpaRepository<GovernanceBenchmark, UUID> {

    List<GovernanceBenchmark> findByOrganizationId(UUID organizationId);

    Optional<GovernanceBenchmark> findByOrganizationIdAndDimensionId(UUID organizationId, UUID dimensionId);

    Optional<GovernanceBenchmark> findByOrganizationIdAndDimensionIdIsNull(UUID organizationId);
}
