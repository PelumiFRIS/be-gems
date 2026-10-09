package com.fris.begems.notification;

import com.fris.begems.common.ApiException;
import com.fris.begems.director.Director;
import com.fris.begems.director.DirectorRepository;
import com.fris.begems.evaluation.Evaluation;
import com.fris.begems.evaluation.EvaluationRespondent;
import com.fris.begems.framework.EvaluationType;
import com.fris.begems.notification.dto.NotificationSummary;
import com.fris.begems.notification.email.EmailMessage;
import com.fris.begems.organization.Organization;
import com.fris.begems.organization.OrganizationRepository;
import com.fris.begems.security.AppUserPrincipal;
import com.fris.begems.user.Role;
import com.fris.begems.user.User;
import com.fris.begems.user.UserRepository;
import com.fris.begems.user.UserStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Memo §29. Each notify* method writes an in-app notification per recipient and
 * queues a matching email (sent after commit by EmailDispatcher). Disabled
 * accounts are skipped. Account emails never contain a password — the
 * administrator shares the temporary password separately, as in board-portal.
 */
@Service
public class NotificationService {

    private static final int LIST_LIMIT = 30;

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final DirectorRepository directorRepository;
    private final OrganizationRepository organizationRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final String frontendUrl;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository,
            DirectorRepository directorRepository, OrganizationRepository organizationRepository,
            ApplicationEventPublisher eventPublisher, @Value("${app.frontend-url}") String frontendUrl) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.directorRepository = directorRepository;
        this.organizationRepository = organizationRepository;
        this.eventPublisher = eventPublisher;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    // ----- The signed-in user's notification centre -----

    public List<NotificationSummary> listMine(AppUserPrincipal principal) {
        return notificationRepository
                .findByUserIdOrderByCreatedAtDesc(principal.getUserId(), PageRequest.of(0, LIST_LIMIT)).stream()
                .map(NotificationSummary::from)
                .toList();
    }

    public long unreadCount(AppUserPrincipal principal) {
        return notificationRepository.countByUserIdAndReadAtIsNull(principal.getUserId());
    }

    @Transactional
    public NotificationSummary markRead(AppUserPrincipal principal, UUID notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, principal.getUserId())
                .orElseThrow(() -> ApiException.notFound("Notification not found"));
        if (notification.getReadAt() == null) {
            notification.setReadAt(Instant.now());
            notificationRepository.save(notification);
        }
        return NotificationSummary.from(notification);
    }

    @Transactional
    public void markAllRead(AppUserPrincipal principal) {
        notificationRepository.markAllRead(principal.getUserId(), Instant.now());
    }

    // ----- Recipients -----

    /** Portal accounts of the given respondents; directors who haven't been invited yet are left out. */
    public List<User> respondentUsers(Collection<EvaluationRespondent> respondents) {
        List<UUID> userIds = directorRepository.findAllById(
                        respondents.stream().map(EvaluationRespondent::getDirectorId).toList()).stream()
                .map(Director::getUserId)
                .filter(Objects::nonNull)
                .toList();
        return userRepository.findAllById(userIds).stream().filter(this::isActive).toList();
    }

    /** Company Secretaries and Evaluators — the people who run evaluations and track actions. */
    public List<User> evaluationStaff(UUID organizationId) {
        return userRepository.findByOrganizationId(organizationId).stream()
                .filter(u -> u.actsAs(Role.COMPANY_SECRETARY) || u.actsAs(Role.EVALUATOR))
                .filter(this::isActive)
                .toList();
    }

    // ----- Evaluation events -----

    public void notifyEvaluationLaunched(Evaluation evaluation, String subjectName, List<User> respondents) {
        String name = evaluationName(evaluation, subjectName);
        String org = organizationName(evaluation.getOrganizationId());
        for (User user : respondents) {
            notify(user, NotificationType.EVALUATION_INVITATION, "You're invited to complete the " + name,
                    org + " has opened the " + name + ". Please complete your questionnaire.",
                    "/my-evaluations/" + evaluation.getId(),
                    "Dear " + user.getFirstName() + ",\n\n"
                            + org + " has opened the " + name + " and you have been invited to take part.\n\n"
                            + "Please sign in to BE-GEMS to complete your questionnaire. Your answers are saved as "
                            + "you go, so you can return to it at any time before you submit.");
        }
    }

    public void notifyEvaluationReminder(Evaluation evaluation, String subjectName, List<User> respondents) {
        String name = evaluationName(evaluation, subjectName);
        for (User user : respondents) {
            notify(user, NotificationType.EVALUATION_REMINDER, "Reminder: please complete the " + name,
                    "Your response to the " + name + " is still outstanding.",
                    "/my-evaluations/" + evaluation.getId(),
                    "Dear " + user.getFirstName() + ",\n\n"
                            + "This is a friendly reminder that your response to the " + name
                            + " is still outstanding. Please sign in to BE-GEMS to complete and submit it.");
        }
    }

    public void notifySubmissionConfirmed(Evaluation evaluation, String subjectName, User respondent) {
        String name = evaluationName(evaluation, subjectName);
        notify(respondent, NotificationType.SUBMISSION_CONFIRMED, "Thank you — your " + name + " response was received",
                "Your questionnaire has been submitted. No further action is needed.",
                "/my-evaluations",
                "Dear " + respondent.getFirstName() + ",\n\n"
                        + "Thank you for completing the " + name + ". Your response has been received and no "
                        + "further action is needed.");
    }

    public void notifyAllResponsesSubmitted(Evaluation evaluation, String subjectName, int respondentCount) {
        String name = evaluationName(evaluation, subjectName);
        String summary = "All " + respondentCount + " respondents have submitted the " + name
                + ". It is ready to be closed and scored.";
        for (User user : evaluationStaff(evaluation.getOrganizationId())) {
            notify(user, NotificationType.ALL_RESPONSES_SUBMITTED, "All responses received: " + name, summary,
                    "/evaluations/" + evaluation.getId(),
                    "Dear " + user.getFirstName() + ",\n\n" + summary);
        }
    }

    // ----- Board report approval (memo §32) -----

    public void notifyReportAwaitingApproval(Evaluation evaluation, String stageLabel, String actorName,
            List<User> recipients) {
        String name = evaluation.getYear() + " Board Evaluation Report";
        String summary = actorName + " moved the " + name + " to " + stageLabel + ". It is ready for your review.";
        for (User user : recipients) {
            notify(user, NotificationType.REPORT_APPROVAL_REQUIRED, name + ": " + stageLabel, summary,
                    boardReportLink(user, evaluation), "Dear " + user.getFirstName() + ",\n\n" + summary);
        }
    }

    public void notifyReportReturned(Evaluation evaluation, String stageLabel, String actorName, String comment,
            List<User> recipients) {
        String name = evaluation.getYear() + " Board Evaluation Report";
        String summary = actorName + " returned the " + name + " to " + stageLabel + " for changes.";
        for (User user : recipients) {
            notify(user, NotificationType.REPORT_RETURNED, name + " returned for changes", summary + " " + comment,
                    boardReportLink(user, evaluation),
                    "Dear " + user.getFirstName() + ",\n\n" + summary + "\n\nComment: " + comment);
        }
    }

    public void notifyReportFinalised(Evaluation evaluation, List<User> recipients) {
        String name = evaluation.getYear() + " Board Evaluation Report";
        String summary = "The " + name + " has been approved and issued as final.";
        for (User user : recipients) {
            notify(user, NotificationType.REPORT_FINALISED, name + " approved", summary,
                    boardReportLink(user, evaluation),
                    "Dear " + user.getFirstName() + ",\n\n" + summary + " You can read it in BE-GEMS.");
        }
    }

    private static String boardReportLink(User user, Evaluation evaluation) {
        return user.getRole() == Role.DIRECTOR
                ? "/board-reports"
                : "/evaluations/" + evaluation.getId() + "/results";
    }

    // ----- Corrective actions -----

    public void notifyActionClosed(UUID organizationId, UUID closedByUserId, String actionDescription) {
        String closedByName = userRepository.findById(closedByUserId)
                .map(u -> (u.getFirstName() + " " + u.getLastName()).trim())
                .orElse("A colleague");
        String summary = closedByName + " marked a corrective action as completed: " + actionDescription;
        for (User user : evaluationStaff(organizationId)) {
            if (!user.getId().equals(closedByUserId)) {
                notify(user, NotificationType.ACTION_CLOSED, "Corrective action completed", summary, "/actions",
                        "Dear " + user.getFirstName() + ",\n\n" + summary);
            }
        }
    }

    public void notifyActionsOverdue(UUID organizationId, List<String> overdueDescriptions) {
        int count = overdueDescriptions.size();
        String title = count == 1 ? "1 corrective action is overdue" : count + " corrective actions are overdue";
        StringBuilder list = new StringBuilder();
        overdueDescriptions.stream().limit(10).forEach(d -> list.append("  • ").append(d).append('\n'));
        if (count > 10) {
            list.append("  … and ").append(count - 10).append(" more\n");
        }
        for (User user : evaluationStaff(organizationId)) {
            notify(user, NotificationType.ACTIONS_OVERDUE, title,
                    "Review the action register to follow up with the owners.", "/actions",
                    "Dear " + user.getFirstName() + ",\n\n" + title + ":\n\n" + list
                            + "\nPlease review the action register and follow up with the owners.");
        }
    }

    // ----- Account emails (email only — the user can't sign in to see an in-app notice yet) -----

    public void sendAccountCreated(User user) {
        String org = organizationName(user.getOrganizationId());
        email(user, "Your BE-GEMS account for " + org,
                "Dear " + user.getFirstName() + ",\n\n"
                        + "An account has been created for you on BE-GEMS, the board evaluation platform used by "
                        + org + ".\n\n"
                        + "Your administrator will share your temporary password with you separately. Sign in "
                        + "with this email address, then choose a new password from My Account.\n\n"
                        + frontendUrl + "/login");
    }

    public void sendPasswordReset(User user) {
        email(user, "Your BE-GEMS password was reset",
                "Dear " + user.getFirstName() + ",\n\n"
                        + "An administrator has reset your BE-GEMS password. They will share your new temporary "
                        + "password with you separately. Sign in with it, then choose a new password from "
                        + "My Account.\n\n"
                        + "If you did not expect this, please contact your administrator.\n\n"
                        + frontendUrl + "/login");
    }

    private void notify(User user, NotificationType type, String title, String body, String link, String emailText) {
        if (!isActive(user)) {
            return;
        }
        notificationRepository.save(Notification.create(user.getOrganizationId(), user.getId(), type, title, body,
                link));
        email(user, title, emailText + "\n\n" + frontendUrl + link);
    }

    private void email(User user, String subject, String text) {
        eventPublisher.publishEvent(new EmailMessage(user.getEmail(),
                user.getFirstName() + " " + user.getLastName(), subject,
                text + "\n\nBest regards,\nBE-GEMS\n\nThis is an automated message; please do not reply."));
    }

    private boolean isActive(User user) {
        return user.getStatus() == UserStatus.ACTIVE;
    }

    private String organizationName(UUID organizationId) {
        return organizationRepository.findById(organizationId).map(Organization::getName).orElse("Your organisation");
    }

    private static String evaluationName(Evaluation evaluation, String subjectName) {
        return evaluation.getEvaluationType() == EvaluationType.BOARD
                ? evaluation.getYear() + " Board Evaluation"
                : evaluation.getYear() + " Peer Evaluation of " + (subjectName == null ? "a director" : subjectName);
    }
}
