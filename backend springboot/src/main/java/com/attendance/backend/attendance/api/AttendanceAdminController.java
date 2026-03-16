package com.attendance.backend.attendance.api;

import com.attendance.backend.attendance.service.AttendanceAdminService;
import com.attendance.backend.common.exception.ApiException;
import com.attendance.backend.domain.enums.AttendanceStatus;
import com.attendance.backend.domain.enums.CheckInMethod;
import com.attendance.backend.security.UserPrincipal;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sessions")
@Validated
public class AttendanceAdminController {

    private final AttendanceAdminService attendanceAdminService;

    public AttendanceAdminController(AttendanceAdminService attendanceAdminService) {
        this.attendanceAdminService = attendanceAdminService;
    }

    @PostMapping("/{sessionId}/checkin/reopen")
    public ReopenCheckinResponse reopenCheckin(
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal UserPrincipal me,
            @RequestBody(required = false) ReopenCheckinRequest req
    ) {
        if (me == null) {
            throw ApiException.unauthorized("UNAUTHORIZED", "Missing JWT principal");
        }

        Integer openFromNowSeconds = req == null ? null : req.openFromNowSeconds();
        Integer closeFromNowSeconds = req == null ? null : req.closeFromNowSeconds();
        Integer lateAfterMinutes = req == null ? null : req.lateAfterMinutes();
        Integer qrRotateSeconds = req == null ? null : req.qrRotateSeconds();

        var result = attendanceAdminService.reopenCheckin(
                sessionId,
                me.getUserId(),
                openFromNowSeconds,
                closeFromNowSeconds,
                lateAfterMinutes,
                qrRotateSeconds
        );

        return new ReopenCheckinResponse(
                result.sessionId(),
                result.status(),
                result.checkinOpenAt(),
                result.checkinCloseAt(),
                result.lateAfterMinutes(),
                result.qrRotateSeconds()
        );
    }

    @PostMapping("/{sessionId}/attendance/{userId}")
    public AttendanceRecordResponse manualMarkAttendance(
            @PathVariable UUID sessionId,
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserPrincipal me,
            @RequestBody ManualMarkAttendanceRequest req
    ) {
        if (me == null) {
            throw ApiException.unauthorized("UNAUTHORIZED", "Missing JWT principal");
        }
        if (req == null || req.status() == null) {
            throw ApiException.badRequest("MANUAL_STATUS_REQUIRED", "status is required");
        }

        var result = attendanceAdminService.manualMarkAttendance(
                sessionId,
                userId,
                me.getUserId(),
                AttendanceStatus.valueOf(req.status().name()),
                req.note()
        );

        return new AttendanceRecordResponse(
                result.sessionId(),
                result.userId(),
                result.attendanceStatus(),
                result.checkInAt(),
                result.checkInMethod(),
                result.qrTokenId(),
                result.deviceId(),
                result.ipAddress(),
                result.userAgent(),
                result.geoLat(),
                result.geoLng(),
                result.distanceMeter(),
                result.suspiciousFlag(),
                result.suspiciousReason(),
                result.excusedByRequestId(),
                result.createdAt(),
                result.updatedAt()
        );
    }

    @PostMapping("/{sessionId}/attendance/{userId}/reset")
    public ResetAttendanceResponse resetAttendance(
            @PathVariable UUID sessionId,
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserPrincipal me
    ) {
        if (me == null) {
            throw ApiException.unauthorized("UNAUTHORIZED", "Missing JWT principal");
        }

        var result = attendanceAdminService.resetAttendance(
                sessionId,
                userId,
                me.getUserId()
        );

        return new ResetAttendanceResponse(
                result.sessionId(),
                result.userId(),
                result.attendanceStatus(),
                result.checkInAt(),
                result.qrTokenId()
        );
    }

    @GetMapping("/{sessionId}/attendance-events")
    public AttendanceEventsResponse getAttendanceEvents(
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal UserPrincipal me,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) Integer limit
    ) {
        if (me == null) {
            throw ApiException.unauthorized("UNAUTHORIZED", "Missing JWT principal");
        }

        var result = attendanceAdminService.getAttendanceEvents(
                sessionId,
                me.getUserId(),
                userId,
                limit
        );

        List<AttendanceEventItemResponse> items = result.items().stream()
                .map(it -> new AttendanceEventItemResponse(
                        it.id(),
                        it.sessionId(),
                        it.userId(),
                        it.actorUserId(),
                        it.eventType().name(),
                        it.oldStatus() == null ? null : it.oldStatus().name(),
                        it.newStatus() == null ? null : it.newStatus().name(),
                        it.qrTokenId(),
                        it.createdAt(),
                        it.eventPayload()
                ))
                .toList();

        return new AttendanceEventsResponse(result.sessionId(), items);
    }

    public record ReopenCheckinRequest(
            Integer openFromNowSeconds,
            Integer closeFromNowSeconds,
            Integer lateAfterMinutes,
            Integer qrRotateSeconds
    ) {}

    public record ReopenCheckinResponse(
            UUID sessionId,
            String status,
            Instant checkinOpenAt,
            Instant checkinCloseAt,
            Integer lateAfterMinutes,
            Integer qrRotateSeconds
    ) {}

    public enum ManualAttendanceOverrideStatus {
        ABSENT,
        PRESENT,
        LATE
    }

    public record ManualMarkAttendanceRequest(
            ManualAttendanceOverrideStatus status,
            @Size(max = 500) String note
    ) {}

    public record AttendanceRecordResponse(
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

    public record ResetAttendanceResponse(
            UUID sessionId,
            UUID userId,
            String attendanceStatus,
            Instant checkInAt,
            String qrTokenId
    ) {}

    public record AttendanceEventsResponse(
            UUID sessionId,
            List<AttendanceEventItemResponse> items
    ) {}

    public record AttendanceEventItemResponse(
            UUID id,
            UUID sessionId,
            UUID userId,
            UUID actorUserId,
            String eventType,
            String oldStatus,
            String newStatus,
            String qrTokenId,
            Instant createdAt,
            JsonNode eventPayload
    ) {}
}