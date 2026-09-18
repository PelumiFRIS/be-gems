package com.fris.begems;

import static org.assertj.core.api.Assertions.assertThat;

import com.fris.begems.auth.dto.AuthResponse;
import com.fris.begems.board.dto.BoardSummary;
import com.fris.begems.director.dto.DirectorSummary;
import com.fris.begems.evaluation.dto.AddRespondentRequest;
import com.fris.begems.evaluation.ConfidentialityMode;
import com.fris.begems.evaluation.dto.CreateEvaluationRequest;
import com.fris.begems.evaluation.dto.EvaluationDetail;
import com.fris.begems.evaluation.dto.EvaluationSummary;
import com.fris.begems.framework.EvaluationType;
import com.fris.begems.support.IntegrationTestSupport;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * There's no endpoint that takes "whose answers to view" as a parameter — every
 * respondent-portal endpoint resolves "my director" from the caller's own JWT
 * (see ResponseService.requireMyDirector), so there's no ID to substitute to view
 * someone else's answers. What's worth testing directly: a director who isn't a
 * respondent on an evaluation can't reach its questions at all, a plain DIRECTOR
 * can't manage the evaluation lifecycle, and organizations stay isolated.
 */
class EvaluationAccessControlFlowTest extends IntegrationTestSupport {

    @Test
    void aDirectorNotOnTheEvaluationCannotReachItsQuestions() {
        AuthResponse admin = signup(uniqueEmail(), "Access Control Org");
        AuthResponse cs = createCompanySecretaryAndLogin(admin.accessToken());
        BoardSummary board = createBoard(admin.accessToken(), "Board of Directors");
        DirectorSummary respondent = createDirector(admin.accessToken(), board.id(), "Ada Lovelace");
        DirectorSummary outsider = createDirector(admin.accessToken(), board.id(), "Not Invited");
        AuthResponse outsiderLogin = inviteDirectorAndLogin(cs.accessToken(), outsider);

        UUID evaluationId = createLaunchedEvaluation(cs.accessToken(), board.id(), respondent.id());

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/my-evaluations/" + evaluationId + "/questions", HttpMethod.GET,
                authedRequest(outsiderLogin.accessToken()), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void aPlainDirectorCannotManageTheEvaluationLifecycle() {
        AuthResponse admin = signup(uniqueEmail(), "Access Control Org 2");
        AuthResponse cs = createCompanySecretaryAndLogin(admin.accessToken());
        BoardSummary board = createBoard(admin.accessToken(), "Board of Directors");
        DirectorSummary director = createDirector(admin.accessToken(), board.id(), "Ada Lovelace");
        AuthResponse directorLogin = inviteDirectorAndLogin(cs.accessToken(), director);

        ResponseEntity<String> createAttempt = restTemplate.exchange(
                "/api/evaluations", HttpMethod.POST,
                authedRequest(directorLogin.accessToken(),
                        new CreateEvaluationRequest(board.id(), EvaluationType.BOARD, null, 2026)),
                String.class);
        assertThat(createAttempt.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void organizationsStayIsolated() {
        AuthResponse adminA = signup(uniqueEmail(), "Org A");
        AuthResponse csA = createCompanySecretaryAndLogin(adminA.accessToken());
        BoardSummary boardA = createBoard(adminA.accessToken(), "Board A");
        DirectorSummary directorA = createDirector(adminA.accessToken(), boardA.id(), "Ada Lovelace");
        UUID evaluationId = createLaunchedEvaluation(csA.accessToken(), boardA.id(), directorA.id());

        AuthResponse adminB = signup(uniqueEmail(), "Org B");

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/evaluations/" + evaluationId, HttpMethod.GET, authedRequest(adminB.accessToken()),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private UUID createLaunchedEvaluation(String csToken, UUID boardId, UUID respondentDirectorId) {
        ResponseEntity<EvaluationSummary> created = restTemplate.exchange(
                "/api/evaluations", HttpMethod.POST,
                authedRequest(csToken, new CreateEvaluationRequest(boardId, EvaluationType.BOARD, null, 2026)),
                EvaluationSummary.class);
        UUID evaluationId = created.getBody().id();

        restTemplate.exchange(
                "/api/evaluations/" + evaluationId + "/respondents", HttpMethod.POST,
                authedRequest(csToken, new AddRespondentRequest(respondentDirectorId, ConfidentialityMode.CONFIDENTIAL)),
                EvaluationDetail.class);
        restTemplate.exchange("/api/evaluations/" + evaluationId + "/launch", HttpMethod.POST,
                authedRequest(csToken), EvaluationSummary.class);
        return evaluationId;
    }
}
