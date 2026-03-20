package com.attendance.backend.notification.api;

import com.attendance.backend.notification.dto.PageNotificationDeliveryAdminResponse;
import com.attendance.backend.notification.service.NotificationDispatchService;
import com.attendance.backend.notification.service.NotificationQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/notification-deliveries")
public class NotificationAdminController {

    private final NotificationQueryService notificationQueryService;
    private final NotificationDispatchService notificationDispatchService;

    public NotificationAdminController(
            NotificationQueryService notificationQueryService,
            NotificationDispatchService notificationDispatchService
    ) {
        this.notificationQueryService = notificationQueryService;
        this.notificationDispatchService = notificationDispatchService;
    }

    @GetMapping
    public PageNotificationDeliveryAdminResponse getDeliveries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "EMAIL") String channel,
            @RequestParam(required = false) Set<String> status
    ) {
        return notificationQueryService.getAdminDeliveries(page, size, channel, status);
    }

    @PostMapping("/{deliveryId}/retry")
    public void retryDelivery(@PathVariable UUID deliveryId) {
        notificationDispatchService.retryDelivery(deliveryId);
    }
}