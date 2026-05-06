package com.attendance.backend.attendance.dto;

public record AttendanceSummaryResponse(
        String semester,
        String academicYear,
        String label,

        long totalClasses,
        long closedSessionCount,
        long totalSessions,

        long presentCount,
        long lateCount,
        long absentCount,
        long excusedCount,

        double attendancePercent,
        double absencePercent,

        boolean eligibleExam,
        String message,

        String warningLevel,
        String riskLevel,

        Breakdown breakdown
) {
    public record Breakdown(
            double presentPercent,
            double latePercent,
            double absentPercent,
            double excusedPercent
    ) {
    }
}