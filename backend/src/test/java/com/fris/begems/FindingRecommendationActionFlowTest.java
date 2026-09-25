package com.fris.begems;

import static org.assertj.core.api.Assertions.assertThat;

import com.fris.begems.action.ActionStatus;
import com.fris.begems.action.dto.ActionRegisterRow;
import com.fris.begems.action.dto.ActionSummary;
import com.fris.begems.action.dto.CreateActionRequest;
import com.fris.begems.action.dto.UpdateActionRequest;
import com.fris.begems.auth.dto.AuthResponse;
import com.fris.begems.board.dto.BoardSummary;
import com.fris.begems.director.dto.DirectorSummary;
import com.fris.begems.evaluation.ConfidentialityMode;
import com.fris.begems.evaluation.dto.AddRespondentRequest;
import com.fris.begems.evaluation.dto.CreateEvaluationRequest;
import com.fris.begems.evaluation.dto.EvaluationDetail;
import com.fris.begems.evaluation.dto.EvaluationSummary;
import com.fris.begems.finding.FindingSeverity;
import com.fris.begems.finding.dto.CreateFindingRequest;
import com.fris.begems.finding.dto.FindingSummary;
import com.fris.begems.framework.EvaluationType;
import com.fris.begems.framework.dto.QuestionSummary;
import com.fris.begems.recommendation.RecommendationPriority;
import com.fris.begems.recommendation.RecommendationStatus;
import com.fris.begems.recommendation.dto.CreateRecommendationRequest;
import com.fris.begems.recommendation.dto.RecommendationSummary;
import com.fris.begems.response.dto.SaveResponseRequest;
import com.fris.begems.support.IntegrationTestSupport;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Drives a Board evaluation to SCORED, then walks the Finding -> Recommendation
 * -> Action chain end to end, including the org-wide action register and its
 * read-time overdue flagging.
 */
class FindingRecommendationActionFlowTest extends IntegrationTestSupport {

    @Test
    void findingsRecommendationsAndActionsFlowFromAScoredEvaluation() {
        AuthResponse admin = signup(uniqueEmail(), "Findings Org");
        AuthResponse cs = createCompanySecretaryAndLogin(admin.accessToken());
        BoardSummary board = createBoard(admin.accessToken(), "Board of Directors");
        DirectorSummary director = createDirector(admin.accessToken(), board.id(), "Ada Lovelace");
        AuthResponse directorLogin = inviteDirectorAndLogin(cs.accessToken(), director);

        UUID evaluationId = scoreABoardEvaluation(cs, board, director, directorLogin);

        // A finding requires a SCORED evaluation.
        ResponseEntity<FindingSummary> findingResponse = restTemplate.exchange(
                "/api/evaluations/" + evaluationId + "/findings", HttpMethod.POST,
                authedRequest(cs.accessToken(), new CreateFindingRequest(null,
                        "Board packs are routinely circulated fewer than 48 hours before meetings",
                        FindingSeverity.HIGH, "Minutes for Q1-Q3 2026", "NCCG 2018, Principle 14", null, null)),
                FindingSummary.class);
        assertThat(findingResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID findingId = findingResponse.getBody().id();
        assertThat(findingResponse.getBody().severity()).isEqualTo(FindingSeverity.HIGH);

        ResponseEntity<FindingSummary[]> findingsList = restTemplate.exchange(
                "/api/evaluations/" + evaluationId + "/findings", HttpMethod.GET, authedRequest(admin.accessToken()),
                FindingSummary[].class);
        assertThat(findingsList.getBody()).hasSize(1);

        ResponseEntity<RecommendationSummary> recommendationResponse = restTemplate.exchange(
                "/api/findings/" + findingId + "/recommendations", HttpMethod.POST,
                authedRequest(cs.accessToken(), new CreateRecommendationRequest(
                        "Adopt and enforce a 48-hour minimum for board pack circulation", "Company Secretary", null,
                        LocalDate.now().plusMonths(1), RecommendationPriority.HIGH, RecommendationStatus.OPEN)),
                RecommendationSummary.class);
        assertThat(recommendationResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(recommendationResponse.getBody().findingId()).isEqualTo(findingId);

        ResponseEntity<ActionSummary> actionResponse = restTemplate.exchange(
                "/api/findings/" + findingId + "/actions", HttpMethod.POST,
                authedRequest(cs.accessToken(), new CreateActionRequest("Update the board pack SOP and circulate it",
                        "Company Secretary", "Board Chair", LocalDate.now().minusDays(2), null)),
                ActionSummary.class);
        assertThat(actionResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID actionId = actionResponse.getBody().id();
        assertThat(actionResponse.getBody().status()).isEqualTo(ActionStatus.NOT_STARTED);

        // Overdue: a past due date on a not-started action.
        ResponseEntity<ActionRegisterRow[]> registerBeforeClosure = restTemplate.exchange(
                "/api/actions", HttpMethod.GET, authedRequest(admin.accessToken()), ActionRegisterRow[].class);
        ActionRegisterRow registerRow = Arrays.stream(registerBeforeClosure.getBody())
                .filter(r -> r.id().equals(actionId)).findFirst().orElseThrow();
        assertThat(registerRow.overdue()).isTrue();
        assertThat(registerRow.evaluationId()).isEqualTo(evaluationId);
        assertThat(registerRow.findingSeverity()).isEqualTo(FindingSeverity.HIGH);

        // Closing it (status -> COMPLETED) auto-sets closureDate and clears overdue.
        ResponseEntity<ActionSummary> closed = restTemplate.exchange(
                "/api/actions/" + actionId, HttpMethod.PUT,
                authedRequest(cs.accessToken(), new UpdateActionRequest("Update the board pack SOP and circulate it",
                        "Company Secretary", "Board Chair", LocalDate.now().minusDays(2), ActionStatus.COMPLETED,
                        "SOP updated and circulated 2026-09-24")),
                ActionSummary.class);
        assertThat(closed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(closed.getBody().status()).isEqualTo(ActionStatus.COMPLETED);
        assertThat(closed.getBody().closureDate()).isEqualTo(LocalDate.now());
        assertThat(closed.getBody().overdue()).isFalse();

        ResponseEntity<ActionRegisterRow[]> registerAfterClosure = restTemplate.exchange(
                "/api/actions", HttpMethod.GET, authedRequest(admin.accessToken()), ActionRegisterRow[].class);
        ActionRegisterRow closedRow = Arrays.stream(registerAfterClosure.getBody())
                .filter(r -> r.id().equals(actionId)).findFirst().orElseThrow();
        assertThat(closedRow.overdue()).isFalse();

        // A plain director cannot record findings.
        ResponseEntity<String> directorAttempt = restTemplate.exchange(
                "/api/evaluations/" + evaluationId + "/findings", HttpMethod.POST,
                authedRequest(directorLogin.accessToken(), new CreateFindingRequest(null, "Should be forbidden",
                        FindingSeverity.LOW, null, null, null, null)),
                String.class);
        assertThat(directorAttempt.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // A different organisation cannot see this finding.
        AuthResponse otherOrgAdmin = signup(uniqueEmail(), "Other Org");
        ResponseEntity<String> crossOrgAttempt = restTemplate.exchange(
                "/api/evaluations/" + evaluationId + "/findings", HttpMethod.GET,
                authedRequest(otherOrgAdmin.accessToken()), String.class);
        assertThat(crossOrgAttempt.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private UUID scoreABoardEvaluation(AuthResponse cs, BoardSummary board, DirectorSummary director,
            AuthResponse directorLogin) {
        ResponseEntity<EvaluationSummary> created = restTemplate.exchange(
                "/api/evaluations", HttpMethod.POST,
                authedRequest(cs.accessToken(), new CreateEvaluationRequest(board.id(), EvaluationType.BOARD, null, 2026)),
                EvaluationSummary.class);
        UUID evaluationId = created.getBody().id();

        ResponseEntity<EvaluationDetail> addRespondent = restTemplate.exchange(
                "/api/evaluations/" + evaluationId + "/respondents", HttpMethod.POST,
                authedRequest(cs.accessToken(), new AddRespondentRequest(director.id(), ConfidentialityMode.CONFIDENTIAL)),
                EvaluationDetail.class);
        assertThat(addRespondent.getStatusCode()).isEqualTo(HttpStatus.OK);

        restTemplate.exchange("/api/evaluations/" + evaluationId + "/launch", HttpMethod.POST,
                authedRequest(cs.accessToken()), EvaluationSummary.class);

        ResponseEntity<QuestionSummary[]> questions = restTemplate.exchange(
                "/api/frameworks/active/questions?evaluationType=BOARD", HttpMethod.GET,
                authedRequest(cs.accessToken()), QuestionSummary[].class);
        for (QuestionSummary question : questions.getBody()) {
            ResponseEntity<String> answer = restTemplate.exchange(
                    "/api/my-evaluations/" + evaluationId + "/responses/" + question.id(), HttpMethod.PUT,
                    authedRequest(directorLogin.accessToken(), new SaveResponseRequest(4, null, null)), String.class);
            assertThat(answer.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
        restTemplate.exchange("/api/my-evaluations/" + evaluationId + "/submit", HttpMethod.POST,
                authedRequest(directorLogin.accessToken()), Void.class);

        restTemplate.exchange("/api/evaluations/" + evaluationId + "/close", HttpMethod.POST,
                authedRequest(cs.accessToken()), EvaluationSummary.class);

        restTemplate.exchange("/api/evaluations/" + evaluationId + "/scores/calculate", HttpMethod.POST,
                authedRequest(cs.accessToken()), String.class);

        return evaluationId;
    }
}
