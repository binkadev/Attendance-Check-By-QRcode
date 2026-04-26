package com.attendance.backend.attendance.api;

import com.attendance.backend.attendance.dto.PageMyAttendanceHistoryResponse;
import com.attendance.backend.attendance.dto.UpcomingSessionResponse;
import com.attendance.backend.attendance.service.AttendanceReadService;
import com.attendance.backend.common.exception.ApiException;
import com.attendance.backend.security.UserPrincipal;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Validated
public class AttendanceReadController {

    private final AttendanceReadService attendanceReadService;

    public AttendanceReadController(AttendanceReadService attendanceReadService) {
        this.attendanceReadService = attendanceReadService;
    }

    @GetMapping("/me/sessions/upcoming")
    public List<UpcomingSessionResponse> listUpcomingSessions(
            @AuthenticationPrincipal UserPrincipal me,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "limit must be >= 1")
            @Max(value = 100, message = "limit must be <= 100")
            int limit
    ) {
        if (me == null) {
            throw ApiException.unauthorized("UNAUTHORIZED", "Missing JWT principal");
        }

        return attendanceReadService.listUpcomingSessions(me.getUserId(), limit);
    }

    @GetMapping("/groups/{groupId}/me/attendances")
    public PageMyAttendanceHistoryResponse listMyAttendancesInGroup(
            @AuthenticationPrincipal UserPrincipal me,
            @PathVariable UUID groupId,
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "page must be >= 0")
            int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "size must be >= 1")
            @Max(value = 200, message = "size must be <= 200")
            int size
    ) {
        if (me == null) {
            throw ApiException.unauthorized("UNAUTHORIZED", "Missing JWT principal");
        }

        return attendanceReadService.listMyAttendancesInGroup(
                me.getUserId(),
                groupId,
                page,
                size
        );
    }

    @GetMapping("/groups/{groupId}/attendance/export")
    public ResponseEntity<byte[]> exportGroupAttendance(
            @AuthenticationPrincipal UserPrincipal me,
            @PathVariable UUID groupId
    ) {
        if (me == null) {
            throw ApiException.unauthorized("UNAUTHORIZED", "Missing JWT principal");
        }

        byte[] file = attendanceReadService.exportGroupAttendance(me.getUserId(), groupId);

        String filename = "attendance-" + groupId + ".xlsx";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .body(file);
    }
}