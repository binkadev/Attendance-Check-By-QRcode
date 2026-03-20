package com.attendance.backend.attendance.service;

import com.attendance.backend.attendance.repository.AttendanceEventRepository;
import com.attendance.backend.attendance.repository.AttendanceSessionRepository;
import com.attendance.backend.attendance.repository.SessionAttendanceRepository;
import com.attendance.backend.common.exception.ApiException;
import com.attendance.backend.domain.entity.AttendanceEvent;
import com.attendance.backend.domain.entity.AttendanceSession;
import com.attendance.backend.domain.entity.SessionAttendance;
import com.attendance.backend.domain.enums.AttendanceStatus;
import com.attendance.backend.domain.enums.CheckInMethod;
import com.attendance.backend.domain.enums.EventType;
import com.attendance.backend.domain.enums.SessionStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AttendanceAdminService {

    private final AttendanceSessionRepository attendanceSessionRepository;
    private final SessionAttendanceRepository sessionAttendanceRepository;
    private final AttendanceEventRepository attendanceEventRepository;
    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final AttendancePolicyNotificationOrchestrator attendancePolicyNotificationOrchestrator;

    public AttendanceAdminService(
            AttendanceSessionRepository attendanceSessionRepository,
            SessionAttendanceRepository sessionAttendanceRepository,
            AttendanceEventRepository attendanceEventRepository,
            EntityManager entityManager,
            ObjectMapper objectMapper,
            Clock clock,
            AttendancePolicyNotificationOrchestrator attendancePolicyNotificationOrchestrator
    ) {
        this.attendanceSessionRepository = attendanceSessionRepository;
        this.sessionAttendanceRepository = sessionAttendanceRepository;
        this.attendanceEventRepository = attendanceEventRepository;
        this.entityManager = entityManager;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.attendancePolicyNotificationOrchestrator = attendancePolicyNotificationOrchestrator;
    }

    @Transactional
    public ReopenCheckinResult reopenCheckin(
            UUID sessionId,
            UUID actorUserId,
            Integer openFromNowSeconds,
            Integer closeFromNowSeconds,
            Integer lateAfterMinutes,
            Integer qrRotateSeconds
    ) {
        Instant now = Instant.now(clock);

        AttendanceSession session = attendanceSessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> ApiException.notFound("SESSION_NOT_FOUND", "Session not found"));

        ensureHostOrCoHost(session.getGroupId(), actorUserId);
        ensureNoOtherOpenSession(session.getGroupId(), sessionId);

        int openOffsetSec = openFromNowSeconds == null ? -60 : openFromNowSeconds;
        int closeOffsetSec = closeFromNowSeconds == null ? 1800 : closeFromNowSeconds;

        if (closeOffsetSec <= openOffsetSec) {
            throw ApiException.badRequest(
                    "CHECKIN_WINDOW_INVALID",
                    "closeFromNowSeconds must be greater than openFromNowSeconds"
            );
        }

        int effectiveLateAfterMinutes =
                lateAfterMinutes == null ? session.getLateAfterMinutes() : lateAfterMinutes;

        int effectiveQrRotateSeconds =
                qrRotateSeconds == null ? session.getQrRotateSeconds() : qrRotateSeconds;

        if (effectiveLateAfterMinutes < 1 || effectiveLateAfterMinutes > 120) {
            throw ApiException.badRequest(
                    "LATE_AFTER_MINUTES_INVALID",
                    "lateAfterMinutes must be between 1 and 120"
            );
        }

        if (effectiveQrRotateSeconds < 5 || effectiveQrRotateSeconds > 120) {
            throw ApiException.badRequest(
                    "QR_ROTATE_SECONDS_INVALID",
                    "qrRotateSeconds must be between 5 and 120"
            );
        }

        Instant newOpenAt = now.plusSeconds(openOffsetSec);
        Instant newCloseAt = now.plusSeconds(closeOffsetSec);

        long effectiveWindowMinutes = Duration.between(newOpenAt, newCloseAt).toMinutes();
        if (effectiveWindowMinutes < 1 || effectiveWindowMinutes > 300) {
            throw ApiException.badRequest(
                    "CHECKIN_WINDOW_INVALID",
                    "effective window must be between 1 and 300 minutes"
            );
        }

        if (effectiveLateAfterMinutes > effectiveWindowMinutes) {
            throw ApiException.badRequest(
                    "LATE_AFTER_MINUTES_INVALID",
                    "lateAfterMinutes cannot be greater than effective window minutes"
            );
        }

        SessionStatus previousStatus = session.getStatus();
        Instant previousOpenAt = session.getCheckinOpenAt();
        Instant previousCloseAt = session.getCheckinCloseAt();
        int previousLateAfterMinutes = session.getLateAfterMinutes();
        int previousQrRotateSeconds = session.getQrRotateSeconds();

        session.setStatus(SessionStatus.OPEN);
        session.setCheckinOpenAt(newOpenAt);
        session.setCheckinCloseAt(newCloseAt);
        session.setLateAfterMinutes(effectiveLateAfterMinutes);
        session.setQrRotateSeconds(effectiveQrRotateSeconds);

        attendanceSessionRepository.saveAndFlush(session);

        AttendanceEvent event = new AttendanceEvent();
        event.id = UUID.randomUUID();
        event.sessionId = sessionId;
        event.userId = actorUserId;
        event.actorUserId = actorUserId;
        event.eventType = EventType.SESSION_OPENED;
        event.oldStatus = null;
        event.newStatus = null;
        event.qrTokenId = null;

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("action", "REOPEN_CHECKIN");
        payload.put("source", "ATTENDANCE_ADMIN_API");
        payload.put("sessionId", sessionId.toString());
        payload.put("actorUserId", actorUserId.toString());
        payload.put("previousSessionStatus", previousStatus.name());
        putIfNotNull(payload, "previousCheckinOpenAt", previousOpenAt);
        putIfNotNull(payload, "previousCheckinCloseAt", previousCloseAt);
        payload.put("previousLateAfterMinutes", previousLateAfterMinutes);
        payload.put("previousQrRotateSeconds", previousQrRotateSeconds);
        payload.put("newCheckinOpenAt", newOpenAt.toString());
        payload.put("newCheckinCloseAt", newCloseAt.toString());
        payload.put("newLateAfterMinutes", effectiveLateAfterMinutes);
        payload.put("newQrRotateSeconds", effectiveQrRotateSeconds);

        event.eventPayload = payload;
        event.createdAt = now;

        attendanceEventRepository.saveAndFlush(event);

        return new ReopenCheckinResult(
                session.getId(),
                session.getStatus().name(),
                session.getCheckinOpenAt(),
                session.getCheckinCloseAt(),
                session.getLateAfterMinutes(),
                session.getQrRotateSeconds()
        );
    }

    @Transactional
    public AttendanceRecordResult manualMarkAttendance(
            UUID sessionId,
            UUID targetUserId,
            UUID actorUserId,
            AttendanceStatus requestedStatus,
            String note
    ) {
        Instant now = Instant.now(clock);

        AttendanceSession session = attendanceSessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> ApiException.notFound("SESSION_NOT_FOUND", "Session not found"));

        ensureHostOrCoHost(session.getGroupId(), actorUserId);
        ensureManualOverrideEnabled(sessionId);

        if (session.getStatus() == SessionStatus.CANCELLED) {
            throw ApiException.conflict(
                    "SESSION_CANCELLED",
                    "Session is CANCELLED"
            );
        }

        if (requestedStatus == null) {
            throw ApiException.badRequest("MANUAL_STATUS_REQUIRED", "status is required");
        }

        if (requestedStatus == AttendanceStatus.EXCUSED) {
            throw ApiException.unprocessable(
                    "EXCUSED_ONLY_VIA_ABSENCE_REQUEST",
                    "EXCUSED can only be set via approved absence request workflow"
            );
        }

        String normalizedNote = normalizeRequiredNote(note);

        SessionAttendance attendance = sessionAttendanceRepository
                .findBySessionAndUserForUpdate(sessionId, targetUserId)
                .orElseThrow(() -> ApiException.notFound(
                        "ATTENDANCE_NOT_FOUND",
                        "Attendance row not found for this session/user"
                ));

        ensureNotExcusedForManualMutation(attendance, "manual override");

        AttendanceStatus oldStatus = attendance.attendanceStatus;
        Instant oldCheckInAt = attendance.checkInAt;
        CheckInMethod oldCheckInMethod = attendance.checkInMethod;
        String oldQrTokenId = attendance.qrTokenId;
        String oldDeviceId = attendance.deviceId;
        String oldIpAddress = attendance.ipAddress;
        String oldUserAgent = attendance.userAgent;
        BigDecimal oldGeoLat = attendance.geoLat;
        BigDecimal oldGeoLng = attendance.geoLng;
        Integer oldDistanceMeter = attendance.distanceMeter;
        boolean oldSuspiciousFlag = attendance.suspiciousFlag;
        String oldSuspiciousReason = attendance.suspiciousReason;
        UUID oldExcusedByRequestId = attendance.excusedByRequestId;

        boolean alreadyNoOp = isManualMarkNoOp(attendance, requestedStatus);
        if (alreadyNoOp) {
            return toAttendanceRecordResult(sessionId, targetUserId, attendance);
        }

        if (requestedStatus == AttendanceStatus.ABSENT) {
            attendance.attendanceStatus = AttendanceStatus.ABSENT;
            attendance.checkInAt = null;
            attendance.checkInMethod = CheckInMethod.MANUAL;
            attendance.qrTokenId = null;
            attendance.deviceId = null;
            attendance.ipAddress = null;
            attendance.userAgent = null;
            attendance.geoLat = null;
            attendance.geoLng = null;
            attendance.distanceMeter = null;
            attendance.suspiciousFlag = false;
            attendance.suspiciousReason = null;
            attendance.excusedByRequestId = null;
        } else {
            attendance.attendanceStatus = requestedStatus;
            attendance.checkInAt = now;
            attendance.checkInMethod = CheckInMethod.MANUAL;
            attendance.qrTokenId = null;
            attendance.deviceId = null;
            attendance.ipAddress = null;
            attendance.userAgent = null;
            attendance.geoLat = null;
            attendance.geoLng = null;
            attendance.distanceMeter = null;
            attendance.suspiciousFlag = false;
            attendance.suspiciousReason = null;
            attendance.excusedByRequestId = null;
        }

        attendance.updatedAt = now;

        sessionAttendanceRepository.saveAndFlush(attendance);

        AttendanceEvent event = new AttendanceEvent();
        event.id = UUID.randomUUID();
        event.sessionId = sessionId;
        event.userId = targetUserId;
        event.actorUserId = actorUserId;
        event.eventType = mapManualMarkEventType(requestedStatus);
        event.oldStatus = oldStatus;
        event.newStatus = attendance.attendanceStatus;
        event.qrTokenId = oldQrTokenId;

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("action", "MANUAL_OVERRIDE");
        payload.put("source", "ATTENDANCE_ADMIN_API");
        payload.put("sessionId", sessionId.toString());
        payload.put("targetUserId", targetUserId.toString());
        payload.put("actorUserId", actorUserId.toString());
        payload.put("oldStatus", oldStatus.name());
        payload.put("newStatus", attendance.attendanceStatus.name());
        payload.put("requestedStatus", requestedStatus.name());
        payload.put("note", normalizedNote);
        putIfNotNull(payload, "oldCheckInAt", oldCheckInAt);
        if (oldCheckInMethod != null) {
            payload.put("oldCheckInMethod", oldCheckInMethod.name());
        }
        putIfNotNull(payload, "oldQrTokenId", oldQrTokenId);
        putIfNotNull(payload, "oldDeviceId", oldDeviceId);
        putIfNotNull(payload, "oldIpAddress", oldIpAddress);
        putIfNotNull(payload, "oldUserAgent", oldUserAgent);
        if (oldGeoLat != null) {
            payload.put("oldGeoLat", oldGeoLat);
        }
        if (oldGeoLng != null) {
            payload.put("oldGeoLng", oldGeoLng);
        }
        if (oldDistanceMeter != null) {
            payload.put("oldDistanceMeter", oldDistanceMeter);
        }
        payload.put("oldSuspiciousFlag", oldSuspiciousFlag);
        putIfNotNull(payload, "oldSuspiciousReason", oldSuspiciousReason);
        if (oldExcusedByRequestId != null) {
            payload.put("oldExcusedByRequestId", oldExcusedByRequestId.toString());
        }

        event.eventPayload = payload;
        event.createdAt = now;

        attendanceEventRepository.saveAndFlush(event);

        if (session.getStatus() == SessionStatus.CLOSED) {
            attendancePolicyNotificationOrchestrator.reevaluateOne(
                    session.getGroupId(),
                    targetUserId,
                    sessionId
            );
        }

        return toAttendanceRecordResult(sessionId, targetUserId, attendance);
    }

    @Transactional
    public ResetAttendanceResult resetAttendance(
            UUID sessionId,
            UUID targetUserId,
            UUID actorUserId
    ) {
        Instant now = Instant.now(clock);

        AttendanceSession session = attendanceSessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> ApiException.notFound("SESSION_NOT_FOUND", "Session not found"));

        ensureHostOrCoHost(session.getGroupId(), actorUserId);
        ensureManualOverrideEnabled(sessionId);

        if (session.getStatus() == SessionStatus.CANCELLED) {
            throw ApiException.conflict(
                    "SESSION_CANCELLED",
                    "Session is CANCELLED"
            );
        }

        SessionAttendance attendance = sessionAttendanceRepository
                .findBySessionAndUserForUpdate(sessionId, targetUserId)
                .orElseThrow(() -> ApiException.notFound(
                        "ATTENDANCE_NOT_FOUND",
                        "Attendance row not found for this session/user"
                ));

        ensureNotExcusedForManualMutation(attendance, "manual reset");

        AttendanceStatus oldStatus = attendance.attendanceStatus;
        Instant oldCheckInAt = attendance.checkInAt;
        CheckInMethod oldCheckInMethod = attendance.checkInMethod;
        String oldQrTokenId = attendance.qrTokenId;
        String oldDeviceId = attendance.deviceId;
        String oldIpAddress = attendance.ipAddress;
        String oldUserAgent = attendance.userAgent;
        BigDecimal oldGeoLat = attendance.geoLat;
        BigDecimal oldGeoLng = attendance.geoLng;
        Integer oldDistanceMeter = attendance.distanceMeter;
        boolean oldSuspiciousFlag = attendance.suspiciousFlag;
        String oldSuspiciousReason = attendance.suspiciousReason;
        UUID oldExcusedByRequestId = attendance.excusedByRequestId;

        boolean alreadyClean =
                attendance.attendanceStatus == AttendanceStatus.ABSENT &&
                        attendance.checkInAt == null &&
                        attendance.checkInMethod == CheckInMethod.QR &&
                        attendance.qrTokenId == null &&
                        attendance.deviceId == null &&
                        attendance.ipAddress == null &&
                        attendance.userAgent == null &&
                        attendance.geoLat == null &&
                        attendance.geoLng == null &&
                        attendance.distanceMeter == null &&
                        !attendance.suspiciousFlag &&
                        attendance.suspiciousReason == null &&
                        attendance.excusedByRequestId == null;

        if (alreadyClean) {
            return new ResetAttendanceResult(
                    sessionId,
                    targetUserId,
                    attendance.attendanceStatus.name(),
                    attendance.checkInAt,
                    attendance.qrTokenId
            );
        }

        attendance.attendanceStatus = AttendanceStatus.ABSENT;
        attendance.checkInAt = null;
        attendance.checkInMethod = CheckInMethod.QR;
        attendance.qrTokenId = null;
        attendance.deviceId = null;
        attendance.ipAddress = null;
        attendance.userAgent = null;
        attendance.geoLat = null;
        attendance.geoLng = null;
        attendance.distanceMeter = null;
        attendance.suspiciousFlag = false;
        attendance.suspiciousReason = null;
        attendance.excusedByRequestId = null;
        attendance.updatedAt = now;

        sessionAttendanceRepository.saveAndFlush(attendance);

        AttendanceEvent event = new AttendanceEvent();
        event.id = UUID.randomUUID();
        event.sessionId = sessionId;
        event.userId = targetUserId;
        event.actorUserId = actorUserId;
        event.eventType = EventType.MARK_MANUAL_ABSENT;
        event.oldStatus = oldStatus;
        event.newStatus = AttendanceStatus.ABSENT;
        event.qrTokenId = oldQrTokenId;

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("action", "RESET_ATTENDANCE");
        payload.put("reason", "RESET_API");
        payload.put("source", "ATTENDANCE_ADMIN_API");
        payload.put("sessionId", sessionId.toString());
        payload.put("targetUserId", targetUserId.toString());
        payload.put("actorUserId", actorUserId.toString());
        payload.put("oldStatus", oldStatus.name());
        payload.put("newStatus", AttendanceStatus.ABSENT.name());
        putIfNotNull(payload, "oldCheckInAt", oldCheckInAt);
        if (oldCheckInMethod != null) {
            payload.put("oldCheckInMethod", oldCheckInMethod.name());
        }
        putIfNotNull(payload, "oldQrTokenId", oldQrTokenId);
        putIfNotNull(payload, "oldDeviceId", oldDeviceId);
        putIfNotNull(payload, "oldIpAddress", oldIpAddress);
        putIfNotNull(payload, "oldUserAgent", oldUserAgent);
        if (oldGeoLat != null) {
            payload.put("oldGeoLat", oldGeoLat);
        }
        if (oldGeoLng != null) {
            payload.put("oldGeoLng", oldGeoLng);
        }
        if (oldDistanceMeter != null) {
            payload.put("oldDistanceMeter", oldDistanceMeter);
        }
        payload.put("oldSuspiciousFlag", oldSuspiciousFlag);
        putIfNotNull(payload, "oldSuspiciousReason", oldSuspiciousReason);
        if (oldExcusedByRequestId != null) {
            payload.put("oldExcusedByRequestId", oldExcusedByRequestId.toString());
        }

        event.eventPayload = payload;
        event.createdAt = now;

        attendanceEventRepository.saveAndFlush(event);

        if (session.getStatus() == SessionStatus.CLOSED) {
            attendancePolicyNotificationOrchestrator.reevaluateOne(
                    session.getGroupId(),
                    targetUserId,
                    sessionId
            );
        }

        return new ResetAttendanceResult(
                sessionId,
                targetUserId,
                attendance.attendanceStatus.name(),
                attendance.checkInAt,
                attendance.qrTokenId
        );
    }

    @Transactional(readOnly = true)
    public AttendanceEventsResult getAttendanceEvents(
            UUID sessionId,
            UUID actorUserId,
            UUID targetUserId,
            Integer limit
    ) {
        AttendanceSession session = attendanceSessionRepository.findById(sessionId)
                .orElseThrow(() -> ApiException.notFound("SESSION_NOT_FOUND", "Session not found"));

        ensureHostOrCoHost(session.getGroupId(), actorUserId);

        int effectiveLimit = limit == null ? 50 : limit;
        if (effectiveLimit < 1 || effectiveLimit > 200) {
            throw ApiException.badRequest("LIMIT_INVALID", "limit must be between 1 and 200");
        }

        var pageable = PageRequest.of(0, effectiveLimit);

        List<AttendanceEvent> rows = (targetUserId == null)
                ? attendanceEventRepository.findBySessionIdOrderByCreatedAtDesc(sessionId, pageable)
                : attendanceEventRepository.findBySessionIdAndUserIdOrderByCreatedAtDesc(sessionId, targetUserId, pageable);

        List<AttendanceEventItem> items = rows.stream()
                .map(e -> new AttendanceEventItem(
                        e.id,
                        e.sessionId,
                        e.userId,
                        e.actorUserId,
                        e.eventType,
                        e.oldStatus,
                        e.newStatus,
                        e.qrTokenId,
                        e.createdAt,
                        e.eventPayload
                ))
                .toList();

        return new AttendanceEventsResult(sessionId, items);
    }

    private void ensureManualOverrideEnabled(UUID sessionId) {
        Number flag = (Number) entityManager.createNativeQuery("""
            select s.allow_manual_override
            from attendance_sessions s
            where s.id = UUID_TO_BIN(:sessionId, 1)
              and s.deleted_at is null
        """)
                .setParameter("sessionId", sessionId.toString())
                .getSingleResult();

        if (flag == null || flag.intValue() != 1) {
            throw ApiException.conflict(
                    "MANUAL_OVERRIDE_DISABLED",
                    "Manual attendance override is disabled for this session"
            );
        }
    }

    private void ensureNotExcusedForManualMutation(SessionAttendance attendance, String actionName) {
        if (attendance.attendanceStatus == AttendanceStatus.EXCUSED || attendance.excusedByRequestId != null) {
            throw ApiException.conflict(
                    "EXCUSED_MUST_BE_REVERTED_VIA_ABSENCE_WORKFLOW",
                    "Excused attendance must be reverted via absence request workflow before " + actionName
            );
        }
    }

    private String normalizeRequiredNote(String note) {
        String normalized = note == null ? "" : note.trim();

        if (normalized.length() < 3) {
            throw ApiException.unprocessable(
                    "MANUAL_OVERRIDE_NOTE_REQUIRED",
                    "note must be at least 3 characters"
            );
        }

        if (normalized.length() > 500) {
            throw ApiException.unprocessable(
                    "MANUAL_OVERRIDE_NOTE_TOO_LONG",
                    "note must be at most 500 characters"
            );
        }

        return normalized;
    }

    private boolean isManualMarkNoOp(SessionAttendance attendance, AttendanceStatus requestedStatus) {
        if (requestedStatus == AttendanceStatus.ABSENT) {
            return attendance.attendanceStatus == AttendanceStatus.ABSENT
                    && attendance.checkInAt == null
                    && attendance.qrTokenId == null
                    && attendance.deviceId == null
                    && attendance.ipAddress == null
                    && attendance.userAgent == null
                    && attendance.geoLat == null
                    && attendance.geoLng == null
                    && attendance.distanceMeter == null
                    && !attendance.suspiciousFlag
                    && attendance.suspiciousReason == null
                    && attendance.excusedByRequestId == null;
        }

        return attendance.attendanceStatus == requestedStatus
                && attendance.checkInAt != null
                && attendance.checkInMethod == CheckInMethod.MANUAL
                && attendance.qrTokenId == null
                && attendance.deviceId == null
                && attendance.ipAddress == null
                && attendance.userAgent == null
                && attendance.geoLat == null
                && attendance.geoLng == null
                && attendance.distanceMeter == null
                && !attendance.suspiciousFlag
                && attendance.suspiciousReason == null
                && attendance.excusedByRequestId == null;
    }

    private EventType mapManualMarkEventType(AttendanceStatus requestedStatus) {
        return switch (requestedStatus) {
            case PRESENT -> EventType.MARK_MANUAL_PRESENT;
            case LATE -> EventType.MARK_MANUAL_LATE;
            case ABSENT -> EventType.MARK_MANUAL_ABSENT;
            case EXCUSED -> throw ApiException.unprocessable(
                    "EXCUSED_ONLY_VIA_ABSENCE_REQUEST",
                    "EXCUSED can only be set via approved absence request workflow"
            );
        };
    }

    private AttendanceRecordResult toAttendanceRecordResult(
            UUID sessionId,
            UUID userId,
            SessionAttendance attendance
    ) {
        return new AttendanceRecordResult(
                sessionId,
                userId,
                attendance.attendanceStatus,
                attendance.checkInAt,
                attendance.checkInMethod,
                attendance.qrTokenId,
                attendance.deviceId,
                attendance.ipAddress,
                attendance.userAgent,
                attendance.geoLat,
                attendance.geoLng,
                attendance.distanceMeter,
                attendance.suspiciousFlag,
                attendance.suspiciousReason,
                attendance.excusedByRequestId,
                attendance.createdAt,
                attendance.updatedAt
        );
    }

    private void ensureHostOrCoHost(UUID groupId, UUID actorUserId) {
        Number cnt = (Number) entityManager.createNativeQuery("""
            select count(*)
            from group_members gm
            where gm.group_id = UUID_TO_BIN(:groupId, 1)
              and gm.user_id  = UUID_TO_BIN(:userId, 1)
              and gm.member_status = 'APPROVED'
              and gm.role in ('OWNER','CO_HOST')
        """)
                .setParameter("groupId", groupId.toString())
                .setParameter("userId", actorUserId.toString())
                .getSingleResult();

        if (cnt == null || cnt.longValue() == 0L) {
            throw ApiException.forbidden("FORBIDDEN", "Only OWNER/CO_HOST can perform this action");
        }
    }

    private void ensureNoOtherOpenSession(UUID groupId, UUID currentSessionId) {
        Number cnt = (Number) entityManager.createNativeQuery("""
            select count(*)
            from attendance_sessions s
            where s.group_id = UUID_TO_BIN(:groupId, 1)
              and s.id <> UUID_TO_BIN(:sessionId, 1)
              and s.status = 'OPEN'
              and s.deleted_at is null
        """)
                .setParameter("groupId", groupId.toString())
                .setParameter("sessionId", currentSessionId.toString())
                .getSingleResult();

        if (cnt != null && cnt.longValue() > 0L) {
            throw ApiException.conflict(
                    "ANOTHER_SESSION_ALREADY_OPEN",
                    "Another OPEN session already exists in this group"
            );
        }
    }

    private void putIfNotNull(ObjectNode node, String key, Instant value) {
        if (value != null) {
            node.put(key, value.toString());
        }
    }

    private void putIfNotNull(ObjectNode node, String key, String value) {
        if (value != null && !value.isBlank()) {
            node.put(key, value);
        }
    }

    public record ReopenCheckinResult(
            UUID sessionId,
            String status,
            Instant checkinOpenAt,
            Instant checkinCloseAt,
            Integer lateAfterMinutes,
            Integer qrRotateSeconds
    ) {}

    public record ResetAttendanceResult(
            UUID sessionId,
            UUID userId,
            String attendanceStatus,
            Instant checkInAt,
            String qrTokenId
    ) {}

    public record AttendanceEventsResult(
            UUID sessionId,
            List<AttendanceEventItem> items
    ) {}

    public record AttendanceEventItem(
            UUID id,
            UUID sessionId,
            UUID userId,
            UUID actorUserId,
            EventType eventType,
            AttendanceStatus oldStatus,
            AttendanceStatus newStatus,
            String qrTokenId,
            Instant createdAt,
            com.fasterxml.jackson.databind.JsonNode eventPayload
    ) {}

    public record AttendanceRecordResult(
            UUID sessionId,
            UUID userId,
            AttendanceStatus attendanceStatus,
            Instant checkInAt,
            CheckInMethod checkInMethod,
            String qrTokenId,
            String deviceId,
            String ipAddress,
            String userAgent,
            BigDecimal geoLat,
            BigDecimal geoLng,
            Integer distanceMeter,
            boolean suspiciousFlag,
            String suspiciousReason,
            UUID excusedByRequestId,
            Instant createdAt,
            Instant updatedAt
    ) {}
}