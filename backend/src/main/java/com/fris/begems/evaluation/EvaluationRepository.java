package com.fris.begems.evaluation;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EvaluationRepository extends JpaRepository<Evaluation, UUID> {

    List<Evaluation> findByBoardId(UUID boardId);

    Optional<Evaluation> findByIdAndOrganizationId(UUID id, UUID organizationId);

    boolean existsBySubjectDirectorId(UUID subjectDirectorId);

    /** Serialises approval decisions so two people can't both move the same report on from one stage. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Evaluation e where e.id = :id and e.organizationId = :organizationId")
    Optional<Evaluation> lockByIdAndOrganizationId(@Param("id") UUID id,
            @Param("organizationId") UUID organizationId);
}
