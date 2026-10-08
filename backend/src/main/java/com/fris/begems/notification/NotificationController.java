package com.fris.begems.notification;

import com.fris.begems.notification.dto.NotificationSummary;
import com.fris.begems.security.AppUserPrincipal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationSummary> list(@AuthenticationPrincipal AppUserPrincipal principal) {
        return notificationService.listMine(principal);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(@AuthenticationPrincipal AppUserPrincipal principal) {
        return Map.of("count", notificationService.unreadCount(principal));
    }

    @PostMapping("/{id}/read")
    public NotificationSummary markRead(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id) {
        return notificationService.markRead(principal, id);
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal AppUserPrincipal principal) {
        notificationService.markAllRead(principal);
        return ResponseEntity.noContent().build();
    }
}
