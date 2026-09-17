package com.fris.begems.framework;

import com.fris.begems.common.ApiException;
import com.fris.begems.framework.dto.DimensionSummary;
import com.fris.begems.framework.dto.FrameworkDetail;
import com.fris.begems.framework.dto.QuestionSummary;
import java.util.List;
import org.springframework.stereotype.Service;

/** Read-only for MVP — frameworks/dimensions/questions are seeded via Flyway, not created through the API yet. */
@Service
public class FrameworkService {

    private static final String ACTIVE_FRAMEWORK_CODE = "NCCG_2018";

    private final FrameworkRepository frameworkRepository;
    private final DimensionRepository dimensionRepository;
    private final QuestionRepository questionRepository;

    public FrameworkService(FrameworkRepository frameworkRepository, DimensionRepository dimensionRepository,
            QuestionRepository questionRepository) {
        this.frameworkRepository = frameworkRepository;
        this.dimensionRepository = dimensionRepository;
        this.questionRepository = questionRepository;
    }

    public FrameworkDetail getActiveFramework() {
        Framework framework = frameworkRepository.findByCodeAndActiveTrue(ACTIVE_FRAMEWORK_CODE)
                .orElseThrow(() -> ApiException.notFound("No active governance framework is configured"));
        List<DimensionSummary> dimensions = dimensionRepository.findByFrameworkIdOrderByDisplayOrder(framework.getId())
                .stream()
                .map(DimensionSummary::from)
                .toList();
        return FrameworkDetail.from(framework, dimensions);
    }

    public List<QuestionSummary> getQuestions(EvaluationType evaluationType) {
        Framework framework = frameworkRepository.findByCodeAndActiveTrue(ACTIVE_FRAMEWORK_CODE)
                .orElseThrow(() -> ApiException.notFound("No active governance framework is configured"));
        return questionRepository
                .findByFrameworkIdAndEvaluationTypeOrderByDisplayOrder(framework.getId(), evaluationType).stream()
                .map(QuestionSummary::from)
                .toList();
    }
}
