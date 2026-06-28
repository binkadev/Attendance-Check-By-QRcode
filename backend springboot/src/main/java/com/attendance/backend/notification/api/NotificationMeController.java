package com.attendance.backend.notification.api;

import com.attendance.backend.common.exception.ApiException;
import com.attendance.backend.notification.dto.NotificationReadAllResponse;
import com.attendance.backend.notification.dto.NotificationResponse;
import com.attendance.backend.notification.dto.NotificationUnreadCountResponse;
import com.attendance.backend.notification.dto.PageNotificationResponse;
import com.attendance.backend.notification.service.NotificationQueryService;
import com.attendance.backend.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
            @AuthenticationPrincipal UserPrincipal me,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String severity
    ) {
        UUID currentUserId = currentUserId(me);
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
    public NotificationUnreadCountResponse getUnreadCount(@AuthenticationPrincipal UserPrincipal me) {
        UUID currentUserId = currentUserId(me);
        return notificationQueryService.getUnreadCount(currentUserId);
    }

    @PostMapping("/{notificationId}/read")
    public NotificationResponse markRead(
            @AuthenticationPrincipal UserPrincipal me,
            @PathVariable UUID notificationId
    ) {
        UUID currentUserId = currentUserId(me);
        return notificationQueryService.markRead(currentUserId, notificationId);
    }

    @PostMapping("/read-all")
    public NotificationReadAllResponse markReadAll(@AuthenticationPrincipal UserPrincipal me) {
        UUID currentUserId = currentUserId(me);
        return notificationQueryService.markReadAll(currentUserId);
    }

    private UUID currentUserId(UserPrincipal me) {
        if (me == null || me.getUserId() == null) {
            throw ApiException.unauthorized("UNAUTHORIZED", "Missing JWT principal");
        }
        return me.getUserId();
    }
}
