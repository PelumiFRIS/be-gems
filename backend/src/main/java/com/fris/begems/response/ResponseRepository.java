package com.fris.begems.response;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResponseRepository extends JpaRepository<Response, UUID> {

    List<Response> findByEvaluationRespondentId(UUID evaluationRespondentId);

    Optional<Response> findByEvaluationRespondentIdAndQuestionId(UUID evaluationRespondentId, UUID questionId);

    List<Response> findByEvaluationRespondentIdIn(List<UUID> evaluationRespondentIds);
}
