package com.attendance.backend.attendance.service;

import com.attendance.backend.attendance.api.AttendancePolicyQueryDtos;
import com.attendance.backend.domain.enums.AttendancePolicyBreachReason;
import com.attendance.backend.domain.enums.AttendancePolicyStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public final class AttendancePolicyComputation {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private AttendancePolicyComputation() {
    }

    public static ComputedPolicyStatus compute(
            AttendancePolicyQueryDtos.AttendancePolicyView policy,
            long presentCount,
            long lateCount,
            long absentCount,
            long excusedCount
    ) {
        long eligibleSessionCount = presentCount + lateCount + absentCount;
        BigDecimal earnedAttendancePoints = BigDecimal.valueOf(presentCount)
                .add(policy.lateWeight().multiply(BigDecimal.valueOf(lateCount)));

        if (eligibleSessionCount <= 0) {
            return new ComputedPolicyStatus(
                    eligibleSessionCount,
                    earnedAttendancePoints.setScale(2, RoundingMode.HALF_UP),
                    null,
                    AttendancePolicyStatus.NO_DATA,
                    List.of()
            );
        }

        BigDecimal attendanceRate = earnedAttendancePoints
                .multiply(ONE_HUNDRED)
                .divide(BigDecimal.valueOf(eligibleSessionCount), 2, RoundingMode.HALF_UP);

        List<AttendancePolicyBreachReason> breachReasons = new ArrayList<>();

        boolean criticalByRate = policy.criticalBelowRate() != null
                && attendanceRate.compareTo(policy.criticalBelowRate()) < 0;
        boolean criticalByAbsentCount = policy.criticalAbsentCount() != null
                && absentCount >= policy.criticalAbsentCount();

        boolean warningByRate = attendanceRate.compareTo(policy.warningBelowRate()) < 0;
        boolean warningByAbsentCount = policy.warningAbsentCount() != null
                && absentCount >= policy.warningAbsentCount();

        if (criticalByRate) {
            breachReasons.add(AttendancePolicyBreachReason.RATE_BELOW_CRITICAL);
        } else if (warningByRate) {
            breachReasons.add(AttendancePolicyBreachReason.RATE_BELOW_WARNING);
        }

        if (criticalByAbsentCount) {
            breachReasons.add(AttendancePolicyBreachReason.ABSENT_COUNT_CRITICAL);
        } else if (warningByAbsentCount) {
            breachReasons.add(AttendancePolicyBreachReason.ABSENT_COUNT_WARNING);
        }

        AttendancePolicyStatus status;
        if (criticalByRate || criticalByAbsentCount) {
            status = AttendancePolicyStatus.CRITICAL;
        } else if (warningByRate || warningByAbsentCount) {
            status = AttendancePolicyStatus.WARNING;
        } else {
            status = AttendancePolicyStatus.NORMAL;
        }

        return new ComputedPolicyStatus(
                eligibleSessionCount,
                earnedAttendancePoints.setScale(2, RoundingMode.HALF_UP),
                attendanceRate,
                status,
                List.copyOf(breachReasons)
        );
    }

    public record ComputedPolicyStatus(
            long eligibleSessionCount,
            BigDecimal earnedAttendancePoints,
            BigDecimal attendanceRate,
            AttendancePolicyStatus policyStatus,
            List<AttendancePolicyBreachReason> breachReasons
    ) {
    }
}