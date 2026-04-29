package com.attendance.backend.session.service;

import com.attendance.backend.attendance.repository.AttendanceSessionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class SessionAutoCloseScheduler {

    private final AttendanceSessionRepository attendanceSessionRepository;

    public SessionAutoCloseScheduler(AttendanceSessionRepository attendanceSessionRepository) {
        this.attendanceSessionRepository = attendanceSessionRepository;
    }

    @Scheduled(
            fixedDelayString = "${attendance.session.auto-close.fixed-delay-ms:60000}",
            initialDelayString = "${attendance.session.auto-close.initial-delay-ms:10000}"
    )
    @Transactional
    public void closeExpiredOpenSessions() {
        attendanceSessionRepository.closeExpiredOpenSessions(Instant.now());
    }
}