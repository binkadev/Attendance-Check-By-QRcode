package com.attendance.backend.notification.api;

import com.attendance.backend.notification.dto.NotificationReadAllResponse;
import com.attendance.backend.notification.dto.NotificationResponse;
import com.attendance.backend.notification.dto.NotificationUnreadCountResponse;
import com.attendance.backend.notification.dto.PageNotificationResponse;
import com.attendance.backend.notification.service.NotificationQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/me/notifications")
public class NotificationMeController {

    private final NotificationQueryService notificationQueryService;

    public NotificationMeController(NotificationQueryService notificationQueryService) {
        this.notificationQueryService = notificationQueryService;
    }

    @GetMapping
    public PageNotificationResponse getMyNotifications(
            Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String severity
    ) {
        UUID currentUserId = UUID.fromString(principal.getName());
        return notificationQueryService.getMyNotifications(
                currentUserId,
                page,
                size,
                unreadOnly,
                type,
                severity
        );
    }

    @GetMapping("/unread-count")
    public NotificationUnreadCountResponse getUnreadCount(Principal principal) {
        UUID currentUserId = UUID.fromString(principal.getName());
        return notificationQueryService.getUnreadCount(currentUserId);
    }

    @PostMapping("/{notificationId}/read")
    public NotificationResponse markRead(
            Principal principal,
            @PathVariable UUID notificationId
    ) {
        UUID currentUserId = UUID.fromString(principal.getName());
        return notificationQueryService.markRead(currentUserId, notificationId);
    }

    @PostMapping("/read-all")
    public NotificationReadAllResponse markReadAll(Principal principal) {
        UUID currentUserId = UUID.fromString(principal.getName());
        return notificationQueryService.markReadAll(currentUserId);
    }
}