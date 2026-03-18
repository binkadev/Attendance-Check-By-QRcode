package com.attendance.backend.attendance.api;

import com.attendance.backend.domain.enums.AttendancePolicyBreachReason;
import com.attendance.backend.domain.enums.AttendancePolicySource;
import com.attendance.backend.domain.enums.AttendancePolicyStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AttendancePolicyQueryDtos {

    private AttendancePolicyQueryDtos() {
    }

    public record AttendancePolicyView(
            UUID groupId,
            AttendancePolicySource source,
            BigDecimal lateWeight,
            BigDecimal warningBelowRate,
            BigDecimal criticalBelowRate,
            Integer warningAbsentCount,
            Integer criticalAbsentCount,
            String excusedHandling,
            String sessionScope,
            String membershipScope,
            Instant createdAt,
            UUID createdByUserId,
            Instant updatedAt,
            UUID updatedByUserId
    ) {
    }

    public record AttendancePolicyStudentStatusView(
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

    public record AttendancePolicyStudentsPageResponse(
            AttendancePolicyView policy,
            List<AttendancePolicyStudentStatusView> items,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
    }

    public record MyAttendancePolicyStatusResponse(
            UUID groupId,
            String groupName,
            AttendancePolicyView policy,
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
}