package com.fris.begems.response;

import com.fris.begems.common.ApiException;
import com.fris.begems.director.Director;
import com.fris.begems.director.DirectorRepository;
import com.fris.begems.evaluation.Evaluation;
import com.fris.begems.evaluation.EvaluationRepository;
import com.fris.begems.evaluation.EvaluationRespondent;
import com.fris.begems.evaluation.EvaluationRespondentRepository;
import com.fris.begems.evaluation.EvaluationStatus;
import com.fris.begems.evaluation.RespondentStatus;
import com.fris.begems.framework.Question;
import com.fris.begems.framework.QuestionRepository;
import com.fris.begems.response.dto.MyEvaluationSummary;
import com.fris.begems.response.dto.QuestionWithAnswer;
import com.fris.begems.response.dto.SaveResponseRequest;
import com.fris.begems.security.AppUserPrincipal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResponseService {

    private final DirectorRepository directorRepository;
    private final EvaluationRespondentRepository respondentRepository;
    private final EvaluationRepository evaluationRepository;
    private final QuestionRepository questionRepository;
    private final ResponseRepository responseRepository;

    public ResponseService(DirectorRepository directorRepository,
            EvaluationRespondentRepository respondentRepository, EvaluationRepository evaluationRepository,
            QuestionRepository questionRepository, ResponseRepository responseRepository) {
        this.directorRepository = directorRepository;
        this.respondentRepository = respondentRepository;
        this.evaluationRepository = evaluationRepository;
        this.questionRepository = questionRepository;
        this.responseRepository = responseRepository;
    }

    public List<MyEvaluationSummary> listMyEvaluations(AppUserPrincipal principal) {
        Director director = requireMyDirector(principal);
        return respondentRepository.findByDirectorId(director.getId()).stream()
                .map(respondent -> {
                    Evaluation evaluation = evaluationRepository.findById(respondent.getEvaluationId())
                            .orElseThrow(() -> ApiException.notFound("Evaluation not found"));
                    List<Question> questions = questionRepository
                            .findByFrameworkIdAndEvaluationTypeOrderByDisplayOrder(
                                    evaluation.getFrameworkId(), evaluation.getEvaluationType());
                    int answered = responseRepository.findByEvaluationRespondentId(respondent.getId()).size();
                    String subjectName = evaluation.getSubjectDirectorId() == null ? null
                            : directorRepository.findById(evaluation.getSubjectDirectorId())
                                    .map(Director::getName).orElse(null);
                    return new MyEvaluationSummary(evaluation.getId(), evaluation.getEvaluationType(), subjectName,
                            evaluation.getYear(), evaluation.getStatus(), respondent.getStatus(), questions.size(),
                            answered);
                })
                .toList();
    }

    public List<QuestionWithAnswer> getQuestions(AppUserPrincipal principal, UUID evaluationId) {
        EvaluationRespondent respondent = requireMyRespondent(principal, evaluationId);
        Evaluation evaluation = requireEvaluation(evaluationId);
        List<Question> questions = questionRepository.findByFrameworkIdAndEvaluationTypeOrderByDisplayOrder(
                evaluation.getFrameworkId(), evaluation.getEvaluationType());
        Map<UUID, Response> answersByQuestion = responseRepository
                .findByEvaluationRespondentId(respondent.getId()).stream()
                .collect(Collectors.toMap(Response::getQuestionId, r -> r));
        return questions.stream()
                .map(question -> QuestionWithAnswer.from(question, answersByQuestion.get(question.getId())))
                .toList();
    }

    @Transactional
    public QuestionWithAnswer saveResponse(AppUserPrincipal principal, UUID evaluationId, UUID questionId,
            SaveResponseRequest request) {
        EvaluationRespondent respondent = requireMyRespondent(principal, evaluationId);
        requireOpenForResponses(evaluationId, respondent);

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> ApiException.notFound("Question not found"));
        validateAnswerShape(question, request);

        Response response = responseRepository
                .findByEvaluationRespondentIdAndQuestionId(respondent.getId(), questionId)
                .orElseGet(() -> Response.create(respondent.getId(), questionId));
        response.setRatingValue(request.ratingValue());
        response.setTextValue(request.textValue());
        response.setNumericValue(request.numericValue());
        response.setUpdatedAt(Instant.now());
        responseRepository.save(response);

        if (respondent.getStatus() == RespondentStatus.INVITED) {
            respondent.setStatus(RespondentStatus.IN_PROGRESS);
            respondentRepository.save(respondent);
        }

        return QuestionWithAnswer.from(question, response);
    }

    @Transactional
    public void submit(AppUserPrincipal principal, UUID evaluationId) {
        EvaluationRespondent respondent = requireMyRespondent(principal, evaluationId);
        requireOpenForResponses(evaluationId, respondent);

        respondent.setStatus(RespondentStatus.SUBMITTED);
        respondent.setSubmittedAt(Instant.now());
        respondentRepository.save(respondent);
    }

    private void validateAnswerShape(Question question, SaveResponseRequest request) {
        boolean valid = switch (question.getResponseType()) {
            case RATING_1_5, YES_NO, YES_NO_PARTIALLY -> request.ratingValue() != null
                    && request.ratingValue() >= 1 && request.ratingValue() <= 5;
            case NARRATIVE -> request.textValue() != null && !request.textValue().isBlank();
            case PERCENTAGE, NUMERIC -> request.numericValue() != null;
        };
        if (!valid) {
            throw ApiException.badRequest("The answer doesn't match this question's expected response type ("
                    + question.getResponseType() + ")");
        }
    }

    private void requireOpenForResponses(UUID evaluationId, EvaluationRespondent respondent) {
        Evaluation evaluation = requireEvaluation(evaluationId);
        if (evaluation.getStatus() != EvaluationStatus.LAUNCHED) {
            throw ApiException.conflict("This evaluation is not currently open for responses");
        }
        if (respondent.getStatus() == RespondentStatus.SUBMITTED) {
            throw ApiException.conflict("You've already submitted this evaluation");
        }
    }

    private Evaluation requireEvaluation(UUID evaluationId) {
        return evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> ApiException.notFound("Evaluation not found"));
    }

    private EvaluationRespondent requireMyRespondent(AppUserPrincipal principal, UUID evaluationId) {
        Director director = requireMyDirector(principal);
        return respondentRepository.findByEvaluationIdAndDirectorId(evaluationId, director.getId())
                .orElseThrow(() -> ApiException.notFound("You are not a respondent on this evaluation"));
    }

    private Director requireMyDirector(AppUserPrincipal principal) {
        return directorRepository.findByUserId(principal.getUserId())
                .orElseThrow(() -> ApiException.notFound("Your account isn't linked to a director profile"));
    }
}
