package com.attendance.backend.attendance.api;

import com.attendance.backend.attendance.service.AttendancePolicyService;
import com.attendance.backend.common.exception.ApiException;
import com.attendance.backend.domain.enums.AttendancePolicyBreachReason;
import com.attendance.backend.domain.enums.AttendancePolicySource;
import com.attendance.backend.domain.enums.AttendancePolicyStatus;
import com.attendance.backend.security.UserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/groups")
@Validated
public class AttendancePolicyController {

    private final AttendancePolicyService attendancePolicyService;

    public AttendancePolicyController(AttendancePolicyService attendancePolicyService) {
        this.attendancePolicyService = attendancePolicyService;
    }

    @GetMapping("/{groupId}/attendance-policy")
    public AttendancePolicyResponse getGroupPolicy(
            @PathVariable UUID groupId,
            @AuthenticationPrincipal UserPrincipal me
    ) {
        UUID userId = requireUserId(me);
        return toPolicyResponse(attendancePolicyService.getGroupPolicy(groupId, userId));
    }

    @PutMapping("/{groupId}/attendance-policy")
    public AttendancePolicyResponse upsertGroupPolicy(
            @PathVariable UUID groupId,
            @AuthenticationPrincipal UserPrincipal me,
            @Valid @RequestBody UpdateAttendancePolicyRequest req
    ) {
        UUID userId = requireUserId(me);
        return toPolicyResponse(attendancePolicyService.upsertGroupPolicy(
                groupId,
                userId,
                new AttendancePolicyService.UpsertPolicyCommand(
                        req.lateWeight(),
                        req.warningBelowRate(),
                        req.criticalBelowRate(),
                        req.warningAbsentCount(),
                        req.criticalAbsentCount(),
                        req.requireLocation(),
                        req.locationLat(),
                        req.locationLng(),
                        req.allowedRadiusMeter()
                )
        ));
    }

    @DeleteMapping("/{groupId}/attendance-policy")
    public void resetGroupPolicy(
            @PathVariable UUID groupId,
            @AuthenticationPrincipal UserPrincipal me
    ) {
        UUID userId = requireUserId(me);
        attendancePolicyService.resetGroupPolicy(groupId, userId);
    }

    private UUID requireUserId(UserPrincipal me) {
        if (me == null) {
            throw ApiException.unauthorized("UNAUTHORIZED", "Missing JWT principal");
        }
        return me.getUserId();
    }

    private AttendancePolicyResponse toPolicyResponse(AttendancePolicyService.PolicyView v) {
        return new AttendancePolicyResponse(
                v.groupId(),
                v.source(),
                v.lateWeight(),
                v.warningBelowRate(),
                v.criticalBelowRate(),
                v.warningAbsentCount(),
                v.criticalAbsentCount(),
                v.requireLocation(),
                v.locationLat(),
                v.locationLng(),
                v.allowedRadiusMeter(),
                v.excusedHandling(),
                v.sessionScope(),
                v.membershipScope(),
                v.createdAt(),
                v.createdByUserId(),
                v.updatedAt(),
                v.updatedByUserId()
        );
    }

    private AttendancePolicyStudentStatusResponse toStudentStatusResponse(AttendancePolicyService.StudentPolicyStatusView v) {
        return new AttendancePolicyStudentStatusResponse(
                v.userId(),
                v.fullName(),
                v.email(),
                v.joinedAt(),
                v.closedSessionCount(),
                v.eligibleSessionCount(),
                v.presentCount(),
                v.lateCount(),
                v.absentCount(),
                v.excusedCount(),
                v.earnedAttendancePoints(),
                v.attendanceRate(),
                v.policyStatus(),
                v.breachReasons()
        );
    }

    public record UpdateAttendancePolicyRequest(
            @NotNull
            @DecimalMin("0.0")
            @DecimalMax("1.0")
            BigDecimal lateWeight,

            @NotNull
            @DecimalMin("0.0")
            @DecimalMax("100.0")
            BigDecimal warningBelowRate,

            @DecimalMin("0.0")
            @DecimalMax("100.0")
            BigDecimal criticalBelowRate,

            @Positive
            Integer warningAbsentCount,

            @Positive
            Integer criticalAbsentCount,

            Boolean requireLocation,

            @DecimalMin("-90.0")
            @DecimalMax("90.0")
            BigDecimal locationLat,

            @DecimalMin("-180.0")
            @DecimalMax("180.0")
            BigDecimal locationLng,

            @Min(10)
            Integer allowedRadiusMeter
    ) {
    }

    public record AttendancePolicyResponse(
            UUID groupId,
            AttendancePolicySource source,
            BigDecimal lateWeight,
            BigDecimal warningBelowRate,
            BigDecimal criticalBelowRate,
            Integer warningAbsentCount,
            Integer criticalAbsentCount,
            boolean requireLocation,
            BigDecimal locationLat,
            BigDecimal locationLng,
            Integer allowedRadiusMeter,
            String excusedHandling,
            String sessionScope,
            String membershipScope,
            Instant createdAt,
            UUID createdByUserId,
            Instant updatedAt,
            UUID updatedByUserId
    ) {
    }

    public record AttendancePolicyStudentStatusResponse(
            UUID userId,
            String fullName,
            String email,
            Instant joinedAt,
            long closedSessionCount,
            long eligibleSessionCount,
            long presentCount,
            long lateCount,
            long absentCount,
            long excusedCount,
            BigDecimal earnedAttendancePoints,
            BigDecimal attendanceRate,
            AttendancePolicyStatus policyStatus,
            List<AttendancePolicyBreachReason> breachReasons
    ) {
    }

    public record PageAttendancePolicyStudentStatusResponse(
            AttendancePolicyResponse policy,
            List<AttendancePolicyStudentStatusResponse> items,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
    }
}