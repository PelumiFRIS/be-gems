package com.fris.begems.framework;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<Question, UUID> {

    List<Question> findByFrameworkIdAndEvaluationTypeOrderByDisplayOrder(UUID frameworkId,
            EvaluationType evaluationType);
}
