package com.fris.begems.director;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DirectorRepository extends JpaRepository<Director, UUID> {

    List<Director> findByBoardId(UUID boardId);

    Optional<Director> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
