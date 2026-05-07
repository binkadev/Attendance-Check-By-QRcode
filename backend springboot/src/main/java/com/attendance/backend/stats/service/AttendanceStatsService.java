package com.attendance.backend.stats.service;

import com.attendance.backend.common.exception.ApiException;
import com.attendance.backend.stats.dto.AttendanceSummaryResponse;
import com.attendance.backend.stats.dto.GroupAttendanceSummaryItemResponse;
import com.attendance.backend.stats.dto.GroupAttendanceSummaryPageResponse;
import com.attendance.backend.stats.repository.AttendanceStatsQueryRepository;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
public class AttendanceStatsService {

    private final AttendanceStatsQueryRepository attendanceStatsQueryRepository;

    public AttendanceStatsService(AttendanceStatsQueryRepository attendanceStatsQueryRepository) {
        this.attendanceStatsQueryRepository = attendanceStatsQueryRepository;
    }

    @Transactional(readOnly = true)
    public AttendanceSummaryResponse getMySummary(UUID userId) {
        return getMyAttendanceSummary(userId, null, null);
    }

    @Transactional(readOnly = true)
    public AttendanceSummaryResponse getMyAttendanceSummary(
            UUID userId,
            String semester,
            String academicYear
    ) {
        String semesterFilter = blankToNull(semester);
        String academicYearFilter = blankToNull(academicYear);

        AttendanceStatsQueryRepository.StudentSummaryRow row =
                attendanceStatsQueryRepository.findStudentSummary(
                        userId,
                        semesterFilter,
                        academicYearFilter
                );

        return toAttendanceSummaryResponse(
                row.totalSessions(),
                row.presentCount(),
                row.lateCount(),
                row.absentCount(),
                row.excusedCount()
        );
    }

    @Transactional(readOnly = true)
    public GroupAttendanceSummaryPageResponse getGroupSummary(UUID groupId, UUID actorUserId, int page, int size) {
        if (!attendanceStatsQueryRepository.groupExists(groupId)) {
            throw ApiException.notFound("GROUP_NOT_FOUND", "Group not found");
        }

        if (!attendanceStatsQueryRepository.isOwnerOrCoHost(groupId, actorUserId)) {
            throw ApiException.forbidden("FORBIDDEN", "Only OWNER/CO_HOST can view group attendance summary");
        }

        Page<AttendanceStatsQueryRepository.GroupSummaryRow> result =
                attendanceStatsQueryRepository.findGroupSummaryPage(groupId, page, size);

        List<GroupAttendanceSummaryItemResponse> items = result.getContent().stream()
                .map(row -> {
                    AttendanceSummaryResponse summary = toAttendanceSummaryResponse(
                            row.totalSessions(),
                            row.presentCount(),
                            row.lateCount(),
                            row.absentCount(),
                            row.excusedCount()
                    );

                    return new GroupAttendanceSummaryItemResponse(
                            row.userId(),
                            row.fullName(),
                            row.email(),
                            summary.totalSessions(),
                            summary.presentCount(),
                            summary.lateCount(),
                            summary.absentCount(),
                            summary.excusedCount(),
                            summary.attendancePercent(),
                            summary.absencePercent(),
                            summary.warningLevel(),
                            summary.riskLevel()
                    );
                })
                .toList();

        return new GroupAttendanceSummaryPageResponse(
                items,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    private AttendanceSummaryResponse toAttendanceSummaryResponse(
            long totalSessions,
            long presentCount,
            long lateCount,
            long absentCount,
            long excusedCount
    ) {
        BigDecimal attendancePercent = calculateAttendancePercent(totalSessions, presentCount, lateCount, excusedCount);
        BigDecimal absencePercent = calculateAbsencePercent(totalSessions, absentCount);

        return new AttendanceSummaryResponse(
                totalSessions,
                presentCount,
                lateCount,
                absentCount,
                excusedCount,
                attendancePercent,
                absencePercent,
                resolveWarningLevel(absencePercent),
                resolveRiskLevel(absencePercent)
        );
    }

    private BigDecimal calculateAttendancePercent(
            long totalSessions,
            long presentCount,
            long lateCount,
            long excusedCount
    ) {
        if (totalSessions == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        long attended = presentCount + lateCount + excusedCount;

        return BigDecimal.valueOf(attended)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalSessions), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateAbsencePercent(long totalSessions, long absentCount) {
        if (totalSessions == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return BigDecimal.valueOf(absentCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalSessions), 2, RoundingMode.HALF_UP);
    }

    private AttendanceSummaryResponse.WarningLevel resolveWarningLevel(BigDecimal absencePercent) {
        if (absencePercent.compareTo(BigDecimal.valueOf(20)) >= 0) {
            return AttendanceSummaryResponse.WarningLevel.CRITICAL_20;
        }

        if (absencePercent.compareTo(BigDecimal.valueOf(15)) >= 0) {
            return AttendanceSummaryResponse.WarningLevel.WARNING_15;
        }

        return AttendanceSummaryResponse.WarningLevel.NONE;
    }

    private AttendanceSummaryResponse.RiskLevel resolveRiskLevel(BigDecimal absencePercent) {
        if (absencePercent.compareTo(BigDecimal.valueOf(20)) >= 0) {
            return AttendanceSummaryResponse.RiskLevel.HIGH;
        }

        if (absencePercent.compareTo(BigDecimal.valueOf(15)) >= 0) {
            return AttendanceSummaryResponse.RiskLevel.MEDIUM;
        }

        return AttendanceSummaryResponse.RiskLevel.LOW;
    }

    private String blankToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }
}