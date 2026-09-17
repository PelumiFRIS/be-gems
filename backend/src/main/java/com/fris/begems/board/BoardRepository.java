package com.fris.begems.board;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardRepository extends JpaRepository<Board, UUID> {

    List<Board> findByOrganizationId(UUID organizationId);

    Optional<Board> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
