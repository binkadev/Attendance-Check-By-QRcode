package com.attendance.backend.session.service;

import com.attendance.backend.attendance.repository.AttendanceSessionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class SessionAutoCloseScheduler {

    private final AttendanceSessionRepository attendanceSessionRepository;
    private final SessionServiceImpl sessionService;

    public SessionAutoCloseScheduler(
            AttendanceSessionRepository attendanceSessionRepository,
            SessionServiceImpl sessionService
    ) {
        this.attendanceSessionRepository = attendanceSessionRepository;
        this.sessionService = sessionService;
    }

    @Scheduled(
            fixedDelayString = "${attendance.session.auto-close.fixed-delay-ms:60000}",
            initialDelayString = "${attendance.session.auto-close.initial-delay-ms:10000}"
    )
    @Transactional
    public void closeExpiredOpenSessions() {
        Instant now = Instant.now();
        List<UUID> expiredSessionIds = attendanceSessionRepository.findExpiredOpenSessionIds(now);
        if (expiredSessionIds == null || expiredSessionIds.isEmpty()) {
            return;
        }
        for (UUID sessionId : expiredSessionIds) {
            sessionService.closeExpiredOpenSession(sessionId, now);
        }
    }
}
