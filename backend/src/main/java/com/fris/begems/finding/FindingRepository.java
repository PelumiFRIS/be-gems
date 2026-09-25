package com.fris.begems.finding;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FindingRepository extends JpaRepository<Finding, UUID> {

    List<Finding> findByEvaluationId(UUID evaluationId);

    Optional<Finding> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
