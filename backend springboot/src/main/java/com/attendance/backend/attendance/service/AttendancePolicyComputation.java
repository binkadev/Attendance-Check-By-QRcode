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
                policy.examBanAbsenceRate(),
                policy.warningAbsentCount(),
                policy.criticalAbsentCount(),
                policy.examBanAbsentCount(),
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
                policy.examBanAbsenceRate(),
                policy.warningAbsentCount(),
                policy.criticalAbsentCount(),
                policy.examBanAbsentCount(),
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
            BigDecimal examBanAbsenceRate,
            Integer warningAbsentCount,
            Integer criticalAbsentCount,
            Integer examBanAbsentCount,
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
                    null,
                    AttendancePolicyStatus.NO_DATA,
                    AttendancePolicyStatus.NO_DATA.name(),
                    "ELIGIBLE",
                    List.of()
            );
        }

        BigDecimal attendanceRate = earnedAttendancePoints
                .multiply(ONE_HUNDRED)
                .divide(BigDecimal.valueOf(eligibleSessionCount), 2, RoundingMode.HALF_UP);

        BigDecimal absenceRate = BigDecimal.valueOf(absentCount)
                .multiply(ONE_HUNDRED)
                .divide(BigDecimal.valueOf(eligibleSessionCount), 2, RoundingMode.HALF_UP);

        List<AttendancePolicyBreachReason> breachReasons = new ArrayList<>();

        boolean criticalByRate = criticalBelowRate != null
                && attendanceRate.compareTo(criticalBelowRate) < 0;

        boolean examBannedByAbsenceRate = examBanAbsenceRate != null
                && absenceRate.compareTo(examBanAbsenceRate) >= 0;

        BigDecimal warningAbsenceRate = absenceRateThresholdForBelowRate(warningBelowRate);
        BigDecimal criticalAbsenceRate = absenceRateThresholdForBelowRate(criticalBelowRate);

        boolean criticalByAbsenceRate = criticalAbsenceRate != null
                && absenceRate.compareTo(criticalAbsenceRate) >= 0;

        boolean warningByAbsenceRate = warningAbsenceRate != null
                && absenceRate.compareTo(warningAbsenceRate) >= 0;

        boolean criticalByAbsentCount = criticalAbsentCount != null
                && absentCount >= criticalAbsentCount;

        boolean examBannedByAbsentCount = examBanAbsentCount != null
                && absentCount >= examBanAbsentCount;

        boolean warningByRate = attendanceRate.compareTo(warningBelowRate) < 0;

        boolean warningByAbsentCount = warningAbsentCount != null
                && absentCount >= warningAbsentCount;

        if (criticalByRate) {
            breachReasons.add(AttendancePolicyBreachReason.RATE_BELOW_CRITICAL);
        } else if (warningByRate) {
            breachReasons.add(AttendancePolicyBreachReason.RATE_BELOW_WARNING);
        }

        if (examBannedByAbsenceRate) {
            breachReasons.add(AttendancePolicyBreachReason.ABSENCE_RATE_EXAM_BANNED);
        } else if (criticalByAbsenceRate) {
            breachReasons.add(AttendancePolicyBreachReason.ABSENCE_RATE_CRITICAL);
        } else if (warningByAbsenceRate) {
            breachReasons.add(AttendancePolicyBreachReason.ABSENCE_RATE_WARNING);
        }

        if (examBannedByAbsentCount) {
            breachReasons.add(AttendancePolicyBreachReason.ABSENT_COUNT_EXAM_BANNED);
        } else if (criticalByAbsentCount) {
            breachReasons.add(AttendancePolicyBreachReason.ABSENT_COUNT_CRITICAL);
        } else if (warningByAbsentCount) {
            breachReasons.add(AttendancePolicyBreachReason.ABSENT_COUNT_WARNING);
        }

        AttendancePolicyStatus status;
        if (examBannedByAbsenceRate || examBannedByAbsentCount) {
            status = AttendancePolicyStatus.EXAM_BANNED;
        } else if (criticalByRate || criticalByAbsenceRate || criticalByAbsentCount) {
            status = AttendancePolicyStatus.CRITICAL;
        } else if (warningByRate || warningByAbsenceRate || warningByAbsentCount) {
            status = AttendancePolicyStatus.WARNING;
        } else {
            status = AttendancePolicyStatus.NORMAL;
        }

        return new ComputedPolicyStatus(
                eligibleSessionCount,
                earnedAttendancePoints.setScale(2, RoundingMode.HALF_UP),
                attendanceRate,
                absenceRate,
                status,
                status.name(),
                examEligibility(status),
                List.copyOf(breachReasons)
        );
    }

    static BigDecimal absenceRateThresholdForBelowRate(BigDecimal belowRate) {
        if (belowRate == null) {
            return null;
        }
        return ONE_HUNDRED.subtract(belowRate).setScale(2, RoundingMode.HALF_UP);
    }

    private static String examEligibility(AttendancePolicyStatus status) {
        return switch (status) {
            case EXAM_BANNED -> "BANNED";
            case WARNING, CRITICAL -> "AT_RISK";
            case NO_DATA, NORMAL -> "ELIGIBLE";
        };
    }

    public record ComputedPolicyStatus(
            long eligibleSessionCount,
            BigDecimal earnedAttendancePoints,
            BigDecimal attendanceRate,
            BigDecimal absenceRate,
            AttendancePolicyStatus policyStatus,
            String riskLevel,
            String examEligibility,
            List<AttendancePolicyBreachReason> breachReasons
    ) {
    }
}
