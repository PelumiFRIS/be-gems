package com.fris.begems.action;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CorrectiveActionRepository extends JpaRepository<CorrectiveAction, UUID> {

    List<CorrectiveAction> findByFindingId(UUID findingId);

    List<CorrectiveAction> findByOrganizationId(UUID organizationId);

    Optional<CorrectiveAction> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
