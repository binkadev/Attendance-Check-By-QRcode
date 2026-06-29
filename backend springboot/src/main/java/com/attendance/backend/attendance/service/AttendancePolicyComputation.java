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
        return compute(policy, presentCount, lateCount, absentCount, excusedCount, 0L, null);
    }

    public static ComputedPolicyStatus compute(
            AttendancePolicyQueryDtos.AttendancePolicyView policy,
            long presentCount,
            long lateCount,
            long absentCount,
            long excusedCount,
            long thresholdBaseSessionCount,
            Integer maxAllowedAbsences
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
                excusedCount,
                thresholdBaseSessionCount,
                maxAllowedAbsences
        );
    }

    public static ComputedPolicyStatus compute(
            AttendancePolicyService.EffectivePolicy policy,
            long presentCount,
            long lateCount,
            long absentCount,
            long excusedCount
    ) {
        return compute(policy, presentCount, lateCount, absentCount, excusedCount, 0L, null);
    }

    public static ComputedPolicyStatus compute(
            AttendancePolicyService.EffectivePolicy policy,
            long presentCount,
            long lateCount,
            long absentCount,
            long excusedCount,
            long thresholdBaseSessionCount,
            Integer maxAllowedAbsences
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
                excusedCount,
                thresholdBaseSessionCount,
                maxAllowedAbsences
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
            long excusedCount,
            long thresholdBaseSessionCount,
            Integer maxAllowedAbsences
    ) {
        long eligibleSessionCount = presentCount + lateCount + absentCount;
        long effectiveThresholdBaseSessionCount = thresholdBaseSessionCount > 0
                ? thresholdBaseSessionCount
                : eligibleSessionCount;

        BigDecimal warningAbsenceRate = absenceRateThresholdForBelowRate(warningBelowRate);
        BigDecimal criticalAbsenceRate = absenceRateThresholdForBelowRate(criticalBelowRate);

        Integer computedWarningAbsentCount = resolveEffectiveAbsentCount(
                warningAbsentCount,
                effectiveThresholdBaseSessionCount,
                warningAbsenceRate
        );
        Integer computedCriticalAbsentCount = resolveEffectiveAbsentCount(
                criticalAbsentCount,
                effectiveThresholdBaseSessionCount,
                criticalAbsenceRate
        );
        Integer computedExamBanAbsentCount = resolveEffectiveExamBanAbsentCount(
                examBanAbsentCount,
                maxAllowedAbsences,
                effectiveThresholdBaseSessionCount,
                examBanAbsenceRate
        );

        BigDecimal earnedAttendancePoints = BigDecimal.valueOf(presentCount)
                .add(lateWeight.multiply(BigDecimal.valueOf(lateCount)));

        if (eligibleSessionCount <= 0) {
            return new ComputedPolicyStatus(
                    eligibleSessionCount,
                    effectiveThresholdBaseSessionCount,
                    maxAllowedAbsences,
                    earnedAttendancePoints.setScale(2, RoundingMode.HALF_UP),
                    null,
                    null,
                    AttendancePolicyStatus.NO_DATA,
                    AttendancePolicyStatus.NO_DATA.name(),
                    "ELIGIBLE",
                    computedWarningAbsentCount,
                    computedCriticalAbsentCount,
                    computedExamBanAbsentCount,
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

        boolean criticalByAbsenceRate = criticalAbsenceRate != null
                && absenceRate.compareTo(criticalAbsenceRate) >= 0;

        boolean warningByAbsenceRate = warningAbsenceRate != null
                && absenceRate.compareTo(warningAbsenceRate) >= 0;

        boolean criticalByAbsentCount = computedCriticalAbsentCount != null
                && absentCount >= computedCriticalAbsentCount;

        boolean examBannedByAbsentCount = computedExamBanAbsentCount != null
                && absentCount >= computedExamBanAbsentCount;

        boolean warningByRate = attendanceRate.compareTo(warningBelowRate) < 0;

        boolean warningByAbsentCount = computedWarningAbsentCount != null
                && absentCount >= computedWarningAbsentCount;

        if (criticalByRate) {
            breachReasons.add(AttendancePolicyBreachReason.RATE_BELOW_CRITICAL);
        } else if (warningByRate) {
            breachReasons.add(AttendancePolicyBreachReason.RATE_BELOW_WARNING);
        }

        if (examBannedByAbsentCount) {
            breachReasons.add(AttendancePolicyBreachReason.ABSENT_COUNT_EXAM_BANNED);
        } else if (criticalByAbsenceRate) {
            breachReasons.add(AttendancePolicyBreachReason.ABSENCE_RATE_CRITICAL);
        } else if (warningByAbsenceRate) {
            breachReasons.add(AttendancePolicyBreachReason.ABSENCE_RATE_WARNING);
        }

        if (!examBannedByAbsentCount) {
            if (criticalByAbsentCount) {
                breachReasons.add(AttendancePolicyBreachReason.ABSENT_COUNT_CRITICAL);
            } else if (warningByAbsentCount) {
                breachReasons.add(AttendancePolicyBreachReason.ABSENT_COUNT_WARNING);
            }
        }

        AttendancePolicyStatus status;
        if (examBannedByAbsentCount) {
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
                effectiveThresholdBaseSessionCount,
                maxAllowedAbsences,
                earnedAttendancePoints.setScale(2, RoundingMode.HALF_UP),
                attendanceRate,
                absenceRate,
                status,
                status.name(),
                examEligibility(status),
                computedWarningAbsentCount,
                computedCriticalAbsentCount,
                computedExamBanAbsentCount,
                List.copyOf(breachReasons)
        );
    }

    static BigDecimal absenceRateThresholdForBelowRate(BigDecimal belowRate) {
        if (belowRate == null) {
            return null;
        }
        return ONE_HUNDRED.subtract(belowRate).setScale(2, RoundingMode.HALF_UP);
    }

    static Integer resolveEffectiveAbsentCount(
            Integer configuredAbsentCount,
            long thresholdBaseSessionCount,
            BigDecimal absenceRate
    ) {
        if (configuredAbsentCount != null && configuredAbsentCount > 0) {
            return configuredAbsentCount;
        }
        if (thresholdBaseSessionCount <= 0 || absenceRate == null) {
            return null;
        }
        return ceilPercent(thresholdBaseSessionCount, absenceRate);
    }

    static Integer resolveEffectiveExamBanAbsentCount(
            Integer configuredExamBanAbsentCount,
            Integer maxAllowedAbsences,
            long thresholdBaseSessionCount,
            BigDecimal examBanAbsenceRate
    ) {
        if (configuredExamBanAbsentCount != null && configuredExamBanAbsentCount > 0) {
            return configuredExamBanAbsentCount;
        }
        if (maxAllowedAbsences != null && maxAllowedAbsences > 0) {
            return maxAllowedAbsences;
        }
        if (thresholdBaseSessionCount <= 0 || examBanAbsenceRate == null) {
            return null;
        }
        return ceilPercent(thresholdBaseSessionCount, examBanAbsenceRate);
    }

    private static int ceilPercent(long total, BigDecimal percent) {
        BigDecimal raw = BigDecimal.valueOf(total)
                .multiply(percent)
                .divide(ONE_HUNDRED, 10, RoundingMode.HALF_UP);
        int computed = raw.setScale(0, RoundingMode.CEILING).intValue();
        return Math.max(computed, 1);
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
            long thresholdBaseSessionCount,
            Integer maxAllowedAbsences,
            BigDecimal earnedAttendancePoints,
            BigDecimal attendanceRate,
            BigDecimal absenceRate,
            AttendancePolicyStatus policyStatus,
            String riskLevel,
            String examEligibility,
            Integer computedWarningAbsentCount,
            Integer computedCriticalAbsentCount,
            Integer computedExamBanAbsentCount,
            List<AttendancePolicyBreachReason> breachReasons
    ) {
    }
}