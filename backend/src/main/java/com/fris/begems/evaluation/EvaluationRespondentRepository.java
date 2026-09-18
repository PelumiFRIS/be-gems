package com.fris.begems.evaluation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvaluationRespondentRepository extends JpaRepository<EvaluationRespondent, UUID> {

    List<EvaluationRespondent> findByEvaluationId(UUID evaluationId);

    List<EvaluationRespondent> findByDirectorId(UUID directorId);

    Optional<EvaluationRespondent> findByEvaluationIdAndDirectorId(UUID evaluationId, UUID directorId);

    boolean existsByEvaluationIdAndDirectorId(UUID evaluationId, UUID directorId);
}
