package com.attendance.backend.notification.api;

import com.attendance.backend.notification.dto.PageNotificationResponse;
import com.attendance.backend.notification.service.NotificationQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/groups")
public class NotificationLecturerController {

    private final NotificationQueryService notificationQueryService;

    public NotificationLecturerController(NotificationQueryService notificationQueryService) {
        this.notificationQueryService = notificationQueryService;
    }

    @GetMapping("/{groupId}/notifications")
    public PageNotificationResponse getGroupNotifications(
            Principal principal,
            @PathVariable UUID groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        UUID currentUserId = UUID.fromString(principal.getName());
        return notificationQueryService.getLecturerGroupNotifications(currentUserId, groupId, page, size);
    }
}