package com.fris.begems;

import static org.assertj.core.api.Assertions.assertThat;

import com.fris.begems.auth.dto.AuthResponse;
import com.fris.begems.benchmark.dto.BenchmarkComparison;
import com.fris.begems.benchmark.dto.BenchmarkComparisonRow;
import com.fris.begems.benchmark.dto.BenchmarkRequest;
import com.fris.begems.benchmark.dto.BenchmarkSettings;
import com.fris.begems.benchmark.dto.BenchmarkTargetSummary;
import com.fris.begems.board.dto.BoardSummary;
import com.fris.begems.director.dto.DirectorSummary;
import com.fris.begems.evaluation.ConfidentialityMode;
import com.fris.begems.evaluation.dto.AddRespondentRequest;
import com.fris.begems.evaluation.dto.CreateEvaluationRequest;
import com.fris.begems.evaluation.dto.EvaluationSummary;
import com.fris.begems.framework.EvaluationType;
import com.fris.begems.framework.dto.QuestionSummary;
import com.fris.begems.response.dto.SaveResponseRequest;
import com.fris.begems.support.IntegrationTestSupport;
import com.fris.begems.user.Role;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class BenchmarkFlowTest extends IntegrationTestSupport {

    @Test
    void organisationTargetsReplaceTheDefaultsAndNegativeVariancesAreFlagged() {
        AuthResponse admin = signup(uniqueEmail(), "Benchmark Org");
        AuthResponse cs = createCompanySecretaryAndLogin(admin.accessToken());

        BenchmarkSettings defaults = settings(cs.accessToken());
        assertThat(defaults.dimensions()).hasSize(18).allSatisfy(d -> {
            assertThat(d.isDefault()).isTrue();
            assertThat(d.target()).isEqualByComparingTo("3.00");
            assertThat(d.source()).isEqualTo("BE-GEMS default: maturity Level 3 (Defined)");
        });
        assertThat(defaults.bgei().target()).isEqualByComparingTo("70.00");
        UUID strategy = dimension(defaults, "Strategy").dimensionId();

        BenchmarkSettings updated = put(cs.accessToken(), "/api/benchmarks/dimensions/" + strategy,
                new BenchmarkRequest(new BigDecimal("4.2"), "  CBN Code 2023, s.5 "));
        BenchmarkTargetSummary strategyTarget = dimension(updated, "Strategy");
        assertThat(strategyTarget.isDefault()).isFalse();
        assertThat(strategyTarget.target()).isEqualByComparingTo("4.20");
        assertThat(strategyTarget.source()).isEqualTo("CBN Code 2023, s.5");
        assertThat(put(admin.accessToken(), "/api/benchmarks/bgei", new BenchmarkRequest(new BigDecimal("85"), null))
                .bgei().target()).isEqualByComparingTo("85.00");

        assertThat(putStatus(cs.accessToken(), "/api/benchmarks/dimensions/" + strategy,
                new BenchmarkRequest(new BigDecimal("5.5"), null))).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(putStatus(cs.accessToken(), "/api/benchmarks/bgei",
                new BenchmarkRequest(new BigDecimal("120"), null))).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(putStatus(cs.accessToken(), "/api/benchmarks/dimensions/" + UUID.randomUUID(),
                new BenchmarkRequest(new BigDecimal("3"), null))).isEqualTo(HttpStatus.NOT_FOUND);

        // Every statement rated 4: each dimension scores 4.00 and the BGEI is 80%.
        BoardSummary board = createBoard(admin.accessToken(), "Board of Directors");
        UUID evaluationId = scoredBoardEvaluation(admin, cs, board, 4);

        BenchmarkComparison comparison = comparison(cs.accessToken(), evaluationId);
        assertThat(comparison.rows()).hasSize(19);
        BenchmarkComparisonRow strategyRow = row(comparison, "Strategy");
        assertThat(strategyRow.variance()).isEqualByComparingTo("-0.20");
        assertThat(strategyRow.belowBenchmark()).isTrue();
        assertThat(strategyRow.source()).isEqualTo("CBN Code 2023, s.5");
        BenchmarkComparisonRow composition = row(comparison, "Board Composition");
        assertThat(composition.variance()).isEqualByComparingTo("1.00");
        assertThat(composition.belowBenchmark()).isFalse();
        assertThat(composition.defaultBenchmark()).isTrue();
        BenchmarkComparisonRow bgei = comparison.rows().get(18);
        assertThat(bgei.dimensionId()).isNull();
        assertThat(bgei.actual()).isEqualByComparingTo("80.00");
        assertThat(bgei.variance()).isEqualByComparingTo("-5.00");
        assertThat(comparison.belowCount()).isEqualTo(2);

        String report = restTemplate.exchange("/api/evaluations/" + evaluationId + "/report", HttpMethod.GET,
                authedRequest(cs.accessToken()), String.class).getBody();
        assertThat(report).contains("2 areas fall below the benchmark (negative variance): Strategy (-0.20) and "
                + "BGEI (-5.00%).", "CBN Code 2023, s.5", "Below benchmark");

        restTemplate.exchange("/api/benchmarks/dimensions/" + strategy, HttpMethod.DELETE,
                authedRequest(cs.accessToken()), BenchmarkSettings.class);
        restTemplate.exchange("/api/benchmarks/bgei", HttpMethod.DELETE, authedRequest(cs.accessToken()),
                BenchmarkSettings.class);
        assertThat(settings(cs.accessToken()).dimensions()).allMatch(BenchmarkTargetSummary::isDefault);
        assertThat(comparison(cs.accessToken(), evaluationId).belowCount()).isZero();
        assertThat(restTemplate.exchange("/api/evaluations/" + evaluationId + "/report", HttpMethod.GET,
                authedRequest(cs.accessToken()), String.class).getBody())
                .contains("Every area met or exceeded its benchmark.");
    }

    @Test
    void targetsAreScopedToTheOrganisationAndEditableOnlyByAdminAndSecretary() {
        AuthResponse admin = signup(uniqueEmail(), "Benchmark Access Org");
        AuthResponse cs = createCompanySecretaryAndLogin(admin.accessToken());
        AuthResponse evaluator = createUserAndLogin(admin.accessToken(), "Eve", "Evaluator", Role.EVALUATOR);
        BoardSummary board = createBoard(admin.accessToken(), "Board of Directors");
        DirectorSummary director = createDirector(admin.accessToken(), board.id(), "Dayo Director");
        AuthResponse directorLogin = inviteDirectorAndLogin(cs.accessToken(), director);
        UUID strategy = dimension(settings(cs.accessToken()), "Strategy").dimensionId();
        put(cs.accessToken(), "/api/benchmarks/dimensions/" + strategy, new BenchmarkRequest(new BigDecimal("4"), null));

        assertThat(dimension(settings(evaluator.accessToken()), "Strategy").target()).isEqualByComparingTo("4.00");
        assertThat(putStatus(evaluator.accessToken(), "/api/benchmarks/bgei",
                new BenchmarkRequest(new BigDecimal("60"), null))).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(restTemplate.exchange("/api/benchmarks", HttpMethod.GET, authedRequest(directorLogin.accessToken()),
                String.class).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        AuthResponse otherAdmin = signup(uniqueEmail(), "Other Benchmark Org");
        assertThat(dimension(settings(otherAdmin.accessToken()), "Strategy").isDefault()).isTrue();

        UUID peerEvaluation = createEvaluation(cs, board, EvaluationType.DIRECTOR_PEER, director.id());
        assertThat(restTemplate.exchange("/api/evaluations/" + peerEvaluation + "/benchmark", HttpMethod.GET,
                authedRequest(cs.accessToken()), String.class).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(restTemplate.exchange("/api/evaluations/" + peerEvaluation + "/benchmark", HttpMethod.GET,
                authedRequest(otherAdmin.accessToken()), String.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    private UUID scoredBoardEvaluation(AuthResponse admin, AuthResponse cs, BoardSummary board, int rating) {
        DirectorSummary director = createDirector(admin.accessToken(), board.id(), "Bola Board");
        AuthResponse directorLogin = inviteDirectorAndLogin(cs.accessToken(), director);
        UUID evaluationId = createEvaluation(cs, board, EvaluationType.BOARD, null);
        assertThat(restTemplate.exchange("/api/evaluations/" + evaluationId + "/respondents", HttpMethod.POST,
                authedRequest(cs.accessToken(), new AddRespondentRequest(director.id(), ConfidentialityMode.CONFIDENTIAL)),
                String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(restTemplate.exchange("/api/evaluations/" + evaluationId + "/launch", HttpMethod.POST,
                authedRequest(cs.accessToken()), String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
        QuestionSummary[] questions = restTemplate.exchange(
                "/api/frameworks/active/questions?evaluationType=BOARD", HttpMethod.GET,
                authedRequest(cs.accessToken()), QuestionSummary[].class).getBody();
        for (QuestionSummary question : questions) {
            assertThat(restTemplate.exchange(
                    "/api/my-evaluations/" + evaluationId + "/responses/" + question.id(), HttpMethod.PUT,
                    authedRequest(directorLogin.accessToken(), new SaveResponseRequest(rating, null, null)),
                    String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
        }
        assertThat(restTemplate.exchange("/api/my-evaluations/" + evaluationId + "/submit", HttpMethod.POST,
                authedRequest(directorLogin.accessToken()), String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
        restTemplate.exchange("/api/evaluations/" + evaluationId + "/close", HttpMethod.POST,
                authedRequest(cs.accessToken()), String.class);
        assertThat(restTemplate.exchange("/api/evaluations/" + evaluationId + "/scores/calculate", HttpMethod.POST,
                authedRequest(cs.accessToken()), String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
        return evaluationId;
    }

    private UUID createEvaluation(AuthResponse cs, BoardSummary board, EvaluationType type, UUID subjectId) {
        ResponseEntity<EvaluationSummary> created = restTemplate.exchange("/api/evaluations", HttpMethod.POST,
                authedRequest(cs.accessToken(), new CreateEvaluationRequest(board.id(), type, subjectId, 2026)),
                EvaluationSummary.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return created.getBody().id();
    }

    private BenchmarkSettings settings(String token) {
        ResponseEntity<BenchmarkSettings> response = restTemplate.exchange("/api/benchmarks", HttpMethod.GET,
                authedRequest(token), BenchmarkSettings.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private BenchmarkSettings put(String token, String path, BenchmarkRequest request) {
        ResponseEntity<BenchmarkSettings> response = restTemplate.exchange(path, HttpMethod.PUT,
                authedRequest(token, request), BenchmarkSettings.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private HttpStatus putStatus(String token, String path, BenchmarkRequest request) {
        return HttpStatus.valueOf(restTemplate.exchange(path, HttpMethod.PUT, authedRequest(token, request),
                String.class).getStatusCode().value());
    }

    private BenchmarkComparison comparison(String token, UUID evaluationId) {
        ResponseEntity<BenchmarkComparison> response = restTemplate.exchange(
                "/api/evaluations/" + evaluationId + "/benchmark", HttpMethod.GET, authedRequest(token),
                BenchmarkComparison.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private static BenchmarkTargetSummary dimension(BenchmarkSettings settings, String name) {
        return settings.dimensions().stream().filter(d -> d.name().equals(name)).findFirst().orElseThrow();
    }

    private static BenchmarkComparisonRow row(BenchmarkComparison comparison, String measure) {
        return comparison.rows().stream().filter(r -> r.measure().equals(measure)).findFirst().orElseThrow();
    }
}
