package com.attendance.backend.notification.service.scheduler;

import com.attendance.backend.notification.service.NotificationDispatchService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotificationDispatchScheduler {

    private final NotificationDispatchService dispatchService;

    public NotificationDispatchScheduler(NotificationDispatchService dispatchService) {
        this.dispatchService = dispatchService;
    }

    @Scheduled(fixedDelayString = "${app.notification.dispatch-fixed-delay-ms:15000}")
    public void dispatchEmailDeliveries() {
        dispatchService.dispatchDueEmailDeliveries(20);
    }
}