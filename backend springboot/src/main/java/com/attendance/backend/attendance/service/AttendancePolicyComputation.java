package com.attendance.backend.attendance.service;

import com.attendance.backend.attendance.dto.AttendancePolicyQueryDtos;
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
        return computeInternal(
                policy.lateWeight(),
                policy.warningBelowRate(),
                policy.criticalBelowRate(),
                policy.warningAbsentCount(),
                policy.criticalAbsentCount(),
                presentCount,
                lateCount,
                absentCount,
                excusedCount
        );
    }

    public static ComputedPolicyStatus compute(
            AttendancePolicyService.EffectivePolicy policy,
            long presentCount,
            long lateCount,
            long absentCount,
            long excusedCount
    ) {
        return computeInternal(
                policy.lateWeight(),
                policy.warningBelowRate(),
                policy.criticalBelowRate(),
                policy.warningAbsentCount(),
                policy.criticalAbsentCount(),
                presentCount,
                lateCount,
                absentCount,
                excusedCount
        );
    }

    private static ComputedPolicyStatus computeInternal(
            BigDecimal lateWeight,
            BigDecimal warningBelowRate,
            BigDecimal criticalBelowRate,
            Integer warningAbsentCount,
            Integer criticalAbsentCount,
            long presentCount,
            long lateCount,
            long absentCount,
            long excusedCount
    ) {
        long eligibleSessionCount = presentCount + lateCount + absentCount;

        BigDecimal earnedAttendancePoints = BigDecimal.valueOf(presentCount)
                .add(lateWeight.multiply(BigDecimal.valueOf(lateCount)));

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

        boolean criticalByRate = criticalBelowRate != null
                && attendanceRate.compareTo(criticalBelowRate) < 0;

        boolean criticalByAbsentCount = criticalAbsentCount != null
                && absentCount >= criticalAbsentCount;

        boolean warningByRate = attendanceRate.compareTo(warningBelowRate) < 0;

        boolean warningByAbsentCount = warningAbsentCount != null
                && absentCount >= warningAbsentCount;

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