package com.attendance.backend.attendance.api;

import com.attendance.backend.attendance.service.AttendanceCheckinService;
import com.attendance.backend.common.exception.ApiException;
import com.attendance.backend.domain.enums.AttendanceStatus;
import com.attendance.backend.security.UserPrincipal;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Validated
public class AttendanceQrController {

    private final AttendanceCheckinService checkinService;

    public AttendanceQrController(AttendanceCheckinService checkinService) {
        this.checkinService = checkinService;
    }

    @PostMapping("/sessions/{sessionId}/checkin/qr")
    public ResponseEntity<QrCheckinResponse> checkinQr(
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal UserPrincipal me,
            @RequestBody QrCheckinRequest body,
            HttpServletRequest request
    ) {
        if (me == null) {
            throw ApiException.unauthorized("UNAUTHORIZED", "Missing JWT principal");
        }
        if (body == null || body.token() == null || body.token().isBlank()) {
            throw ApiException.badRequest("QR_TOKEN_REQUIRED", "token is required");
        }

        UUID userId = me.getUserId();

        var cmd = new AttendanceCheckinService.QrCheckinCommand(
                sessionId,
                userId,
                body.token(),
                body.deviceId(),
                extractClientIp(request),
                extractUserAgent(request),
                body.geoLat(),
                body.geoLng(),
                null
        );

        var r = checkinService.qrCheckin(cmd);

        return ResponseEntity.ok(new QrCheckinResponse(
                r.sessionId(),
                r.userId(),
                r.attendanceStatus(),
                r.checkInAt(),
                r.qrTokenId()
        ));
    }

    private String extractUserAgent(HttpServletRequest request) {
        return request == null ? null : request.getHeader("User-Agent");
    }

    private String extractClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            String first = forwardedFor.split(",")[0].trim();
            if (!first.isBlank()) {
                return first;
            }
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        return request.getRemoteAddr();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record QrCheckinRequest(
            String token,
            String deviceId,
            BigDecimal geoLat,
            BigDecimal geoLng
    ) {}

    public record QrCheckinResponse(
            UUID sessionId,
            UUID userId,
            AttendanceStatus attendanceStatus,
            Instant checkInAt,
            String qrTokenId
    ) {}
}