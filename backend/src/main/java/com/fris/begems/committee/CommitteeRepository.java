package com.fris.begems.committee;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommitteeRepository extends JpaRepository<Committee, UUID> {

    List<Committee> findByBoardId(UUID boardId);

    Optional<Committee> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
