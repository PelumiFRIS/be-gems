package com.fris.begems.scoring;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvaluationScoreRepository extends JpaRepository<EvaluationScore, UUID> {

    List<EvaluationScore> findByEvaluationId(UUID evaluationId);

    void deleteByEvaluationId(UUID evaluationId);
}
