package com.fris.begems.skill;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardSkillRepository extends JpaRepository<BoardSkill, UUID> {

    List<BoardSkill> findByBoardIdOrderByDisplayOrderAscNameAsc(UUID boardId);

    Optional<BoardSkill> findByIdAndOrganizationId(UUID id, UUID organizationId);

    boolean existsByBoardIdAndNameIgnoreCase(UUID boardId, String name);

    boolean existsByBoardIdAndNameIgnoreCaseAndIdNot(UUID boardId, String name, UUID id);
}
