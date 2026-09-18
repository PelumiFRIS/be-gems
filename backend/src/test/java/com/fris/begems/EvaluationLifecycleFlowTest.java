package com.fris.begems;

import static org.assertj.core.api.Assertions.assertThat;

import com.fris.begems.auth.dto.AuthResponse;
import com.fris.begems.board.dto.BoardSummary;
import com.fris.begems.director.dto.DirectorSummary;
import com.fris.begems.evaluation.ConfidentialityMode;
import com.fris.begems.evaluation.dto.AddRespondentRequest;
import com.fris.begems.evaluation.dto.CreateEvaluationRequest;
import com.fris.begems.evaluation.dto.EvaluationDetail;
import com.fris.begems.evaluation.dto.EvaluationSummary;
import com.fris.begems.framework.EvaluationType;
import com.fris.begems.response.dto.MyEvaluationSummary;
import com.fris.begems.response.dto.QuestionWithAnswer;
import com.fris.begems.response.dto.SaveResponseRequest;
import com.fris.begems.support.IntegrationTestSupport;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class EvaluationLifecycleFlowTest extends IntegrationTestSupport {

    @Test
    void fullLifecycleFromCreationThroughRespondentSubmissionAndClose() {
        AuthResponse admin = signup(uniqueEmail(), "Lifecycle Org");
        AuthResponse cs = createCompanySecretaryAndLogin(admin.accessToken());
        BoardSummary board = createBoard(admin.accessToken(), "Board of Directors");
        DirectorSummary directorA = createDirector(admin.accessToken(), board.id(), "Ada Lovelace");
        DirectorSummary directorB = createDirector(admin.accessToken(), board.id(), "Grace Hopper");
        AuthResponse loginA = inviteDirectorAndLogin(cs.accessToken(), directorA);
        AuthResponse loginB = inviteDirectorAndLogin(cs.accessToken(), directorB);

        ResponseEntity<EvaluationSummary> created = restTemplate.exchange(
                "/api/evaluations", HttpMethod.POST,
                authedRequest(cs.accessToken(), new CreateEvaluationRequest(board.id(), EvaluationType.BOARD, null, 2026)),
                EvaluationSummary.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID evaluationId = created.getBody().id();
        assertThat(created.getBody().status().toString()).isEqualTo("DRAFT");

        ResponseEntity<String> launchTooEarly = restTemplate.exchange(
                "/api/evaluations/" + evaluationId + "/launch", HttpMethod.POST, authedRequest(cs.accessToken()),
                String.class);
        assertThat(launchTooEarly.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        addRespondent(cs.accessToken(), evaluationId, directorA.id());
        addRespondent(cs.accessToken(), evaluationId, directorB.id());

        ResponseEntity<EvaluationSummary> launched = restTemplate.exchange(
                "/api/evaluations/" + evaluationId + "/launch", HttpMethod.POST, authedRequest(cs.accessToken()),
                EvaluationSummary.class);
        assertThat(launched.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(launched.getBody().status().toString()).isEqualTo("LAUNCHED");

        ResponseEntity<MyEvaluationSummary[]> myEvaluations = restTemplate.exchange(
                "/api/my-evaluations", HttpMethod.GET, authedRequest(loginA.accessToken()),
                MyEvaluationSummary[].class);
        assertThat(myEvaluations.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(myEvaluations.getBody()).hasSize(1);
        assertThat(myEvaluations.getBody()[0].totalQuestions()).isEqualTo(58);
        assertThat(myEvaluations.getBody()[0].answeredQuestions()).isEqualTo(0);

        ResponseEntity<QuestionWithAnswer[]> questions = restTemplate.exchange(
                "/api/my-evaluations/" + evaluationId + "/questions", HttpMethod.GET,
                authedRequest(loginA.accessToken()), QuestionWithAnswer[].class);
        assertThat(questions.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(questions.getBody()).hasSize(58);
        UUID firstQuestionId = questions.getBody()[0].questionId();

        ResponseEntity<QuestionWithAnswer> saved = restTemplate.exchange(
                "/api/my-evaluations/" + evaluationId + "/responses/" + firstQuestionId, HttpMethod.PUT,
                authedRequest(loginA.accessToken(), new SaveResponseRequest(4, null, null)), QuestionWithAnswer.class);
        assertThat(saved.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(saved.getBody().ratingValue()).isEqualTo(4);

        ResponseEntity<QuestionWithAnswer[]> resumed = restTemplate.exchange(
                "/api/my-evaluations/" + evaluationId + "/questions", HttpMethod.GET,
                authedRequest(loginA.accessToken()), QuestionWithAnswer[].class);
        assertThat(resumed.getBody()[0].ratingValue()).isEqualTo(4);

        ResponseEntity<Void> submitA = restTemplate.exchange(
                "/api/my-evaluations/" + evaluationId + "/submit", HttpMethod.POST, authedRequest(loginA.accessToken()),
                Void.class);
        assertThat(submitA.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> saveAfterSubmit = restTemplate.exchange(
                "/api/my-evaluations/" + evaluationId + "/responses/" + firstQuestionId, HttpMethod.PUT,
                authedRequest(loginA.accessToken(), new SaveResponseRequest(2, null, null)), String.class);
        assertThat(saveAfterSubmit.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        // Director B never answers or submits — closing should auto-submit them.
        ResponseEntity<EvaluationSummary> closed = restTemplate.exchange(
                "/api/evaluations/" + evaluationId + "/close", HttpMethod.POST, authedRequest(cs.accessToken()),
                EvaluationSummary.class);
        assertThat(closed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(closed.getBody().status().toString()).isEqualTo("CLOSED");

        ResponseEntity<EvaluationDetail> detail = restTemplate.exchange(
                "/api/evaluations/" + evaluationId, HttpMethod.GET, authedRequest(cs.accessToken()),
                EvaluationDetail.class);
        assertThat(detail.getBody().respondents()).allSatisfy(r -> assertThat(r.status().toString()).isEqualTo("SUBMITTED"));

        ResponseEntity<String> saveAfterClose = restTemplate.exchange(
                "/api/my-evaluations/" + evaluationId + "/responses/" + firstQuestionId, HttpMethod.PUT,
                authedRequest(loginB.accessToken(), new SaveResponseRequest(3, null, null)), String.class);
        assertThat(saveAfterClose.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    private void addRespondent(String csToken, UUID evaluationId, UUID directorId) {
        ResponseEntity<EvaluationDetail> response = restTemplate.exchange(
                "/api/evaluations/" + evaluationId + "/respondents", HttpMethod.POST,
                authedRequest(csToken, new AddRespondentRequest(directorId, ConfidentialityMode.CONFIDENTIAL)),
                EvaluationDetail.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
