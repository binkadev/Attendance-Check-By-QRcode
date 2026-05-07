package com.attendance.backend.stats.api;

import com.attendance.backend.common.exception.ApiException;
import com.attendance.backend.security.UserPrincipal;
import com.attendance.backend.stats.dto.AttendanceSummaryResponse;
import com.attendance.backend.stats.dto.GroupAttendanceSummaryPageResponse;
import com.attendance.backend.stats.service.AttendanceStatsService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Validated
public class AttendanceStatsController {

    private final AttendanceStatsService attendanceStatsService;

    public AttendanceStatsController(AttendanceStatsService attendanceStatsService) {
        this.attendanceStatsService = attendanceStatsService;
    }

    @GetMapping("/me/attendance/summary")
    public AttendanceSummaryResponse getMyAttendanceSummary(
            @AuthenticationPrincipal UserPrincipal me,
            @RequestParam(required = false)
            @Size(max = 30, message = "semester must be <= 30 characters")
            String semester,
            @RequestParam(required = false)
            @Size(max = 30, message = "academicYear must be <= 30 characters")
            String academicYear
    ) {
        if (me == null || me.getUserId() == null) {
            throw ApiException.unauthorized("UNAUTHORIZED", "Missing JWT principal");
        }

        return attendanceStatsService.getMyAttendanceSummary(
                me.getUserId(),
                semester,
                academicYear
        );
    }

    @GetMapping("/groups/{groupId}/attendance/summary")
    public GroupAttendanceSummaryPageResponse getGroupAttendanceSummary(
            @PathVariable UUID groupId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size
    ) {
        if (principal == null || principal.getUserId() == null) {
            throw ApiException.unauthorized("UNAUTHORIZED", "Unauthorized");
        }

        return attendanceStatsService.getGroupSummary(groupId, principal.getUserId(), page, size);
    }
}