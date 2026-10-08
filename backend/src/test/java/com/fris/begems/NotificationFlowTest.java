package com.fris.begems;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.fris.begems.action.ActionStatus;
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
import com.fris.begems.evaluation.dto.ReminderResult;
import com.fris.begems.finding.FindingSeverity;
import com.fris.begems.finding.dto.CreateFindingRequest;
import com.fris.begems.finding.dto.FindingSummary;
import com.fris.begems.framework.EvaluationType;
import com.fris.begems.notification.NotificationType;
import com.fris.begems.notification.OverdueActionDigestJob;
import com.fris.begems.notification.dto.NotificationSummary;
import com.fris.begems.notification.email.EmailMessage;
import com.fris.begems.notification.email.EmailSender;
import com.fris.begems.support.IntegrationTestSupport;
import com.fris.begems.user.Role;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

class NotificationFlowTest extends IntegrationTestSupport {

    private static final long EMAIL_TIMEOUT_MS = 5000;

    @MockitoSpyBean
    private EmailSender emailSender;

    @Autowired
    private OverdueActionDigestJob overdueActionDigestJob;

    @Test
    void evaluationLifecycleNotifiesRespondentsAndStaff() {
        AuthResponse admin = signup(uniqueEmail(), "Notify Org");
        AuthResponse cs = createCompanySecretaryAndLogin(admin.accessToken());
        BoardSummary board = createBoard(admin.accessToken(), "Board of Directors");
        DirectorSummary directorA = createDirector(admin.accessToken(), board.id(), "Ada Lovelace");
        DirectorSummary directorB = createDirector(admin.accessToken(), board.id(), "Grace Hopper");
        DirectorSummary uninvited = createDirector(admin.accessToken(), board.id(), "Alan Turing");
        AuthResponse loginA = inviteDirectorAndLogin(cs.accessToken(), directorA);
        AuthResponse loginB = inviteDirectorAndLogin(cs.accessToken(), directorB);

        verifyEmailSent(directorA.email(), "Your BE-GEMS account for Notify Org");
        verifyEmailSent(cs.user().email(), "Your BE-GEMS account for Notify Org");

        UUID evaluationId = createEvaluation(cs, board);
        addRespondent(cs.accessToken(), evaluationId, directorA.id());
        addRespondent(cs.accessToken(), evaluationId, directorB.id());
        addRespondent(cs.accessToken(), evaluationId, uninvited.id());

        ResponseEntity<String> remindBeforeLaunch = restTemplate.exchange(
                "/api/evaluations/" + evaluationId + "/reminders", HttpMethod.POST, authedRequest(cs.accessToken()),
                String.class);
        assertThat(remindBeforeLaunch.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        launch(cs, evaluationId);

        List<NotificationSummary> inboxA = notifications(loginA.accessToken());
        assertThat(inboxA).hasSize(1);
        assertThat(inboxA.get(0).type()).isEqualTo(NotificationType.EVALUATION_INVITATION);
        assertThat(inboxA.get(0).title()).isEqualTo("You're invited to complete the 2026 Board Evaluation");
        assertThat(inboxA.get(0).link()).isEqualTo("/my-evaluations/" + evaluationId);
        assertThat(inboxA.get(0).read()).isFalse();
        assertThat(unreadCount(loginA.accessToken())).isEqualTo(1);
        verifyEmailSent(directorA.email(), "You're invited to complete the 2026 Board Evaluation");

        submit(loginA, evaluationId);
        verifyEmailSent(directorA.email(), "Thank you — your 2026 Board Evaluation response was received");

        // Only staff can send reminders. B is outstanding; the uninvited director has no login to remind.
        ResponseEntity<String> directorRemind = restTemplate.exchange(
                "/api/evaluations/" + evaluationId + "/reminders", HttpMethod.POST, authedRequest(loginA.accessToken()),
                String.class);
        assertThat(directorRemind.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<ReminderResult> reminded = restTemplate.exchange(
                "/api/evaluations/" + evaluationId + "/reminders", HttpMethod.POST, authedRequest(cs.accessToken()),
                ReminderResult.class);
        assertThat(reminded.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reminded.getBody()).isEqualTo(new ReminderResult(1, 1));
        assertThat(notifications(loginB.accessToken())).extracting(NotificationSummary::type)
                .containsExactly(NotificationType.EVALUATION_REMINDER, NotificationType.EVALUATION_INVITATION);
        assertThat(notifications(loginA.accessToken())).extracting(NotificationSummary::type)
                .doesNotContain(NotificationType.EVALUATION_REMINDER);
        verifyEmailSent(directorB.email(), "Reminder: please complete the 2026 Board Evaluation");

        // Mark one read, then the rest.
        List<NotificationSummary> inboxB = notifications(loginB.accessToken());
        ResponseEntity<NotificationSummary> markedRead = restTemplate.exchange(
                "/api/notifications/" + inboxB.get(0).id() + "/read", HttpMethod.POST,
                authedRequest(loginB.accessToken()), NotificationSummary.class);
        assertThat(markedRead.getBody().read()).isTrue();
        assertThat(unreadCount(loginB.accessToken())).isEqualTo(1);

        ResponseEntity<String> otherUsersNotification = restTemplate.exchange(
                "/api/notifications/" + inboxB.get(1).id() + "/read", HttpMethod.POST,
                authedRequest(loginA.accessToken()), String.class);
        assertThat(otherUsersNotification.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ResponseEntity<Void> readAll = restTemplate.exchange(
                "/api/notifications/read-all", HttpMethod.POST, authedRequest(loginB.accessToken()), Void.class);
        assertThat(readAll.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(unreadCount(loginB.accessToken())).isZero();
        assertThat(unreadCount(loginA.accessToken())).isEqualTo(2);

        // The staff alert waits until every respondent — including the one without a login — has submitted.
        submit(loginB, evaluationId);
        assertThat(notifications(cs.accessToken())).isEmpty();
    }

    @Test
    void lastSubmissionAlertsStaffAndActionsNotifyOnClosureAndWhenOverdue() {
        AuthResponse admin = signup(uniqueEmail(), "Notify Actions Org");
        AuthResponse cs = createCompanySecretaryAndLogin(admin.accessToken());
        AuthResponse evaluator = createUserAndLogin(admin.accessToken(), "Eve", "Evaluator", Role.EVALUATOR);
        BoardSummary board = createBoard(admin.accessToken(), "Board of Directors");
        DirectorSummary director = createDirector(admin.accessToken(), board.id(), "Ada Lovelace");
        AuthResponse directorLogin = inviteDirectorAndLogin(cs.accessToken(), director);

        UUID evaluationId = createEvaluation(cs, board);
        addRespondent(cs.accessToken(), evaluationId, director.id());
        launch(cs, evaluationId);
        submit(directorLogin, evaluationId);

        for (AuthResponse staff : List.of(cs, evaluator)) {
            List<NotificationSummary> inbox = notifications(staff.accessToken());
            assertThat(inbox).extracting(NotificationSummary::type)
                    .containsExactly(NotificationType.ALL_RESPONSES_SUBMITTED);
            assertThat(inbox.get(0).link()).isEqualTo("/evaluations/" + evaluationId);
        }
        // The founding admin holds Company Secretary access, so they're staff too.
        assertThat(notifications(admin.accessToken())).extracting(NotificationSummary::type)
                .containsExactly(NotificationType.ALL_RESPONSES_SUBMITTED);
        verifyEmailSent(cs.user().email(), "All responses received: 2026 Board Evaluation");

        restTemplate.exchange("/api/evaluations/" + evaluationId + "/close", HttpMethod.POST,
                authedRequest(cs.accessToken()), EvaluationSummary.class);
        restTemplate.exchange("/api/evaluations/" + evaluationId + "/scores/calculate", HttpMethod.POST,
                authedRequest(cs.accessToken()), String.class);

        ResponseEntity<FindingSummary> finding = restTemplate.exchange(
                "/api/evaluations/" + evaluationId + "/findings", HttpMethod.POST,
                authedRequest(cs.accessToken(), new CreateFindingRequest(null, "Board packs circulate late",
                        FindingSeverity.HIGH, null, null, null, null)),
                FindingSummary.class);
        assertThat(finding.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        String description = "Adopt a 48-hour board pack rule " + UUID.randomUUID();
        ResponseEntity<ActionSummary> action = restTemplate.exchange(
                "/api/findings/" + finding.getBody().id() + "/actions", HttpMethod.POST,
                authedRequest(cs.accessToken(), new CreateActionRequest(description, "Company Secretary",
                        "Board Chair", LocalDate.now().minusDays(3), null)),
                ActionSummary.class);
        assertThat(action.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        overdueActionDigestJob.sendDigests();
        NotificationSummary digest = notifications(cs.accessToken()).get(0);
        assertThat(digest.type()).isEqualTo(NotificationType.ACTIONS_OVERDUE);
        assertThat(digest.title()).isEqualTo("1 corrective action is overdue");
        assertThat(digest.link()).isEqualTo("/actions");
        verify(emailSender, timeout(EMAIL_TIMEOUT_MS)).send(argThat(m -> m.toAddress().equals(cs.user().email())
                && m.subject().equals("1 corrective action is overdue") && m.text().contains(description)));

        ResponseEntity<ActionSummary> closed = restTemplate.exchange(
                "/api/actions/" + action.getBody().id(), HttpMethod.PUT,
                authedRequest(evaluator.accessToken(), new UpdateActionRequest(description, "Company Secretary",
                        "Board Chair", LocalDate.now().minusDays(3), ActionStatus.COMPLETED, "Rule adopted")),
                ActionSummary.class);
        assertThat(closed.getStatusCode()).isEqualTo(HttpStatus.OK);

        NotificationSummary closure = notifications(cs.accessToken()).get(0);
        assertThat(closure.type()).isEqualTo(NotificationType.ACTION_CLOSED);
        assertThat(closure.body()).isEqualTo("Eve Evaluator marked a corrective action as completed: " + description);
        assertThat(notifications(evaluator.accessToken())).extracting(NotificationSummary::type)
                .doesNotContain(NotificationType.ACTION_CLOSED);
    }

    @Test
    void accountEmailsNeverContainThePassword() {
        AuthResponse admin = signup(uniqueEmail(), "Notify Accounts Org");
        AuthResponse cs = createCompanySecretaryAndLogin(admin.accessToken());

        ResponseEntity<Map<String, String>> reset = restTemplate.exchange(
                "/api/users/" + cs.user().id() + "/reset-password", HttpMethod.POST,
                authedRequest(admin.accessToken()), new ParameterizedTypeReference<>() { });
        assertThat(reset.getStatusCode()).isEqualTo(HttpStatus.OK);
        String temporaryPassword = reset.getBody().get("temporaryPassword");

        verify(emailSender, timeout(EMAIL_TIMEOUT_MS)).send(argThat(m -> m.toAddress().equals(cs.user().email())
                && m.subject().equals("Your BE-GEMS password was reset")
                && !m.text().contains(temporaryPassword)));
    }

    private void verifyEmailSent(String toAddress, String subject) {
        verify(emailSender, timeout(EMAIL_TIMEOUT_MS)).send(argThat((EmailMessage m) ->
                m.toAddress().equalsIgnoreCase(toAddress) && m.subject().equals(subject)));
    }

    private UUID createEvaluation(AuthResponse cs, BoardSummary board) {
        ResponseEntity<EvaluationSummary> created = restTemplate.exchange(
                "/api/evaluations", HttpMethod.POST,
                authedRequest(cs.accessToken(), new CreateEvaluationRequest(board.id(), EvaluationType.BOARD, null, 2026)),
                EvaluationSummary.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return created.getBody().id();
    }

    private void addRespondent(String csToken, UUID evaluationId, UUID directorId) {
        ResponseEntity<EvaluationDetail> response = restTemplate.exchange(
                "/api/evaluations/" + evaluationId + "/respondents", HttpMethod.POST,
                authedRequest(csToken, new AddRespondentRequest(directorId, ConfidentialityMode.CONFIDENTIAL)),
                EvaluationDetail.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private void launch(AuthResponse cs, UUID evaluationId) {
        ResponseEntity<EvaluationSummary> launched = restTemplate.exchange(
                "/api/evaluations/" + evaluationId + "/launch", HttpMethod.POST, authedRequest(cs.accessToken()),
                EvaluationSummary.class);
        assertThat(launched.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private void submit(AuthResponse director, UUID evaluationId) {
        ResponseEntity<Void> submitted = restTemplate.exchange(
                "/api/my-evaluations/" + evaluationId + "/submit", HttpMethod.POST,
                authedRequest(director.accessToken()), Void.class);
        assertThat(submitted.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private List<NotificationSummary> notifications(String token) {
        ResponseEntity<NotificationSummary[]> response = restTemplate.exchange(
                "/api/notifications", HttpMethod.GET, authedRequest(token), NotificationSummary[].class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return Arrays.asList(response.getBody());
    }

    private long unreadCount(String token) {
        ResponseEntity<Map<String, Long>> response = restTemplate.exchange(
                "/api/notifications/unread-count", HttpMethod.GET, authedRequest(token),
                new ParameterizedTypeReference<>() { });
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody().get("count");
    }
}
