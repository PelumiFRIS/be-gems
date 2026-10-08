package com.fris.begems.notification;

import com.fris.begems.action.ActionStatus;
import com.fris.begems.action.CorrectiveAction;
import com.fris.begems.action.CorrectiveActionRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Daily digest of overdue corrective actions to each organisation's Company
 * Secretaries and Evaluators. Action owners are free text rather than portal
 * users, so the digest goes to the staff who follow up with them. On Render's free
 * tier the service sleeps when idle, so a run is skipped if nobody has used the
 * app recently — acceptable for a reminder that repeats daily.
 */
@Component
public class OverdueActionDigestJob {

    private static final ZoneId LAGOS = ZoneId.of("Africa/Lagos");

    private final CorrectiveActionRepository correctiveActionRepository;
    private final NotificationService notificationService;

    public OverdueActionDigestJob(CorrectiveActionRepository correctiveActionRepository,
            NotificationService notificationService) {
        this.correctiveActionRepository = correctiveActionRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "${app.notifications.overdue-digest-cron:0 0 8 * * MON-FRI}", zone = "Africa/Lagos")
    @Transactional
    public void sendDigests() {
        Map<UUID, List<CorrectiveAction>> overdueByOrganization = correctiveActionRepository
                .findByStatusNotAndDueDateBefore(ActionStatus.COMPLETED, LocalDate.now(LAGOS)).stream()
                .collect(Collectors.groupingBy(CorrectiveAction::getOrganizationId));
        overdueByOrganization.forEach((organizationId, actions) -> notificationService.notifyActionsOverdue(
                organizationId,
                actions.stream()
                        .sorted(Comparator.comparing(CorrectiveAction::getDueDate))
                        .map(a -> a.getDescription() + " (due " + a.getDueDate()
                                + (a.getOwner() == null ? "" : ", owner: " + a.getOwner()) + ")")
                        .toList()));
    }
}
