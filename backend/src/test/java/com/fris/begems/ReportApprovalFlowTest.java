package com.fris.begems;

import static org.assertj.core.api.Assertions.assertThat;

import com.fris.begems.approval.ApprovalDecision;
import com.fris.begems.approval.ReportStage;
import com.fris.begems.approval.dto.ApprovalRequest;
import com.fris.begems.approval.dto.MyBoardReport;
import com.fris.begems.approval.dto.ReportApprovalStatus;
import com.fris.begems.auth.dto.AuthResponse;
import com.fris.begems.benchmark.dto.BenchmarkRequest;
import com.fris.begems.board.dto.BoardSummary;
import com.fris.begems.director.DirectorClassification;
import com.fris.begems.director.dto.DirectorSummary;
import com.fris.begems.evaluation.ConfidentialityMode;
import com.fris.begems.evaluation.dto.AddRespondentRequest;
import com.fris.begems.evaluation.dto.CreateEvaluationRequest;
import com.fris.begems.evaluation.dto.EvaluationSummary;
import com.fris.begems.framework.EvaluationType;
import com.fris.begems.framework.dto.QuestionSummary;
import com.fris.begems.notification.NotificationType;
import com.fris.begems.notification.dto.NotificationSummary;
import com.fris.begems.response.dto.SaveResponseRequest;
import com.fris.begems.support.IntegrationTestSupport;
import com.fris.begems.user.Role;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ReportApprovalFlowTest extends IntegrationTestSupport {

    @Test
    void boardReportMovesThroughEachStageToAFinalVersionDirectorsCanRead() {
        AuthResponse admin = signup(uniqueEmail(), "Approval Org");
        AuthResponse cs = createCompanySecretaryAndLogin(admin.accessToken());
        AuthResponse evaluator = createUserAndLogin(admin.accessToken(), "Eve", "Evaluator", Role.EVALUATOR);
        BoardSummary board = createBoard(admin.accessToken(), "Board of Directors");
        DirectorSummary chair = createChairman(admin.accessToken(), board.id());
        AuthResponse chairLogin = inviteDirectorAndLogin(cs.accessToken(), chair);
        DirectorSummary member = createDirector(admin.accessToken(), board.id(), "Bola Board");
        AuthResponse memberLogin = inviteDirectorAndLogin(cs.accessToken(), member);
        UUID evaluationId = scoredBoardEvaluation(cs, board, member, memberLogin);

        ReportApprovalStatus status = status(cs.accessToken(), evaluationId);
        assertThat(status.stage()).isEqualTo(ReportStage.EVALUATOR_REVIEW);
        assertThat(status.stages()).hasSize(6);
        assertThat(status.canApprove()).isTrue();
        assertThat(status.canReturn()).isFalse();
        assertThat(statusCode(chairLogin.accessToken(), evaluationId)).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(reportResponse(chairLogin.accessToken(), evaluationId).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(myBoardReports(chairLogin.accessToken())).isEmpty();

        ResponseEntity<String> draft = reportResponse(cs.accessToken(), evaluationId);
        assertThat(draft.getBody()).contains("Draft for review", "Evaluator Review stage", "Draft — Evaluator Review");
        assertThat(draft.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)).contains("%28Draft%29");

        assertThat(decide(cs.accessToken(), evaluationId, "return", "Too early").getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        assertThat(approve(evaluator.accessToken(), evaluationId, null).stage()).isEqualTo(ReportStage.DRAFT_REPORT);
        assertThat(approve(evaluator.accessToken(), evaluationId, null).stage()).isEqualTo(ReportStage.QUALITY_REVIEW);
        assertThat(decide(cs.accessToken(), evaluationId, "return", "  ").getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        ResponseEntity<ReportApprovalStatus> returned = restTemplate.exchange(
                "/api/evaluations/" + evaluationId + "/report-approval/return", HttpMethod.POST,
                authedRequest(cs.accessToken(), new ApprovalRequest("Add the committee commentary")),
                ReportApprovalStatus.class);
        assertThat(returned.getBody().stage()).isEqualTo(ReportStage.DRAFT_REPORT);
        assertThat(notifications(evaluator.accessToken()))
                .anyMatch(n -> n.type() == NotificationType.REPORT_RETURNED
                        && n.body().contains("Add the committee commentary"));

        approve(evaluator.accessToken(), evaluationId, null);
        assertThat(approve(evaluator.accessToken(), evaluationId, null).stage()).isEqualTo(ReportStage.CS_REVIEW);
        assertThat(decide(evaluator.accessToken(), evaluationId, "approve", null).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(notifications(cs.accessToken())).anyMatch(n -> n.type() == NotificationType.REPORT_APPROVAL_REQUIRED
                && n.title().equals("2026 Board Evaluation Report: Company Secretary Review"));
        assertThat(approve(cs.accessToken(), evaluationId, null).stage()).isEqualTo(ReportStage.BOARD_APPROVAL);

        assertThat(statusCode(memberLogin.accessToken(), evaluationId)).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(myBoardReports(memberLogin.accessToken())).isEmpty();
        List<MyBoardReport> chairReports = myBoardReports(chairLogin.accessToken());
        assertThat(chairReports).singleElement().satisfies(r -> {
            assertThat(r.evaluationId()).isEqualTo(evaluationId);
            assertThat(r.awaitingMyApproval()).isTrue();
        });
        assertThat(notifications(chairLogin.accessToken()))
                .anyMatch(n -> n.type() == NotificationType.REPORT_APPROVAL_REQUIRED && n.link().equals("/board-reports"));
        ReportApprovalStatus chairView = status(chairLogin.accessToken(), evaluationId);
        assertThat(chairView.canApprove()).isTrue();
        assertThat(chairView.commentRequired()).isFalse();
        assertThat(chairView.approveLabel()).isEqualTo("Approve the final report");
        assertThat(reportResponse(chairLogin.accessToken(), evaluationId).getBody())
                .contains("Chairman/Board Approval stage");

        ReportApprovalStatus approved = approve(chairLogin.accessToken(), evaluationId, null);
        assertThat(approved.stage()).isEqualTo(ReportStage.FINAL);
        assertThat(approved.canApprove()).isFalse();
        assertThat(approved.history()).hasSize(7);
        assertThat(approved.history().get(2).decision()).isEqualTo(ApprovalDecision.RETURNED);
        assertThat(approved.history().get(6).actorName()).isEqualTo(chair.name());
        assertThat(decide(chairLogin.accessToken(), evaluationId, "approve", null).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        assertThat(myBoardReports(memberLogin.accessToken())).singleElement()
                .satisfies(r -> assertThat(r.stage()).isEqualTo(ReportStage.FINAL));
        assertThat(notifications(memberLogin.accessToken()))
                .anyMatch(n -> n.type() == NotificationType.REPORT_FINALISED);
        ResponseEntity<String> finalReport = reportResponse(memberLogin.accessToken(), evaluationId);
        assertThat(finalReport.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(finalReport.getBody()).contains("Appendix D — Approval record", "Final — approved",
                "Add the committee commentary", "Returned to Draft Report").doesNotContain("Draft for review");
        assertThat(finalReport.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)).doesNotContain("Draft");

        // The issued version is kept as approved, even if the organisation's settings change afterwards.
        UUID strategy = restTemplate.exchange("/api/benchmarks", HttpMethod.GET, authedRequest(cs.accessToken()),
                com.fris.begems.benchmark.dto.BenchmarkSettings.class).getBody().dimensions().stream()
                .filter(d -> d.name().equals("Strategy")).findFirst().orElseThrow().dimensionId();
        restTemplate.exchange("/api/benchmarks/dimensions/" + strategy, HttpMethod.PUT,
                authedRequest(cs.accessToken(), new BenchmarkRequest(new BigDecimal("4.5"), "Changed after approval")),
                String.class);
        assertThat(reportResponse(cs.accessToken(), evaluationId).getBody()).isEqualTo(finalReport.getBody());
    }

    @Test
    void companySecretaryMayRecordTheBoardsApprovalWithAComment() {
        AuthResponse admin = signup(uniqueEmail(), "Approval Record Org");
        AuthResponse cs = createCompanySecretaryAndLogin(admin.accessToken());
        AuthResponse evaluator = createUserAndLogin(admin.accessToken(), "Eve", "Evaluator", Role.EVALUATOR);
        BoardSummary board = createBoard(admin.accessToken(), "Board of Directors");
        DirectorSummary member = createDirector(admin.accessToken(), board.id(), "Bola Board");
        AuthResponse memberLogin = inviteDirectorAndLogin(cs.accessToken(), member);
        UUID evaluationId = scoredBoardEvaluation(cs, board, member, memberLogin);

        for (int i = 0; i < 4; i++) {
            approve(cs.accessToken(), evaluationId, null);
        }
        ReportApprovalStatus atBoard = status(cs.accessToken(), evaluationId);
        assertThat(atBoard.stage()).isEqualTo(ReportStage.BOARD_APPROVAL);
        assertThat(atBoard.commentRequired()).isTrue();
        assertThat(atBoard.approveLabel()).isEqualTo("Record the Board's approval");
        assertThat(status(evaluator.accessToken(), evaluationId).canApprove()).isFalse();

        assertThat(decide(cs.accessToken(), evaluationId, "approve", null).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        ReportApprovalStatus approved = approve(cs.accessToken(), evaluationId,
                "Approved at the Board meeting of 2 October 2026");
        assertThat(approved.stage()).isEqualTo(ReportStage.FINAL);
        assertThat(approved.history().get(4).comment()).isEqualTo("Approved at the Board meeting of 2 October 2026");

        UUID peerEvaluation = createEvaluation(cs, board, EvaluationType.DIRECTOR_PEER, member.id());
        assertThat(statusCode(cs.accessToken(), peerEvaluation)).isEqualTo(HttpStatus.CONFLICT);
        AuthResponse otherAdmin = signup(uniqueEmail(), "Other Approval Org");
        assertThat(statusCode(otherAdmin.accessToken(), evaluationId)).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(restTemplate.exchange("/api/evaluations?boardId=" + board.id(), HttpMethod.GET,
                authedRequest(cs.accessToken()), EvaluationSummary[].class).getBody())
                .filteredOn(e -> e.id().equals(evaluationId))
                .singleElement().satisfies(e -> assertThat(e.reportStage()).isEqualTo(ReportStage.FINAL));
    }

    private DirectorSummary createChairman(String adminToken, UUID boardId) {
        ResponseEntity<DirectorSummary> response = restTemplate.exchange("/api/directors", HttpMethod.POST,
                authedRequest(adminToken, directorRequest(boardId, "Chidi Chairman", uniqueEmail(),
                        DirectorClassification.CHAIRMAN)),
                DirectorSummary.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private UUID scoredBoardEvaluation(AuthResponse cs, BoardSummary board, DirectorSummary respondent,
            AuthResponse respondentLogin) {
        UUID evaluationId = createEvaluation(cs, board, EvaluationType.BOARD, null);
        restTemplate.exchange("/api/evaluations/" + evaluationId + "/respondents", HttpMethod.POST,
                authedRequest(cs.accessToken(), new AddRespondentRequest(respondent.id(),
                        ConfidentialityMode.CONFIDENTIAL)), String.class);
        restTemplate.exchange("/api/evaluations/" + evaluationId + "/launch", HttpMethod.POST,
                authedRequest(cs.accessToken()), String.class);
        QuestionSummary[] questions = restTemplate.exchange(
                "/api/frameworks/active/questions?evaluationType=BOARD", HttpMethod.GET,
                authedRequest(cs.accessToken()), QuestionSummary[].class).getBody();
        for (QuestionSummary question : questions) {
            restTemplate.exchange("/api/my-evaluations/" + evaluationId + "/responses/" + question.id(),
                    HttpMethod.PUT, authedRequest(respondentLogin.accessToken(), new SaveResponseRequest(4, null, null)),
                    String.class);
        }
        restTemplate.exchange("/api/my-evaluations/" + evaluationId + "/submit", HttpMethod.POST,
                authedRequest(respondentLogin.accessToken()), String.class);
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

    private ReportApprovalStatus status(String token, UUID evaluationId) {
        ResponseEntity<ReportApprovalStatus> response = restTemplate.exchange(
                "/api/evaluations/" + evaluationId + "/report-approval", HttpMethod.GET, authedRequest(token),
                ReportApprovalStatus.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private HttpStatus statusCode(String token, UUID evaluationId) {
        return HttpStatus.valueOf(restTemplate.exchange("/api/evaluations/" + evaluationId + "/report-approval",
                HttpMethod.GET, authedRequest(token), String.class).getStatusCode().value());
    }

    private ReportApprovalStatus approve(String token, UUID evaluationId, String comment) {
        ResponseEntity<ReportApprovalStatus> response = restTemplate.exchange(
                "/api/evaluations/" + evaluationId + "/report-approval/approve", HttpMethod.POST,
                authedRequest(token, new ApprovalRequest(comment)), ReportApprovalStatus.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private ResponseEntity<String> decide(String token, UUID evaluationId, String action, String comment) {
        return restTemplate.exchange("/api/evaluations/" + evaluationId + "/report-approval/" + action,
                HttpMethod.POST, authedRequest(token, new ApprovalRequest(comment)), String.class);
    }

    private ResponseEntity<String> reportResponse(String token, UUID evaluationId) {
        return restTemplate.exchange("/api/evaluations/" + evaluationId + "/report", HttpMethod.GET,
                authedRequest(token), String.class);
    }

    private List<MyBoardReport> myBoardReports(String token) {
        return Arrays.asList(restTemplate.exchange("/api/my-board-reports", HttpMethod.GET, authedRequest(token),
                MyBoardReport[].class).getBody());
    }

    private List<NotificationSummary> notifications(String token) {
        return Arrays.asList(restTemplate.exchange("/api/notifications", HttpMethod.GET, authedRequest(token),
                NotificationSummary[].class).getBody());
    }
}
