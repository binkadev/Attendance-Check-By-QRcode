package com.attendance.backend.attendance.service;

import com.attendance.backend.attendance.dto.MyAttendanceHistoryItemResponse;
import com.attendance.backend.attendance.dto.PageMyAttendanceHistoryResponse;
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
import java.time.LocalTime;
import java.time.ZoneId;
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

    /**
     * App-facing upcoming sessions.
     *
     * Source chính:
     * - class_groups.start_date
     * - class_groups.total_sessions
     * - group_weekly_schedules
     *
     * attendance_sessions chỉ là phiên điểm danh thật đã được mở/tạo.
     * Vì vậy nếu chưa có attendance_sessions, API vẫn phải trả lịch học kế hoạch
     * với sessionId = null.
     */
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

            planned.addAll(generateUpcomingOccurrences(group, schedules, today, safeLimit));
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

        int remaining = safeLimit;

        if (todayItems.size() > remaining) {
            todayItems = todayItems.subList(0, remaining);
        }

        remaining -= todayItems.size();

        if (upcomingItems.size() > remaining) {
            upcomingItems = upcomingItems.subList(0, Math.max(remaining, 0));
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
                    g.start_date AS start_date,
                    g.total_sessions AS total_sessions
                FROM class_groups g
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
                        rs.getDate("start_date").toLocalDate(),
                        rs.getInt("total_sessions")
                ),
                actorUserId.toString()
        );
    }

    private List<PlannedOccurrence> generateUpcomingOccurrences(
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

                Instant startAt = cursor
                        .atTime(schedule.startTime())
                        .atZone(APP_ZONE)
                        .toInstant();

                Instant endAt = cursor
                        .atTime(schedule.endTime())
                        .atZone(APP_ZONE)
                        .toInstant();

                /*
                 * Với app mobile:
                 * - Hôm nay vẫn phải hiện dù giờ học đã bắt đầu hoặc đã qua.
                 * - Các ngày sau hôm nay vẫn hiện bình thường.
                 * - Chỉ loại các ngày trước hôm nay.
                 */
                if (!cursor.isBefore(today)) {
                    RealSessionMatch realSession = findMatchingRealSession(
                            group.groupId(),
                            cursor,
                            startAt
                    );

                    UUID sessionId = realSession == null ? null : realSession.sessionId();

                    String sessionName = realSession != null && hasText(realSession.title())
                            ? realSession.title()
                            : "Buổi " + generatedCount;

                    Instant effectiveStartAt = realSession != null && realSession.startAt() != null
                            ? realSession.startAt()
                            : startAt;

                    Instant effectiveEndAt = realSession != null && realSession.endAt() != null
                            ? realSession.endAt()
                            : endAt;

                    result.add(new PlannedOccurrence(
                            sessionId,
                            realSession == null ? null : realSession.status(),
                            realSession == null ? null : realSession.checkinOpenAt(),
                            realSession == null ? null : realSession.checkinCloseAt(),
                            group.groupId(),
                            sessionName,
                            generatedCount,
                            effectiveStartAt,
                            effectiveEndAt,
                            cursor,
                            effectiveStartAt.atZone(APP_ZONE).toLocalTime(),
                            effectiveEndAt.atZone(APP_ZONE).toLocalTime(),
                            group.room(),
                            group.groupName()
                    ));

                    /*
                     * Không cần generate quá nhiều cho từng group.
                     * List cuối còn sort toàn cục và limit lại.
                     */
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
            UUID groupId,
            LocalDate sessionDate,
            Instant plannedStartAt
    ) {
        List<RealSessionMatch> rows = jdbcTemplate.query(
                """
                SELECT
                    BIN_TO_UUID(s.id, 1) AS session_id,
                    s.title AS title,
                    s.start_at AS start_at,
                    s.end_at AS end_at,
                    s.status AS status,
                    s.checkin_open_at AS checkin_open_at,
                    s.checkin_close_at AS checkin_close_at
                FROM attendance_sessions s
                WHERE s.group_id = UUID_TO_BIN(?, 1)
                  AND s.deleted_at IS NULL
                  AND s.status <> 'CANCELLED'
                  AND s.session_date = ?
                  AND ABS(TIMESTAMPDIFF(SECOND, s.start_at, ?)) <= ?
                ORDER BY
                    CASE WHEN s.status = 'OPEN' THEN 0 ELSE 1 END,
                    ABS(TIMESTAMPDIFF(SECOND, s.start_at, ?)) ASC,
                    s.created_at DESC
                LIMIT 1
                """,
                (rs, rowNum) -> new RealSessionMatch(
                        UUID.fromString(rs.getString("session_id")),
                        rs.getString("title"),
                        toInstant(rs.getObject("start_at")),
                        toInstant(rs.getObject("end_at")),
                        rs.getString("status"),
                        toInstant(rs.getObject("checkin_open_at")),
                        toInstant(rs.getObject("checkin_close_at"))
                ),
                groupId.toString(),
                java.sql.Date.valueOf(sessionDate),
                Timestamp.from(plannedStartAt),
                SESSION_MATCH_TOLERANCE_SECONDS,
                Timestamp.from(plannedStartAt)
        );

        return rows.isEmpty() ? null : rows.get(0);
    }

    private UpcomingSessionResponse toUpcomingSessionResponse(PlannedOccurrence occurrence) {
        return new UpcomingSessionResponse(
                occurrence.sessionId(),
                occurrence.attendanceStatus(),
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
                occurrence.groupName()
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

        if (value instanceof java.sql.Date date) {
            return date.toLocalDate().atStartOfDay(APP_ZONE).toInstant();
        }

        if (value instanceof java.util.Date date) {
            return date.toInstant();
        }

        return Timestamp.valueOf(value.toString()).toInstant();
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
            UUID sessionId,
            String attendanceStatus,
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
            String groupName
    ) {
    }

    private record RealSessionMatch(
            UUID sessionId,
            String title,
            Instant startAt,
            Instant endAt,
            String status,
            Instant checkinOpenAt,
            Instant checkinCloseAt
    ) {
    }
}