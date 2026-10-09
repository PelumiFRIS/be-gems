package com.fris.begems;

import static org.assertj.core.api.Assertions.assertThat;

import com.fris.begems.action.dto.CreateActionRequest;
import com.fris.begems.auth.dto.AuthResponse;
import com.fris.begems.board.dto.BoardSummary;
import com.fris.begems.director.DirectorClassification;
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
import com.fris.begems.framework.ResponseType;
import com.fris.begems.framework.dto.QuestionSummary;
import com.fris.begems.recommendation.RecommendationPriority;
import com.fris.begems.recommendation.RecommendationStatus;
import com.fris.begems.recommendation.dto.CreateRecommendationRequest;
import com.fris.begems.response.dto.SaveResponseRequest;
import com.fris.begems.skill.dto.SkillRatingRequest;
import com.fris.begems.skill.dto.SkillsMatrix;
import com.fris.begems.support.IntegrationTestSupport;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ReportFlowTest extends IntegrationTestSupport {

    @Test
    void boardReportCoversEverySectionEscapesUserTextAndIsRestrictedToStaffRoles() {
        AuthResponse admin = signup(uniqueEmail(), "Report Org");
        AuthResponse cs = createCompanySecretaryAndLogin(admin.accessToken());
        BoardSummary board = createBoard(admin.accessToken(), "Board of Directors");
        DirectorSummary chairman = createDirector(admin.accessToken(), board.id(), "Chidi Chairman",
                DirectorClassification.CHAIRMAN);
        DirectorSummary member = createDirector(admin.accessToken(), board.id(), "Ngozi Member",
                DirectorClassification.INDEPENDENT_NON_EXECUTIVE_DIRECTOR);
        AuthResponse chairmanLogin = inviteDirectorAndLogin(cs.accessToken(), chairman);
        AuthResponse memberLogin = inviteDirectorAndLogin(cs.accessToken(), member);

        UUID evaluationId = createEvaluation(cs, board, EvaluationType.BOARD, null);
        addRespondent(cs, evaluationId, chairman.id());
        addRespondent(cs, evaluationId, member.id());
        launch(cs, evaluationId);

        // Not yet scored: no report.
        assertThat(report(cs.accessToken(), evaluationId).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        for (QuestionSummary question : questions(cs, EvaluationType.BOARD)) {
            answer(chairmanLogin, evaluationId, question.id(), 5);
            answer(memberLogin, evaluationId, question.id(), 3);
        }
        submit(chairmanLogin, evaluationId);
        submit(memberLogin, evaluationId);
        closeAndScore(cs, evaluationId);

        ResponseEntity<FindingSummary> finding = restTemplate.exchange(
                "/api/evaluations/" + evaluationId + "/findings", HttpMethod.POST,
                authedRequest(cs.accessToken(), new CreateFindingRequest(null,
                        "<script>alert('x')</script> & late board packs", FindingSeverity.HIGH, null,
                        "NCCG 2018, Principle 14", null, null)),
                FindingSummary.class);
        assertThat(finding.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        restTemplate.exchange("/api/findings/" + finding.getBody().id() + "/recommendations", HttpMethod.POST,
                authedRequest(cs.accessToken(), new CreateRecommendationRequest("Circulate packs 7 days ahead",
                        "Company Secretary", null, LocalDate.now().plusMonths(1), RecommendationPriority.HIGH,
                        RecommendationStatus.OPEN)),
                String.class);
        restTemplate.exchange("/api/findings/" + finding.getBody().id() + "/actions", HttpMethod.POST,
                authedRequest(cs.accessToken(), new CreateActionRequest("Update the board pack SOP",
                        "Company Secretary", "Chairman", LocalDate.now().minusDays(1), null)),
                String.class);

        // Strategy (High): one Advanced director. Audit (High): nobody Advanced.
        rateSkill(cs.accessToken(), board.id(), "Strategy", chairman.id(), 5);
        rateSkill(cs.accessToken(), board.id(), "Strategy", member.id(), 3);
        rateSkill(cs.accessToken(), board.id(), "Audit", member.id(), 2);

        ResponseEntity<String> response = report(cs.accessToken(), evaluationId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType().toString()).contains("text/html");
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .contains("Board%20Evaluation%20Report%202026.html");

        String html = response.getBody();
        for (String section : List.of("Confidentiality Statement", "Executive Summary", "Background", "Objectives",
                "Scope", "Methodology", "Evaluation Framework", "Board Composition", "Board Effectiveness Results",
                "Committee Results", "Chairman Evaluation", "Director Evaluation", "Governance Maturity",
                "Regulatory Benchmark", "Key Strengths", "Key Findings", "Areas for Improvement",
                "Recommendations", "Action Plan", "Conclusion", "Appendices")) {
            assertThat(html).contains(section);
        }
        // Uniform 5s and 3s average to 4.00 everywhere: Managed, BGEI 80% (Highly Effective).
        assertThat(html).contains("80.00%", "Highly Effective", "4.00 / 5.00", "Managed");
        assertThat(html).contains("Chidi Chairman", "Circulate packs 7 days ahead", "Not started (overdue)");
        assertThat(html).contains("&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt; &amp; late board packs");
        assertThat(html).doesNotContain("<script>alert");
        assertThat(html).contains("Board Skills Matrix", "Of the 2 competencies assessed: "
                + "1 single-person dependency and 1 critical gap. 16 competencies have not yet been rated.",
                "Critical gaps (High requirement, no director rated Advanced or above): Audit.",
                "Single-person dependencies (High requirement, only one director rated Advanced or above): "
                        + "Strategy.");

        assertThat(report(admin.accessToken(), evaluationId).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(report(memberLogin.accessToken(), evaluationId).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        AuthResponse otherOrgAdmin = signup(uniqueEmail(), "Other Org");
        assertThat(report(otherOrgAdmin.accessToken(), evaluationId).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void individualReportSplitsSelfPeerAndChairmanViewsWithoutNamingRespondents() {
        AuthResponse admin = signup(uniqueEmail(), "Individual Report Org");
        AuthResponse cs = createCompanySecretaryAndLogin(admin.accessToken());
        BoardSummary board = createBoard(admin.accessToken(), "Board of Directors");
        DirectorSummary subject = createDirector(admin.accessToken(), board.id(), "Sade Subject",
                DirectorClassification.NON_EXECUTIVE_DIRECTOR);
        DirectorSummary chairman = createDirector(admin.accessToken(), board.id(), "Chidi Chairman",
                DirectorClassification.CHAIRMAN);
        DirectorSummary peer = createDirector(admin.accessToken(), board.id(), "Femi Peer",
                DirectorClassification.NON_EXECUTIVE_DIRECTOR);
        AuthResponse subjectLogin = inviteDirectorAndLogin(cs.accessToken(), subject);
        AuthResponse chairmanLogin = inviteDirectorAndLogin(cs.accessToken(), chairman);
        AuthResponse peerLogin = inviteDirectorAndLogin(cs.accessToken(), peer);

        UUID evaluationId = createEvaluation(cs, board, EvaluationType.DIRECTOR_PEER, subject.id());
        addRespondent(cs, evaluationId, subject.id());
        addRespondent(cs, evaluationId, chairman.id());
        addRespondent(cs, evaluationId, peer.id());
        launch(cs, evaluationId);

        for (QuestionSummary question : questions(cs, EvaluationType.DIRECTOR_PEER)) {
            if (question.responseType() == ResponseType.NARRATIVE) {
                saveText(peerLogin, evaluationId, question.id(), "Peer comment on: " + question.text());
                continue;
            }
            answer(subjectLogin, evaluationId, question.id(), 5);
            answer(chairmanLogin, evaluationId, question.id(), 4);
            answer(peerLogin, evaluationId, question.id(), 2);
        }
        submit(subjectLogin, evaluationId);
        submit(chairmanLogin, evaluationId);
        submit(peerLogin, evaluationId);
        closeAndScore(cs, evaluationId);
        rateSkill(cs.accessToken(), board.id(), "Finance", subject.id(), 4);
        rateSkill(cs.accessToken(), board.id(), "Finance", peer.id(), 1);

        ResponseEntity<String> response = report(cs.accessToken(), evaluationId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String html = response.getBody();
        assertThat(html).contains("Individual Director Evaluation Report", "Sade Subject");
        assertThat(html).contains("Board skills matrix ratings", "4 – Advanced").doesNotContain("1 – Basic");
        for (String section : List.of("Self-assessment", "Peer Assessment", "Chairman Assessment",
                "Competency Assessment", "Attendance", "Contribution", "Strengths", "Development Areas",
                "Training Recommendations")) {
            assertThat(html).contains(section);
        }
        // Self 5.00, peer 2.00, Chairman 4.00; overall (all three) 3.67.
        assertThat(html).contains("5.00 / 5.00", "2.00 / 5.00", "4.00 / 5.00", "3.67 / 5.00");
        // Self (5.00) minus the other directors' average (3.00).
        assertThat(html).contains("+2.00");
        assertThat(html).contains("Peer comment on: What should this director continue doing?",
                "Peer comment on: Any board-level gaps to address");
        assertThat(html).doesNotContain("Femi Peer").doesNotContain("Chidi Chairman");

        setCompanySecretaryAccess(admin.accessToken(), admin.user().id(), false);
        assertThat(report(admin.accessToken(), evaluationId).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        setCompanySecretaryAccess(admin.accessToken(), admin.user().id(), true);
        assertThat(report(admin.accessToken(), evaluationId).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(report(subjectLogin.accessToken(), evaluationId).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    private DirectorSummary createDirector(String adminToken, UUID boardId, String name,
            DirectorClassification classification) {
        ResponseEntity<DirectorSummary> response = restTemplate.exchange("/api/directors", HttpMethod.POST,
                authedRequest(adminToken, directorRequest(boardId, name, uniqueEmail(), classification)),
                DirectorSummary.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private UUID createEvaluation(AuthResponse cs, BoardSummary board, EvaluationType type, UUID subjectId) {
        ResponseEntity<EvaluationSummary> created = restTemplate.exchange("/api/evaluations", HttpMethod.POST,
                authedRequest(cs.accessToken(), new CreateEvaluationRequest(board.id(), type, subjectId, 2026)),
                EvaluationSummary.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return created.getBody().id();
    }

    private void addRespondent(AuthResponse cs, UUID evaluationId, UUID directorId) {
        ResponseEntity<EvaluationDetail> response = restTemplate.exchange(
                "/api/evaluations/" + evaluationId + "/respondents", HttpMethod.POST,
                authedRequest(cs.accessToken(), new AddRespondentRequest(directorId, ConfidentialityMode.CONFIDENTIAL)),
                EvaluationDetail.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private void launch(AuthResponse cs, UUID evaluationId) {
        ResponseEntity<String> response = restTemplate.exchange("/api/evaluations/" + evaluationId + "/launch",
                HttpMethod.POST, authedRequest(cs.accessToken()), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private QuestionSummary[] questions(AuthResponse cs, EvaluationType type) {
        return restTemplate.exchange("/api/frameworks/active/questions?evaluationType=" + type, HttpMethod.GET,
                authedRequest(cs.accessToken()), QuestionSummary[].class).getBody();
    }

    private void answer(AuthResponse respondent, UUID evaluationId, UUID questionId, int rating) {
        save(respondent, evaluationId, questionId, new SaveResponseRequest(rating, null, null));
    }

    private void saveText(AuthResponse respondent, UUID evaluationId, UUID questionId, String text) {
        save(respondent, evaluationId, questionId, new SaveResponseRequest(null, text, null));
    }

    private void save(AuthResponse respondent, UUID evaluationId, UUID questionId, SaveResponseRequest request) {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/my-evaluations/" + evaluationId + "/responses/" + questionId, HttpMethod.PUT,
                authedRequest(respondent.accessToken(), request), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private void submit(AuthResponse respondent, UUID evaluationId) {
        ResponseEntity<String> response = restTemplate.exchange("/api/my-evaluations/" + evaluationId + "/submit",
                HttpMethod.POST, authedRequest(respondent.accessToken()), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private void closeAndScore(AuthResponse cs, UUID evaluationId) {
        restTemplate.exchange("/api/evaluations/" + evaluationId + "/close", HttpMethod.POST,
                authedRequest(cs.accessToken()), String.class);
        ResponseEntity<String> scored = restTemplate.exchange(
                "/api/evaluations/" + evaluationId + "/scores/calculate", HttpMethod.POST,
                authedRequest(cs.accessToken()), String.class);
        assertThat(scored.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private ResponseEntity<String> report(String token, UUID evaluationId) {
        return restTemplate.exchange("/api/evaluations/" + evaluationId + "/report", HttpMethod.GET,
                authedRequest(token), String.class);
    }

    private void rateSkill(String token, UUID boardId, String skillName, UUID directorId, int rating) {
        SkillsMatrix matrix = restTemplate.exchange("/api/boards/" + boardId + "/skills-matrix", HttpMethod.GET,
                authedRequest(token), SkillsMatrix.class).getBody();
        UUID skillId = matrix.skills().stream().filter(s -> s.name().equals(skillName)).findFirst().orElseThrow()
                .id();
        ResponseEntity<String> rated = restTemplate.exchange("/api/skills/" + skillId + "/ratings/" + directorId,
                HttpMethod.PUT, authedRequest(token, new SkillRatingRequest(rating)), String.class);
        assertThat(rated.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
