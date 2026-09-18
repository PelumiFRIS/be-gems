package com.fris.begems.evaluation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvaluationRepository extends JpaRepository<Evaluation, UUID> {

    List<Evaluation> findByBoardId(UUID boardId);

    Optional<Evaluation> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
