package com.attendance.backend.attendance.service;

import com.attendance.backend.attendance.dto.MyAttendanceHistoryItemResponse;
import com.attendance.backend.attendance.dto.PageMyAttendanceHistoryResponse;
import com.attendance.backend.attendance.dto.AttendanceSummaryResponse;
import com.attendance.backend.attendance.dto.UpcomingSessionResponse;
import com.attendance.backend.attendance.dto.UpcomingSessionsTimelineResponse;
import com.attendance.backend.attendance.repository.AttendanceReadQueryRepository;
import com.attendance.backend.attendance.support.SimpleXlsxWriter;
import com.attendance.backend.common.exception.ApiException;
import com.attendance.backend.domain.entity.GroupMember;
import com.attendance.backend.domain.entity.GroupWeeklySchedule;
import com.attendance.backend.domain.enums.MemberRole;
import com.attendance.backend.domain.enums.MemberStatus;
import com.attendance.backend.group.repository.ClassGroupRepository;
import com.attendance.backend.group.repository.GroupMemberRepository;
import com.attendance.backend.group.repository.GroupWeeklyScheduleRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class AttendanceReadServiceImpl implements AttendanceReadService {

    private static final int MAX_PAGE_SIZE = 200;
    private static final int MAX_UPCOMING_LIMIT = 100;
    private static final int MAX_GENERATION_DAYS = 3700;
    private static final int SESSION_MATCH_TOLERANCE_SECONDS = 1800;
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final AttendanceReadQueryRepository attendanceReadQueryRepository;
    private final ClassGroupRepository classGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupWeeklyScheduleRepository groupWeeklyScheduleRepository;
    private final JdbcTemplate jdbcTemplate;

    public AttendanceReadServiceImpl(
            AttendanceReadQueryRepository attendanceReadQueryRepository,
            ClassGroupRepository classGroupRepository,
            GroupMemberRepository groupMemberRepository,
            GroupWeeklyScheduleRepository groupWeeklyScheduleRepository,
            JdbcTemplate jdbcTemplate
    ) {
        this.attendanceReadQueryRepository = attendanceReadQueryRepository;
        this.classGroupRepository = classGroupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.groupWeeklyScheduleRepository = groupWeeklyScheduleRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional(readOnly = true)
    public UpcomingSessionsTimelineResponse listUpcomingSessions(UUID actorUserId, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), MAX_UPCOMING_LIMIT);
        LocalDate today = LocalDate.now(APP_ZONE);

        List<GroupScheduleSource> groups = findVisibleScheduleGroups(actorUserId);
        List<PlannedOccurrence> planned = new ArrayList<>();

        for (GroupScheduleSource group : groups) {
            if (group.startDate() == null || group.totalSessions() == null || group.totalSessions() <= 0) {
                continue;
            }

            List<GroupWeeklySchedule> schedules =
                    groupWeeklyScheduleRepository.findByGroupIdAndDeletedAtIsNullOrderByDayOfWeekAscStartTimeAsc(
                            group.groupId()
                    );

            if (schedules.isEmpty()) {
                continue;
            }

            planned.addAll(generateUpcomingOccurrences(actorUserId, group, schedules, today, safeLimit));
        }

        List<PlannedOccurrence> sorted = planned.stream()
                .sorted(Comparator
                        .comparing(PlannedOccurrence::sessionDate)
                        .thenComparing(PlannedOccurrence::startTime)
                        .thenComparing(PlannedOccurrence::groupName)
                        .thenComparing(PlannedOccurrence::sessionIndex))
                .toList();

        List<UpcomingSessionResponse> todayItems = sorted.stream()
                .filter(item -> item.sessionDate().isEqual(today))
                .map(this::toUpcomingSessionResponse)
                .toList();

        List<UpcomingSessionResponse> upcomingItems = sorted.stream()
                .filter(item -> item.sessionDate().isAfter(today))
                .map(this::toUpcomingSessionResponse)
                .toList();

        if (upcomingItems.size() > safeLimit) {
            upcomingItems = upcomingItems.subList(0, safeLimit);
        }

        return new UpcomingSessionsTimelineResponse(List.of(
                new UpcomingSessionsTimelineResponse.Section(
                        "TODAY",
                        "Hôm nay",
                        today,
                        todayItems
                ),
                new UpcomingSessionsTimelineResponse.Section(
                        "UPCOMING",
                        "Lịch học sắp tới",
                        null,
                        upcomingItems
                )
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceSummaryResponse getMyAttendanceSummary(
            UUID actorUserId,
            String semester,
            String academicYear
    ) {
        String semesterFilter = blankToNull(semester);
        String academicYearFilter = blankToNull(academicYear);

        AttendanceSummaryCounts counts = jdbcTemplate.queryForObject(
                """
                SELECT
                    COUNT(DISTINCT g.id) AS total_classes,
                    COUNT(s.id) AS closed_session_count,
    
                    SUM(
                        CASE
                            WHEN s.id IS NULL THEN 0
                            WHEN COALESCE(sa.attendance_status, 'ABSENT') = 'PRESENT' THEN 1
                            ELSE 0
                        END
                    ) AS present_count,
    
                    SUM(
                        CASE
                            WHEN s.id IS NULL THEN 0
                            WHEN COALESCE(sa.attendance_status, 'ABSENT') = 'LATE' THEN 1
                            ELSE 0
                        END
                    ) AS late_count,
    
                    SUM(
                        CASE
                            WHEN s.id IS NULL THEN 0
                            WHEN COALESCE(sa.attendance_status, 'ABSENT') = 'ABSENT' THEN 1
                            ELSE 0
                        END
                    ) AS absent_count,
    
                    SUM(
                        CASE
                            WHEN s.id IS NULL THEN 0
                            WHEN COALESCE(sa.attendance_status, 'ABSENT') = 'EXCUSED' THEN 1
                            ELSE 0
                        END
                    ) AS excused_count
                FROM class_groups g
                JOIN group_members gm
                  ON gm.group_id = g.id
                 AND gm.user_id = UUID_TO_BIN(?, 1)
                 AND gm.member_status = 'APPROVED'
                 AND gm.role = 'MEMBER'
                LEFT JOIN attendance_sessions s
                  ON s.group_id = g.id
                 AND s.deleted_at IS NULL
                 AND s.status = 'CLOSED'
                LEFT JOIN session_attendance sa
                  ON sa.session_id = s.id
                 AND sa.user_id = gm.user_id
                WHERE g.deleted_at IS NULL
                  AND g.status = 'ACTIVE'
                  AND (? IS NULL OR g.semester = ?)
                  AND (? IS NULL OR g.academic_year = ?)
                """,
                (rs, rowNum) -> new AttendanceSummaryCounts(
                        rs.getLong("total_classes"),
                        rs.getLong("closed_session_count"),
                        rs.getLong("present_count"),
                        rs.getLong("late_count"),
                        rs.getLong("absent_count"),
                        rs.getLong("excused_count")
                ),
                actorUserId.toString(),
                semesterFilter,
                semesterFilter,
                academicYearFilter,
                academicYearFilter
        );

        if (counts == null) {
            counts = new AttendanceSummaryCounts(0, 0, 0, 0, 0, 0);
        }

        long totalSessions = counts.closedSessionCount();

        double presentPercent = percent(counts.presentCount(), totalSessions);
        double latePercent = percent(counts.lateCount(), totalSessions);
        double absentPercent = percent(counts.absentCount(), totalSessions);
        double excusedPercent = percent(counts.excusedCount(), totalSessions);

        /*
         * UI đang hiển thị Overall Rate theo số PRESENT thật.
         * Ví dụ ảnh: Present 42, Late 3, Absent 1 => 42 / 46 = 91.3%.
         * Vì vậy không gộp LATE vào attendancePercent.
         */
        double attendancePercent = presentPercent;
        double absencePercent = absentPercent;

        boolean eligibleExam = totalSessions == 0 || absencePercent <= 20.0;

        String message;
        if (totalSessions == 0) {
            message = "Chưa có dữ liệu điểm danh";
        } else if (eligibleExam) {
            message = "Đủ điều kiện dự thi";
        } else {
            message = "Có nguy cơ không đủ điều kiện dự thi";
        }

        return new AttendanceSummaryResponse(
                semesterFilter,
                academicYearFilter,
                summaryLabel(semesterFilter, academicYearFilter),

                counts.totalClasses(),
                counts.closedSessionCount(),
                counts.closedSessionCount(),

                counts.presentCount(),
                counts.lateCount(),
                counts.absentCount(),
                counts.excusedCount(),

                attendancePercent,
                absencePercent,

                eligibleExam,
                message,

                warningLevel(absencePercent, totalSessions),
                riskLevel(absencePercent, totalSessions),

                new AttendanceSummaryResponse.Breakdown(
                        presentPercent,
                        latePercent,
                        absentPercent,
                        excusedPercent
                )
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageMyAttendanceHistoryResponse listMyAttendancesInGroup(
            UUID actorUserId,
            UUID groupId,
            int page,
            int size
    ) {
        requireGroupExists(groupId);
        requireApprovedMember(actorUserId, groupId);

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        List<MyAttendanceHistoryItemResponse> items =
                attendanceReadQueryRepository.findMyAttendancesInGroup(actorUserId, groupId, safePage, safeSize);

        long totalElements = attendanceReadQueryRepository.countMyAttendancesInGroup(actorUserId, groupId);
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / safeSize);

        return new PageMyAttendanceHistoryResponse(
                items,
                safePage,
                safeSize,
                totalElements,
                totalPages
        );
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportGroupAttendance(UUID actorUserId, UUID groupId) {
        requireGroupExists(groupId);
        requireManageAccess(actorUserId, groupId);

        List<MyAttendanceHistoryItemResponse> items =
                attendanceReadQueryRepository.findGroupAttendanceForExport(groupId);

        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of(
                "Group ID",
                "Group Name",
                "Session ID",
                "Session Name",
                "Session Date",
                "Start Time",
                "End Time",
                "Session Status",
                "Attendance Status",
                "Check In At",
                "Check In Method",
                "Suspicious",
                "Suspicious Reason"
        ));

        for (MyAttendanceHistoryItemResponse item : items) {
            rows.add(List.of(
                    value(item.groupId()),
                    value(item.groupName()),
                    value(item.sessionId()),
                    value(item.sessionName()),
                    value(item.sessionDate()),
                    value(item.startTime()),
                    value(item.endTime()),
                    value(item.sessionStatus()),
                    value(item.attendanceStatus()),
                    value(item.checkInAt()),
                    value(item.checkInMethod()),
                    value(item.suspiciousFlag()),
                    value(item.suspiciousReason())
            ));
        }

        return SimpleXlsxWriter.writeSheet("attendance", rows);
    }

    private List<GroupScheduleSource> findVisibleScheduleGroups(UUID actorUserId) {
        return jdbcTemplate.query(
                """
                SELECT
                    BIN_TO_UUID(g.id, 1) AS group_id,
                    g.name AS group_name,
                    g.room AS room,
                    u.full_name AS lecturer_name,
                    g.start_date AS start_date,
                    g.total_sessions AS total_sessions
                FROM class_groups g
                JOIN users u
                  ON u.id = g.owner_user_id
                JOIN group_members gm
                  ON gm.group_id = g.id
                 AND gm.user_id = UUID_TO_BIN(?, 1)
                 AND gm.member_status = 'APPROVED'
                WHERE g.deleted_at IS NULL
                  AND g.status = 'ACTIVE'
                  AND g.start_date IS NOT NULL
                  AND g.total_sessions IS NOT NULL
                  AND g.total_sessions > 0
                ORDER BY g.name ASC
                """,
                (rs, rowNum) -> new GroupScheduleSource(
                        UUID.fromString(rs.getString("group_id")),
                        rs.getString("group_name"),
                        rs.getString("room"),
                        rs.getString("lecturer_name"),
                        rs.getDate("start_date").toLocalDate(),
                        rs.getInt("total_sessions")
                ),
                actorUserId.toString()
        );
    }

    private String blankToNull(String value) {
        if (!hasText(value)) {
            return null;
        }

        return value.trim();
    }

    private double percent(long count, long total) {
        if (total <= 0) {
            return 0.0;
        }

        return roundOne((count * 100.0) / total);
    }

    private double roundOne(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private String summaryLabel(String semester, String academicYear) {
        if (hasText(semester) && hasText(academicYear)) {
            return semester + " (" + academicYear + ")";
        }

        if (hasText(semester)) {
            return semester;
        }

        if (hasText(academicYear)) {
            return academicYear;
        }

        return "Tất cả học kỳ";
    }

    private String warningLevel(double absencePercent, long totalSessions) {
        if (totalSessions <= 0) {
            return "NONE";
        }

        if (absencePercent > 20.0) {
            return "CRITICAL_20";
        }

        if (absencePercent >= 15.0) {
            return "WARNING_15";
        }

        return "NONE";
    }

    private String riskLevel(double absencePercent, long totalSessions) {
        if (totalSessions <= 0) {
            return "LOW";
        }

        if (absencePercent > 20.0) {
            return "HIGH";
        }

        if (absencePercent >= 15.0) {
            return "MEDIUM";
        }

        return "LOW";
    }

    private List<PlannedOccurrence> generateUpcomingOccurrences(
            UUID actorUserId,
            GroupScheduleSource group,
            List<GroupWeeklySchedule> schedules,
            LocalDate today,
            int targetLimit
    ) {
        List<NormalizedWeeklySchedule> normalizedSchedules = schedules.stream()
                .map(this::toNormalizedWeeklySchedule)
                .sorted(Comparator
                        .comparing(NormalizedWeeklySchedule::dayOfWeek)
                        .thenComparing(NormalizedWeeklySchedule::startTime))
                .toList();

        List<PlannedOccurrence> result = new ArrayList<>();

        int generatedCount = 0;
        LocalDate cursor = group.startDate();
        int guardDays = 0;

        while (generatedCount < group.totalSessions() && guardDays <= MAX_GENERATION_DAYS) {
            for (NormalizedWeeklySchedule schedule : normalizedSchedules) {
                if (cursor.getDayOfWeek() != schedule.dayOfWeek()) {
                    continue;
                }

                generatedCount++;

                Instant plannedStartAt = cursor
                        .atTime(schedule.startTime())
                        .atZone(APP_ZONE)
                        .toInstant();

                Instant plannedEndAt = cursor
                        .atTime(schedule.endTime())
                        .atZone(APP_ZONE)
                        .toInstant();

                if (!cursor.isBefore(today)) {
                    RealSessionMatch realSession = findMatchingRealSession(
                            actorUserId,
                            group.groupId(),
                            cursor,
                            plannedStartAt,
                            plannedEndAt
                    );

                    UUID attendanceSessionId = realSession == null ? null : realSession.sessionId();

                    String sessionName = realSession != null && hasText(realSession.title())
                            ? realSession.title()
                            : "Buổi " + generatedCount;

                    String attendanceStatus = realSession == null
                            ? "ABSENT"
                            : normalizeAttendanceStatus(realSession.attendanceStatus());

                    result.add(new PlannedOccurrence(
                            attendanceSessionId,
                            attendanceStatus,
                            attendanceStatusLabel(attendanceStatus),
                            checkedIn(attendanceStatus),
                            realSession == null ? null : realSession.checkInAt(),
                            realSession == null ? null : realSession.checkinOpenAt(),
                            realSession == null ? null : realSession.checkinCloseAt(),
                            group.groupId(),
                            sessionName,
                            generatedCount,
                            plannedStartAt,
                            plannedEndAt,
                            cursor,
                            schedule.startTime(),
                            schedule.endTime(),
                            group.room(),
                            group.groupName(),
                            group.lecturerName()
                    ));

                    if (result.size() >= targetLimit) {
                        return result;
                    }
                }

                if (generatedCount >= group.totalSessions()) {
                    break;
                }
            }

            cursor = cursor.plusDays(1);
            guardDays++;
        }

        return result;
    }

    private NormalizedWeeklySchedule toNormalizedWeeklySchedule(GroupWeeklySchedule schedule) {
        DayOfWeek dayOfWeek;

        try {
            dayOfWeek = DayOfWeek.valueOf(schedule.getDayOfWeek());
        } catch (RuntimeException ex) {
            throw ApiException.unprocessable(
                    "INVALID_WEEKLY_SCHEDULE_DAY",
                    "Invalid weekly schedule dayOfWeek: " + schedule.getDayOfWeek()
            );
        }

        LocalTime startTime = schedule.getStartTime();
        LocalTime endTime = schedule.getEndTime();

        if (startTime == null || endTime == null || !startTime.isBefore(endTime)) {
            throw ApiException.unprocessable(
                    "INVALID_WEEKLY_SCHEDULE_TIME",
                    "Weekly schedule startTime must be before endTime"
            );
        }

        return new NormalizedWeeklySchedule(dayOfWeek, startTime, endTime);
    }

    private RealSessionMatch findMatchingRealSession(
            UUID actorUserId,
            UUID groupId,
            LocalDate sessionDate,
            Instant plannedStartAt,
            Instant plannedEndAt
    ) {
        List<RealSessionMatch> rows = jdbcTemplate.query(
                """
                SELECT
                    BIN_TO_UUID(s.id, 1) AS session_id,
                    s.title AS title,
                    s.start_at AS start_at,
                    s.end_at AS end_at,
                    s.status AS session_status,
                    s.checkin_open_at AS checkin_open_at,
                    s.checkin_close_at AS checkin_close_at,
                    COALESCE(sa.attendance_status, 'ABSENT') AS attendance_status,
                    sa.check_in_at AS check_in_at
                FROM attendance_sessions s
                LEFT JOIN session_attendance sa
                  ON sa.session_id = s.id
                 AND sa.user_id = UUID_TO_BIN(?, 1)
                WHERE s.group_id = UUID_TO_BIN(?, 1)
                  AND s.deleted_at IS NULL
                  AND s.status <> 'CANCELLED'
                  AND s.session_date = ?
                  AND (
                        ABS(TIMESTAMPDIFF(SECOND, s.start_at, ?)) <= ?
                        OR (
                            s.start_at <= ?
                            AND COALESCE(s.end_at, s.checkin_close_at, s.start_at) >= ?
                        )
                        OR sa.user_id IS NOT NULL
                      )
                ORDER BY
                    CASE WHEN sa.user_id IS NOT NULL THEN 0 ELSE 1 END,
                    CASE WHEN s.status = 'OPEN' THEN 0 ELSE 1 END,
                    CASE
                        WHEN s.start_at <= ?
                         AND COALESCE(s.end_at, s.checkin_close_at, s.start_at) >= ?
                        THEN 0 ELSE 1
                    END,
                    ABS(TIMESTAMPDIFF(SECOND, s.start_at, ?)) ASC,
                    s.created_at DESC
                LIMIT 1
                """,
                (rs, rowNum) -> new RealSessionMatch(
                        UUID.fromString(rs.getString("session_id")),
                        rs.getString("title"),
                        toInstant(rs.getObject("start_at")),
                        toInstant(rs.getObject("end_at")),
                        rs.getString("session_status"),
                        rs.getString("attendance_status"),
                        toInstant(rs.getObject("check_in_at")),
                        toInstant(rs.getObject("checkin_open_at")),
                        toInstant(rs.getObject("checkin_close_at"))
                ),
                actorUserId.toString(),
                groupId.toString(),
                java.sql.Date.valueOf(sessionDate),
                Timestamp.from(plannedStartAt),
                SESSION_MATCH_TOLERANCE_SECONDS,
                Timestamp.from(plannedEndAt),
                Timestamp.from(plannedStartAt),
                Timestamp.from(plannedEndAt),
                Timestamp.from(plannedStartAt),
                Timestamp.from(plannedStartAt)
        );

        return rows.isEmpty() ? null : rows.get(0);
    }

    private UpcomingSessionResponse toUpcomingSessionResponse(PlannedOccurrence occurrence) {
        return new UpcomingSessionResponse(
                occurrence.attendanceSessionId(),
                occurrence.attendanceStatus(),
                occurrence.attendanceStatusLabel(),
                occurrence.checkedIn(),
                occurrence.checkInAt(),
                occurrence.checkinOpenAt(),
                occurrence.checkinCloseAt(),
                occurrence.groupId(),
                occurrence.sessionName(),
                occurrence.startAt(),
                occurrence.endAt(),
                occurrence.sessionDate(),
                occurrence.startTime(),
                occurrence.endTime(),
                occurrence.room(),
                occurrence.groupName(),
                occurrence.lecturerName()
        );
    }

    private Instant toInstant(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Instant instant) {
            return instant;
        }

        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant();
        }

        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.atZone(APP_ZONE).toInstant();
        }

        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toInstant();
        }

        if (value instanceof ZonedDateTime zonedDateTime) {
            return zonedDateTime.toInstant();
        }

        if (value instanceof java.sql.Date date) {
            return date.toLocalDate().atStartOfDay(APP_ZONE).toInstant();
        }

        if (value instanceof java.util.Date date) {
            return date.toInstant();
        }

        String text = value.toString().trim();

        try {
            return Instant.parse(text);
        } catch (DateTimeParseException ignored) {
            return LocalDateTime.parse(text)
                    .atZone(APP_ZONE)
                    .toInstant();
        }
    }

    private void requireGroupExists(UUID groupId) {
        classGroupRepository.findActiveById(groupId)
                .orElseThrow(() -> ApiException.notFound("GROUP_NOT_FOUND", "Group not found"));
    }

    private GroupMember requireApprovedMember(UUID actorUserId, UUID groupId) {
        GroupMember membership = groupMemberRepository.findByGroupIdAndUserId(groupId, actorUserId)
                .orElseThrow(() -> ApiException.forbidden("FORBIDDEN", "You are not a member of this group"));

        if (membership.getMemberStatus() != MemberStatus.APPROVED) {
            throw ApiException.forbidden("FORBIDDEN", "Your group membership is not approved");
        }

        return membership;
    }

    private void requireManageAccess(UUID actorUserId, UUID groupId) {
        GroupMember membership = requireApprovedMember(actorUserId, groupId);

        boolean canManage = membership.getRole() == MemberRole.OWNER
                || membership.getRole() == MemberRole.CO_HOST;

        if (!canManage) {
            throw ApiException.forbidden("FORBIDDEN", "Only OWNER or CO_HOST can export attendance");
        }
    }

    private String normalizeAttendanceStatus(String status) {
        if (!hasText(status)) {
            return "ABSENT";
        }

        return status.trim().toUpperCase();
    }

    private boolean checkedIn(String attendanceStatus) {
        return "PRESENT".equals(attendanceStatus) || "LATE".equals(attendanceStatus);
    }

    private String attendanceStatusLabel(String attendanceStatus) {
        return switch (attendanceStatus) {
            case "PRESENT", "LATE" -> "Đã điểm danh";
            case "EXCUSED" -> "Có phép";
            case "ABSENT" -> "Chưa điểm danh";
            default -> "Chưa điểm danh";
        };
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private record GroupScheduleSource(
            UUID groupId,
            String groupName,
            String room,
            String lecturerName,
            LocalDate startDate,
            Integer totalSessions
    ) {
    }

    private record NormalizedWeeklySchedule(
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime
    ) {
    }

    private record PlannedOccurrence(
            UUID attendanceSessionId,
            String attendanceStatus,
            String attendanceStatusLabel,
            Boolean checkedIn,
            Instant checkInAt,
            Instant checkinOpenAt,
            Instant checkinCloseAt,
            UUID groupId,
            String sessionName,
            int sessionIndex,
            Instant startAt,
            Instant endAt,
            LocalDate sessionDate,
            LocalTime startTime,
            LocalTime endTime,
            String room,
            String groupName,
            String lecturerName
    ) {
    }

    private record RealSessionMatch(
            UUID sessionId,
            String title,
            Instant startAt,
            Instant endAt,
            String sessionStatus,
            String attendanceStatus,
            Instant checkInAt,
            Instant checkinOpenAt,
            Instant checkinCloseAt
    ) {
    }

    private record AttendanceSummaryCounts(
            long totalClasses,
            long closedSessionCount,
            long presentCount,
            long lateCount,
            long absentCount,
            long excusedCount
    ) {
    }
}