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
import com.fris.begems.framework.dto.QuestionSummary;
import com.fris.begems.response.dto.SaveResponseRequest;
import com.fris.begems.scoring.dto.ScoreRowSummary;
import com.fris.begems.support.IntegrationTestSupport;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Two respondents answer every Board question — one rates everything 5, the
 * other rates everything 3 — so every dimension score averages to exactly 4.00,
 * making the weighted board score, maturity level, and BGEI classification all
 * hand-calculable: weights sum to 100%, so a uniform 4.00 across every dimension
 * yields a board overall of 4.00 (Managed, 3.70-4.49) and a BGEI of 80.00%
 * (Highly Effective, 80.00-89.99%).
 */
class ScoringEngineFlowTest extends IntegrationTestSupport {

    @Test
    void computesDimensionBoardMaturityAndBgeiScoresFromRespondentAnswers() {
        AuthResponse admin = signup(uniqueEmail(), "Scoring Org");
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
        UUID evaluationId = created.getBody().id();

        addRespondent(cs.accessToken(), evaluationId, directorA.id());
        addRespondent(cs.accessToken(), evaluationId, directorB.id());
        restTemplate.exchange("/api/evaluations/" + evaluationId + "/launch", HttpMethod.POST,
                authedRequest(cs.accessToken()), EvaluationSummary.class);

        ResponseEntity<QuestionSummary[]> questions = restTemplate.exchange(
                "/api/frameworks/active/questions?evaluationType=BOARD", HttpMethod.GET,
                authedRequest(cs.accessToken()), QuestionSummary[].class);
        assertThat(questions.getBody()).hasSize(58);

        for (QuestionSummary question : questions.getBody()) {
            answer(loginA.accessToken(), evaluationId, question.id(), 5);
            answer(loginB.accessToken(), evaluationId, question.id(), 3);
        }
        submit(loginA.accessToken(), evaluationId);
        submit(loginB.accessToken(), evaluationId);

        restTemplate.exchange("/api/evaluations/" + evaluationId + "/close", HttpMethod.POST,
                authedRequest(cs.accessToken()), EvaluationSummary.class);

        ResponseEntity<ScoreRowSummary[]> scoresResponse = restTemplate.exchange(
                "/api/evaluations/" + evaluationId + "/scores/calculate", HttpMethod.POST,
                authedRequest(cs.accessToken()), ScoreRowSummary[].class);
        assertThat(scoresResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        ScoreRowSummary[] scores = scoresResponse.getBody();

        assertThat(scores).filteredOn(s -> s.scopeType().toString().equals("DIMENSION"))
                .hasSize(18)
                .allSatisfy(s -> assertThat(s.rawScore()).isEqualByComparingTo(new BigDecimal("4.00")));

        ScoreRowSummary boardOverall = findOne(scores, "BOARD_OVERALL");
        assertThat(boardOverall.rawScore()).isEqualByComparingTo(new BigDecimal("4.00"));
        assertThat(boardOverall.maturityLevel()).isEqualTo(4);
        assertThat(boardOverall.maturityLabel()).isEqualTo("Managed");

        ScoreRowSummary bgeiOverall = findOne(scores, "BGEI_OVERALL");
        assertThat(bgeiOverall.weightedScore()).isEqualByComparingTo(new BigDecimal("80.00"));
        assertThat(bgeiOverall.bgeiBandLabel()).isEqualTo("Highly Effective");

        // Re-fetching without recalculating returns the same persisted rows.
        ResponseEntity<ScoreRowSummary[]> reread = restTemplate.exchange(
                "/api/evaluations/" + evaluationId + "/scores", HttpMethod.GET, authedRequest(admin.accessToken()),
                ScoreRowSummary[].class);
        assertThat(reread.getBody()).hasSize(scores.length);
    }

    private ScoreRowSummary findOne(ScoreRowSummary[] scores, String scopeType) {
        return java.util.Arrays.stream(scores)
                .filter(s -> s.scopeType().toString().equals(scopeType))
                .findFirst()
                .orElseThrow();
    }

    private void answer(String token, UUID evaluationId, UUID questionId, int rating) {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/my-evaluations/" + evaluationId + "/responses/" + questionId, HttpMethod.PUT,
                authedRequest(token, new SaveResponseRequest(rating, null, null)), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private void submit(String token, UUID evaluationId) {
        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/my-evaluations/" + evaluationId + "/submit", HttpMethod.POST, authedRequest(token), Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private void addRespondent(String adminToken, UUID evaluationId, UUID directorId) {
        ResponseEntity<EvaluationDetail> response = restTemplate.exchange(
                "/api/evaluations/" + evaluationId + "/respondents", HttpMethod.POST,
                authedRequest(adminToken, new AddRespondentRequest(directorId, ConfidentialityMode.CONFIDENTIAL)),
                EvaluationDetail.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
