package com.attendance.backend.attendance.service;

import com.attendance.backend.attendance.repository.AttendancePolicyQueryRepository;
import com.attendance.backend.attendance.repository.AttendancePolicyStudentAggregateProjection;
import com.attendance.backend.domain.enums.AttendancePolicyStatus;
import com.attendance.backend.domain.enums.NotificationSeverity;
import com.attendance.backend.domain.enums.NotificationSourceType;
import com.attendance.backend.domain.enums.NotificationType;
import com.attendance.backend.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class AttendancePolicyNotificationOrchestrator {

    private final AttendancePolicyService attendancePolicyService;
    private final AttendancePolicyQueryRepository attendancePolicyQueryRepository;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager entityManager;

    public AttendancePolicyNotificationOrchestrator(
            AttendancePolicyService attendancePolicyService,
            AttendancePolicyQueryRepository attendancePolicyQueryRepository,
            NotificationService notificationService,
            ObjectMapper objectMapper
    ) {
        this.attendancePolicyService = attendancePolicyService;
        this.attendancePolicyQueryRepository = attendancePolicyQueryRepository;
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void reevaluateOne(UUID groupId, UUID userId, UUID sourceSessionId) {
        AttendancePolicyService.EffectivePolicy policy = attendancePolicyService.getEffectivePolicy(groupId);
        AttendancePolicySnapshot snapshot = buildPolicySnapshot(policy, groupId, userId);

        if (snapshot == null) {
            return;
        }

        SourceSessionInfo sourceSession = findSourceSessionInfo(groupId, userId, sourceSessionId);
        publishPolicyNotification(policy, snapshot, groupId, sourceSessionId, sourceSession);
    }

    @Transactional
    public void reevaluateClosedSession(UUID groupId, UUID sourceSessionId) {
        AttendancePolicyService.EffectivePolicy policy = attendancePolicyService.getEffectivePolicy(groupId);
        List<SessionClosureMemberStatus> statuses = findSessionClosureMemberStatuses(groupId, sourceSessionId);

        for (SessionClosureMemberStatus status : statuses) {
            AttendancePolicySnapshot snapshot = buildPolicySnapshot(policy, groupId, status.userId());
            if (snapshot == null) {
                continue;
            }

            SourceSessionInfo sourceSession = findSourceSessionInfo(groupId, status.userId(), sourceSessionId);
            if ("ABSENT".equals(status.attendanceStatus())) {
                publishAbsenceWarning(status.userId(), groupId, sourceSessionId, sourceSession, policy, snapshot);
            }

            publishPolicyNotification(policy, snapshot, groupId, sourceSessionId, sourceSession);
        }
    }

    private AttendancePolicySnapshot buildPolicySnapshot(
            AttendancePolicyService.EffectivePolicy policy,
            UUID groupId,
            UUID userId
    ) {
        AttendancePolicyStudentAggregateProjection aggregate =
                attendancePolicyQueryRepository.aggregateForApprovedMember(
                        groupId.toString(),
                        userId.toString()
                );

        if (aggregate == null) {
            return null;
        }

        AttendancePolicyComputation.ComputedPolicyStatus computed =
                AttendancePolicyComputation.compute(
                        policy,
                        nullSafe(aggregate.getPresentCount()),
                        nullSafe(aggregate.getLateCount()),
                        nullSafe(aggregate.getAbsentCount()),
                        nullSafe(aggregate.getExcusedCount())
                );

        return new AttendancePolicySnapshot(
                aggregate,
                computed,
                Instant.now(),
                findAbsenceSessions(groupId, userId)
        );
    }

    private void publishPolicyNotification(
            AttendancePolicyService.EffectivePolicy policy,
            AttendancePolicySnapshot snapshot,
            UUID groupId,
            UUID sourceSessionId,
            SourceSessionInfo sourceSession
    ) {
        AttendancePolicyStudentAggregateProjection aggregate = snapshot.aggregate();
        AttendancePolicyComputation.ComputedPolicyStatus computed = snapshot.computed();
        UUID recipientUserId = UUID.fromString(aggregate.getUserId());

        NotificationType notificationType = notificationType(computed.policyStatus());
        if (notificationType == null) {
            return;
        }

        NotificationContent content = notificationContent(computed.policyStatus(), policy, snapshot, sourceSession);

        notificationService.createOne(new NotificationService.CreateNotificationCommand(
                recipientUserId,
                groupId,
                sourceSessionId,
                notificationType,
                content.title(),
                content.body(),
                buildPayload(policy, aggregate, computed, groupId, sourceSessionId, sourceSession, snapshot),
                content.severity(),
                NotificationSourceType.ATTENDANCE_POLICY,
                sourceSessionId,
                buildPolicyDedupKey(
                        notificationType,
                        recipientUserId,
                        groupId,
                        sourceSessionId
                )
        ));
    }

    private ObjectNode buildPayload(
            AttendancePolicyService.EffectivePolicy policy,
            AttendancePolicyStudentAggregateProjection aggregate,
            AttendancePolicyComputation.ComputedPolicyStatus computed,
            UUID groupId,
            UUID sourceSessionId,
            SourceSessionInfo sourceSession,
            AttendancePolicySnapshot snapshot
    ) {
        ObjectNode payload = objectMapper.createObjectNode();

        putAcademicContext(payload, groupId, sourceSessionId, sourceSession);

        payload.put("userId", aggregate.getUserId());
        payload.put("fullName", aggregate.getFullName());
        payload.put("email", aggregate.getEmail());

        payload.put("closedSessionCount", nullSafe(aggregate.getClosedSessionCount()));
        payload.put("presentCount", nullSafe(aggregate.getPresentCount()));
        payload.put("lateCount", nullSafe(aggregate.getLateCount()));
        payload.put("absentCount", nullSafe(aggregate.getAbsentCount()));
        payload.put("excusedCount", nullSafe(aggregate.getExcusedCount()));

        payload.put("eligibleSessionCount", computed.eligibleSessionCount());
        payload.put("earnedAttendancePoints", computed.earnedAttendancePoints());

        putNullableDecimal(payload, "attendanceRate", computed.attendanceRate());
        putNullableDecimal(payload, "absenceRate", computed.absenceRate());

        payload.put("policyStatus", computed.policyStatus().name());
        payload.put("riskLevel", computed.riskLevel());
        payload.put("examEligibility", computed.examEligibility());
        payload.put("computedAt", snapshot.computedAt().toString());

        ArrayNode reasons = payload.putArray("breachReasons");
        for (var reason : computed.breachReasons()) {
            reasons.add(reason.name());
        }

        payload.put("lateWeight", policy.lateWeight());
        payload.put("warningBelowRate", policy.warningBelowRate());
        putNullableDecimal(payload, "warningAbsenceRate", AttendancePolicyComputation.absenceRateThresholdForBelowRate(policy.warningBelowRate()));

        putNullableDecimal(payload, "criticalBelowRate", policy.criticalBelowRate());
        putNullableDecimal(payload, "criticalAbsenceRate", AttendancePolicyComputation.absenceRateThresholdForBelowRate(policy.criticalBelowRate()));
        putNullableDecimal(payload, "examBanAbsenceRate", policy.examBanAbsenceRate());

        putNullableInteger(payload, "warningAbsentCount", policy.warningAbsentCount());
        putNullableInteger(payload, "criticalAbsentCount", policy.criticalAbsentCount());
        putNullableInteger(payload, "examBanAbsentCount", policy.examBanAbsentCount());

        putThresholds(payload, policy);
        putAbsenceSessions(payload, snapshot.absenceSessions());

        return payload;
    }

    private void publishAbsenceWarning(
            UUID recipientUserId,
            UUID groupId,
            UUID sourceSessionId,
            SourceSessionInfo sourceSession,
            AttendancePolicyService.EffectivePolicy policy,
            AttendancePolicySnapshot snapshot
    ) {
        ObjectNode payload = buildPayload(
                policy,
                snapshot.aggregate(),
                snapshot.computed(),
                groupId,
                sourceSessionId,
                sourceSession.withAttendanceStatus("ABSENT"),
                snapshot
        );

        notificationService.createOne(new NotificationService.CreateNotificationCommand(
                recipientUserId,
                groupId,
                sourceSessionId,
                NotificationType.ABSENCE_WARNING,
                "Bạn đã vắng buổi học",
                absenceWarningBody(sourceSession, snapshot, policy),
                payload,
                NotificationSeverity.WARNING,
                NotificationSourceType.ATTENDANCE_SESSION,
                sourceSessionId,
                buildAbsenceDedupKey(recipientUserId, groupId, sourceSessionId)
        ));
    }

    private String absenceWarningBody(
            SourceSessionInfo sourceSession,
            AttendancePolicySnapshot snapshot,
            AttendancePolicyService.EffectivePolicy policy
    ) {
        AttendancePolicyStudentAggregateProjection aggregate = snapshot.aggregate();
        AttendancePolicyComputation.ComputedPolicyStatus computed = snapshot.computed();

        return "Bạn đã vắng " + sessionPhrase(sourceSession)
                + " của " + coursePhrase(sourceSession)
                + ", " + classPhrase(sourceSession)
                + startAtPhrase(sourceSession, ", diễn ra vào")
                + "."
                + locationSentence(sourceSession)
                + " Hiện bạn đã vắng " + nullSafe(aggregate.getAbsentCount())
                + "/" + computed.eligibleSessionCount()
                + " buổi, tỷ lệ vắng là " + formatRate(computed.absenceRate())
                + "% và tỷ lệ điểm danh là " + formatRate(computed.attendanceRate())
                + "%."
                + absenceEvidenceSentence(snapshot)
                + thresholdSentence("cảnh báo", AttendancePolicyComputation.absenceRateThresholdForBelowRate(policy.warningBelowRate()));
    }

    private String policyWarningBody(
            SourceSessionInfo sourceSession,
            AttendancePolicySnapshot snapshot,
            AttendancePolicyService.EffectivePolicy policy
    ) {
        AttendancePolicyStudentAggregateProjection aggregate = snapshot.aggregate();
        AttendancePolicyComputation.ComputedPolicyStatus computed = snapshot.computed();

        return "Bạn đang ở mức cảnh báo chuyên cần " + coursePhrase(sourceSession)
                + ", " + classPhrase(sourceSession)
                + " sau " + sessionPhrase(sourceSession)
                + startAtPhrase(sourceSession, " ngày")
                + "."
                + locationSentence(sourceSession)
                + " Hiện bạn đã vắng " + nullSafe(aggregate.getAbsentCount())
                + "/" + computed.eligibleSessionCount()
                + " buổi (tỷ lệ vắng " + formatRate(computed.absenceRate())
                + "%, tỷ lệ điểm danh " + formatRate(computed.attendanceRate())
                + "%)."
                + absenceEvidenceSentence(snapshot)
                + thresholdSentence("cảnh báo", AttendancePolicyComputation.absenceRateThresholdForBelowRate(policy.warningBelowRate()))
                + " Vui lòng điểm danh đầy đủ các buổi tiếp theo.";
    }

    private String policyCriticalBody(
            SourceSessionInfo sourceSession,
            AttendancePolicySnapshot snapshot,
            AttendancePolicyService.EffectivePolicy policy
    ) {
        AttendancePolicyStudentAggregateProjection aggregate = snapshot.aggregate();
        AttendancePolicyComputation.ComputedPolicyStatus computed = snapshot.computed();

        return "Bạn đang ở mức nguy cơ nghiêm trọng về chuyên cần " + coursePhrase(sourceSession)
                + ", " + classPhrase(sourceSession)
                + " sau " + sessionPhrase(sourceSession)
                + startAtPhrase(sourceSession, " ngày")
                + "."
                + locationSentence(sourceSession)
                + " Hiện bạn đã vắng " + nullSafe(aggregate.getAbsentCount())
                + "/" + computed.eligibleSessionCount()
                + " buổi (tỷ lệ vắng " + formatRate(computed.absenceRate())
                + "%, tỷ lệ điểm danh " + formatRate(computed.attendanceRate())
                + "%), gần ngưỡng không đủ điều kiện dự thi."
                + absenceEvidenceSentence(snapshot)
                + thresholdSentence("nguy cơ nghiêm trọng", AttendancePolicyComputation.absenceRateThresholdForBelowRate(policy.criticalBelowRate()));
    }

    private String policyExamBannedBody(
            SourceSessionInfo sourceSession,
            AttendancePolicySnapshot snapshot,
            AttendancePolicyService.EffectivePolicy policy
    ) {
        AttendancePolicyStudentAggregateProjection aggregate = snapshot.aggregate();
        AttendancePolicyComputation.ComputedPolicyStatus computed = snapshot.computed();

        return "Bạn có thể không đủ điều kiện dự thi " + coursePhrase(sourceSession)
                + ", " + classPhrase(sourceSession)
                + ". Sau " + sessionPhrase(sourceSession)
                + startAtPhrase(sourceSession, " ngày")
                + ", bạn đã vắng " + nullSafe(aggregate.getAbsentCount())
                + "/" + computed.eligibleSessionCount()
                + " buổi (tỷ lệ vắng " + formatRate(computed.absenceRate())
                + "%, tỷ lệ điểm danh " + formatRate(computed.attendanceRate())
                + "%)."
                + locationSentence(sourceSession)
                + absenceEvidenceSentence(snapshot)
                + thresholdSentence("cấm thi", policy.examBanAbsenceRate());
    }

    private NotificationType notificationType(AttendancePolicyStatus status) {
        return switch (status) {
            case WARNING -> NotificationType.ATTENDANCE_POLICY_WARNING;
            case CRITICAL -> NotificationType.ATTENDANCE_POLICY_CRITICAL;
            case EXAM_BANNED -> NotificationType.ATTENDANCE_POLICY_EXAM_BANNED;
            case NO_DATA, NORMAL -> null;
        };
    }

    private NotificationContent notificationContent(
            AttendancePolicyStatus status,
            AttendancePolicyService.EffectivePolicy policy,
            AttendancePolicySnapshot snapshot,
            SourceSessionInfo sourceSession
    ) {
        return switch (status) {
            case WARNING -> new NotificationContent(
                    "Cảnh báo chuyên cần",
                    policyWarningBody(sourceSession, snapshot, policy),
                    NotificationSeverity.WARNING
            );
            case CRITICAL -> new NotificationContent(
                    "Nguy cơ nghiêm trọng về chuyên cần",
                    policyCriticalBody(sourceSession, snapshot, policy),
                    NotificationSeverity.CRITICAL
            );
            case EXAM_BANNED -> new NotificationContent(
                    "Không đủ điều kiện dự thi",
                    policyExamBannedBody(sourceSession, snapshot, policy),
                    NotificationSeverity.CRITICAL
            );
            case NO_DATA, NORMAL -> throw new IllegalArgumentException("No notification content for status " + status);
        };
    }

    private SourceSessionInfo findSourceSessionInfo(UUID groupId, UUID userId, UUID sourceSessionId) {
        if (sourceSessionId == null) {
            return SourceSessionInfo.empty();
        }

        if (userId == null) {
            @SuppressWarnings("unchecked")
            List<Object[]> rows = entityManager.createNativeQuery("""
                    select
                        BIN_TO_UUID(g.id, 1) as group_id,
                        g.name as group_name,
                        g.code as group_code,
                        g.class_code,
                        g.course_code,
                        BIN_TO_UUID(s.id, 1) as session_id,
                        s.title,
                        s.start_at,
                        s.end_at,
                        s.session_date,
                        g.room,
                        g.campus,
                        lecturer.full_name as lecturer_name,
                        'ABSENT' as attendance_status
                    from attendance_sessions s
                    join class_groups g
                      on g.id = s.group_id
                     and g.deleted_at is null
                    left join users lecturer
                      on lecturer.id = g.owner_user_id
                     and lecturer.deleted_at is null
                    where s.id = UUID_TO_BIN(:sessionId, 1)
                      and s.group_id = UUID_TO_BIN(:groupId, 1)
                      and s.deleted_at is null
                    limit 1
                    """)
                    .setParameter("groupId", groupId.toString())
                    .setParameter("sessionId", sourceSessionId.toString())
                    .getResultList();

            return toSourceSessionInfo(rows);
        }

        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("""
                select
                    BIN_TO_UUID(g.id, 1) as group_id,
                    g.name as group_name,
                    g.code as group_code,
                    g.class_code,
                    g.course_code,
                    BIN_TO_UUID(s.id, 1) as session_id,
                    s.title,
                    s.start_at,
                    s.end_at,
                    s.session_date,
                    g.room,
                    g.campus,
                    lecturer.full_name as lecturer_name,
                    coalesce(sa.attendance_status, 'ABSENT') as attendance_status
                from attendance_sessions s
                join class_groups g
                  on g.id = s.group_id
                 and g.deleted_at is null
                left join users lecturer
                  on lecturer.id = g.owner_user_id
                 and lecturer.deleted_at is null
                left join session_attendance sa
                  on sa.session_id = s.id
                 and sa.user_id = UUID_TO_BIN(:userId, 1)
                where s.id = UUID_TO_BIN(:sessionId, 1)
                  and s.group_id = UUID_TO_BIN(:groupId, 1)
                  and s.deleted_at is null
                limit 1
                """)
                .setParameter("groupId", groupId.toString())
                .setParameter("userId", userId.toString())
                .setParameter("sessionId", sourceSessionId.toString())
                .getResultList();

        return toSourceSessionInfo(rows);
    }

    private SourceSessionInfo toSourceSessionInfo(List<Object[]> rows) {
        if (rows.isEmpty()) {
            return SourceSessionInfo.empty();
        }

        Object[] row = rows.get(0);
        return new SourceSessionInfo(
                toUuid(row[0]),
                clean(row[1]),
                clean(row[2]),
                clean(row[3]),
                null,
                clean(row[4]),
                toUuid(row[5]),
                clean(row[6]),
                toInstant(row[7]),
                toInstant(row[8]),
                toLocalDate(row[9]),
                clean(row[10]),
                buildLocation(row[10], row[11]),
                clean(row[12]),
                clean(row[13])
        );
    }

    private List<AbsenceSessionEvidence> findAbsenceSessions(UUID groupId, UUID userId) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("""
                select
                    BIN_TO_UUID(s.id, 1) as session_id,
                    s.title,
                    s.session_date,
                    s.start_at,
                    s.end_at,
                    g.room,
                    g.campus,
                    coalesce(sa.attendance_status, 'ABSENT') as attendance_status,
                    case
                        when sa.session_id is null then 'MISSING_ATTENDANCE_ROW'
                        else 'SESSION_ATTENDANCE'
                    end as source,
                    s.end_at as closed_at
                from group_members gm
                join attendance_sessions s
                  on s.group_id = gm.group_id
                 and s.status = 'CLOSED'
                 and s.deleted_at is null
                 and s.start_at >= coalesce(gm.joined_at, gm.created_at)
                join class_groups g
                  on g.id = s.group_id
                 and g.deleted_at is null
                left join session_attendance sa
                  on sa.session_id = s.id
                 and sa.user_id = gm.user_id
                where gm.group_id = UUID_TO_BIN(:groupId, 1)
                  and gm.user_id = UUID_TO_BIN(:userId, 1)
                  and gm.member_status = 'APPROVED'
                  and coalesce(sa.attendance_status, 'ABSENT') = 'ABSENT'
                order by s.start_at asc, s.id asc
                """)
                .setParameter("groupId", groupId.toString())
                .setParameter("userId", userId.toString())
                .getResultList();

        return rows.stream()
                .map(row -> new AbsenceSessionEvidence(
                        toUuid(row[0]),
                        clean(row[1]),
                        toLocalDate(row[2]),
                        toInstant(row[3]),
                        toInstant(row[4]),
                        clean(row[5]),
                        buildLocation(row[5], row[6]),
                        row[7] == null ? "ABSENT" : row[7].toString(),
                        row[8] == null ? "SESSION_ATTENDANCE" : row[8].toString(),
                        toInstant(row[9])
                ))
                .toList();
    }

    private List<SessionClosureMemberStatus> findSessionClosureMemberStatuses(UUID groupId, UUID sourceSessionId) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("""
                select
                    BIN_TO_UUID(gm.user_id, 1) as user_id,
                    coalesce(sa.attendance_status, 'ABSENT') as attendance_status
                from group_members gm
                join attendance_sessions s
                  on s.group_id = gm.group_id
                 and s.id = UUID_TO_BIN(:sessionId, 1)
                 and s.status = 'CLOSED'
                 and s.deleted_at is null
                 and s.start_at >= coalesce(gm.joined_at, gm.created_at)
                left join session_attendance sa
                  on sa.session_id = s.id
                 and sa.user_id = gm.user_id
                where gm.group_id = UUID_TO_BIN(:groupId, 1)
                  and gm.member_status = 'APPROVED'
                  and gm.role = 'MEMBER'
                order by gm.user_id
                """)
                .setParameter("groupId", groupId.toString())
                .setParameter("sessionId", sourceSessionId.toString())
                .getResultList();

        return rows.stream()
                .map(row -> new SessionClosureMemberStatus(
                        UUID.fromString(row[0].toString()),
                        row[1] == null ? "ABSENT" : row[1].toString()
                ))
                .toList();
    }

    private void putThresholds(ObjectNode payload, AttendancePolicyService.EffectivePolicy policy) {
        ObjectNode warning = payload.putObject("warningThresholds");
        putNullableDecimal(warning, "belowRate", policy.warningBelowRate());
        putNullableDecimal(warning, "absenceRate", AttendancePolicyComputation.absenceRateThresholdForBelowRate(policy.warningBelowRate()));
        putNullableInteger(warning, "absentCount", policy.warningAbsentCount());

        ObjectNode critical = payload.putObject("criticalThresholds");
        putNullableDecimal(critical, "belowRate", policy.criticalBelowRate());
        putNullableDecimal(critical, "absenceRate", AttendancePolicyComputation.absenceRateThresholdForBelowRate(policy.criticalBelowRate()));
        putNullableInteger(critical, "absentCount", policy.criticalAbsentCount());

        ObjectNode examBan = payload.putObject("examBanThresholds");
        putNullableDecimal(examBan, "absenceRate", policy.examBanAbsenceRate());
        putNullableInteger(examBan, "absentCount", policy.examBanAbsentCount());
    }

    private void putAcademicContext(
            ObjectNode payload,
            UUID fallbackGroupId,
            UUID fallbackSessionId,
            SourceSessionInfo sourceSession
    ) {
        UUID groupId = sourceSession.groupId() == null ? fallbackGroupId : sourceSession.groupId();
        UUID sessionId = sourceSession.sessionId() == null ? fallbackSessionId : sourceSession.sessionId();
        String groupName = clean(sourceSession.groupName());
        String groupCode = clean(sourceSession.groupCode());
        String classCode = clean(sourceSession.classCode());
        String courseName = clean(sourceSession.courseName());
        String courseCode = clean(sourceSession.courseCode());

        putNullableUuid(payload, "groupId", groupId);
        putNullableString(payload, "groupName", groupName);
        putNullableString(payload, "className", groupName);
        putNullableString(payload, "groupCode", groupCode);
        putNullableString(payload, "classCode", firstText(classCode, groupCode));
        putNullableString(payload, "courseName", courseName);
        putNullableString(payload, "subjectName", courseName);
        putNullableString(payload, "courseCode", courseCode);
        putNullableString(payload, "subjectCode", courseCode);

        putNullableUuid(payload, "sessionId", sessionId);
        putNullableString(payload, "sessionTitle", clean(sourceSession.title()));
        putNullableString(payload, "sessionName", clean(sourceSession.title()));
        putNullableInstant(payload, "sessionStartAt", sourceSession.startAt());
        putNullableInstant(payload, "sessionEndAt", sourceSession.endAt());
        putNullableString(payload, "triggerSessionTitle", clean(sourceSession.title()));
        putNullableString(payload, "triggerSessionName", clean(sourceSession.title()));
        putNullableInstant(payload, "triggerSessionStartAt", sourceSession.startAt());
        putNullableInstant(payload, "triggerSessionEndAt", sourceSession.endAt());
        putNullableString(payload, "triggerSessionRoom", clean(sourceSession.room()));
        putNullableString(payload, "triggerSessionLocation", clean(sourceSession.location()));
        if (sourceSession.sessionDate() == null) {
            payload.putNull("sessionDate");
            payload.putNull("triggerSessionDate");
        } else {
            payload.put("sessionDate", sourceSession.sessionDate().toString());
            payload.put("triggerSessionDate", sourceSession.sessionDate().toString());
        }

        putNullableString(payload, "room", clean(sourceSession.room()));
        putNullableString(payload, "location", clean(sourceSession.location()));
        putNullableString(payload, "lecturerName", clean(sourceSession.lecturerName()));
        putNullableString(payload, "attendanceStatus", clean(sourceSession.attendanceStatus()));
    }

    private void putAbsenceSessions(ObjectNode payload, List<AbsenceSessionEvidence> absenceSessions) {
        ArrayNode items = payload.putArray("absenceSessions");
        for (AbsenceSessionEvidence evidence : absenceSessions) {
            ObjectNode item = items.addObject();
            putNullableUuid(item, "sessionId", evidence.sessionId());
            putNullableString(item, "sessionTitle", clean(evidence.title()));
            putNullableString(item, "sessionName", clean(evidence.title()));
            if (evidence.sessionDate() == null) {
                item.putNull("sessionDate");
            } else {
                item.put("sessionDate", evidence.sessionDate().toString());
            }
            putNullableInstant(item, "startAt", evidence.startAt());
            putNullableInstant(item, "endAt", evidence.endAt());
            putNullableString(item, "room", clean(evidence.room()));
            putNullableString(item, "location", clean(evidence.location()));
            item.put("attendanceStatus", "ABSENT");
            putNullableString(item, "source", clean(evidence.source()));
            putNullableInstant(item, "closedAt", evidence.closedAt());
        }
    }

    private void putNullableString(ObjectNode node, String field, String value) {
        if (value == null) {
            node.putNull(field);
        } else {
            node.put(field, value);
        }
    }

    private void putNullableUuid(ObjectNode node, String field, UUID value) {
        if (value == null) {
            node.putNull(field);
        } else {
            node.put(field, value.toString());
        }
    }

    private void putNullableInstant(ObjectNode node, String field, Instant value) {
        if (value == null) {
            node.putNull(field);
        } else {
            node.put(field, value.toString());
        }
    }

    private void putNullableDecimal(ObjectNode node, String field, BigDecimal value) {
        if (value == null) {
            node.putNull(field);
        } else {
            node.put(field, value);
        }
    }

    private void putNullableInteger(ObjectNode node, String field, Integer value) {
        if (value == null) {
            node.putNull(field);
        } else {
            node.put(field, value);
        }
    }

    private Instant toInstant(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Timestamp ts) {
            return ts.toInstant();
        }
        if (value instanceof java.util.Date d) {
            return d.toInstant();
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        throw new IllegalStateException("Unsupported timestamp type: " + value.getClass());
    }

    private LocalDate toLocalDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof java.sql.Date date) {
            return date.toLocalDate();
        }
        if (value instanceof Timestamp ts) {
            return ts.toLocalDateTime().toLocalDate();
        }
        if (value instanceof java.util.Date d) {
            return new java.sql.Date(d.getTime()).toLocalDate();
        }
        return LocalDate.parse(value.toString());
    }

    private UUID toUuid(Object value) {
        if (value == null) {
            return null;
        }
        return UUID.fromString(value.toString());
    }

    private String buildLocation(Object room, Object campus) {
        String roomText = clean(room);
        String campusText = clean(campus);
        if (roomText == null) {
            return campusText;
        }
        if (campusText == null) {
            return roomText;
        }
        return roomText + ", " + campusText;
    }

    private String sessionPhrase(SourceSessionInfo sourceSession) {
        String title = clean(sourceSession.title());
        return title == null ? "buổi học này" : "buổi \"" + title + "\"";
    }

    private String coursePhrase(SourceSessionInfo sourceSession) {
        String courseName = clean(sourceSession.courseName());
        String courseCode = clean(sourceSession.courseCode());
        if (courseCode != null && courseName != null) {
            return "môn " + courseCode + " - " + courseName;
        }
        if (courseCode != null) {
            return "môn " + courseCode;
        }
        return courseName == null ? "môn học này" : "môn " + courseName;
    }

    private String classPhrase(SourceSessionInfo sourceSession) {
        String className = clean(sourceSession.groupName());
        String classCode = firstText(sourceSession.classCode(), sourceSession.groupCode());
        if (classCode != null && className != null) {
            return "lớp " + classCode + " - " + className;
        }
        if (classCode != null) {
            return "lớp " + classCode;
        }
        return className == null ? "lớp học này" : "lớp " + className;
    }

    private String startAtPhrase(SourceSessionInfo sourceSession, String prefix) {
        if (sourceSession.startAt() == null) {
            return "";
        }
        return prefix + " " + sourceSession.startAt();
    }

    private String locationSentence(SourceSessionInfo sourceSession) {
        String location = firstText(sourceSession.location(), sourceSession.room());
        return location == null ? "" : " Địa điểm: " + location + ".";
    }

    private String absenceEvidenceSentence(AttendancePolicySnapshot snapshot) {
        String summary = absenceDatesSummary(snapshot.absenceSessions());
        return summary == null ? "" : " Các buổi vắng được tính: " + summary + ".";
    }

    private String absenceDatesSummary(List<AbsenceSessionEvidence> absenceSessions) {
        if (absenceSessions == null || absenceSessions.isEmpty()) {
            return null;
        }

        int visibleCount = Math.min(absenceSessions.size(), 5);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < visibleCount; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(absenceSessionLabel(absenceSessions.get(i)));
        }
        int remaining = absenceSessions.size() - visibleCount;
        if (remaining > 0) {
            sb.append(", và ").append(remaining).append(" buổi khác");
        }
        return sb.toString();
    }

    private String absenceSessionLabel(AbsenceSessionEvidence evidence) {
        String when = evidence.startAt() != null
                ? evidence.startAt().toString()
                : evidence.sessionDate() == null ? null : evidence.sessionDate().toString();
        String title = clean(evidence.title());
        if (title == null) {
            return when == null ? "buổi vắng" : when;
        }
        return when == null ? "\"" + title + "\"" : "\"" + title + "\" " + when;
    }

    private String thresholdSentence(String label, BigDecimal value) {
        if (value == null) {
            return "";
        }
        return " Ngưỡng " + label + " của lớp là " + formatRate(value) + "%.";
    }

    private String formatRate(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        BigDecimal stripped = value.stripTrailingZeros();
        if (stripped.scale() < 0) {
            stripped = stripped.setScale(0);
        }
        return stripped.toPlainString();
    }

    private String firstText(String first, String second) {
        String cleanFirst = clean(first);
        return cleanFirst == null ? clean(second) : cleanFirst;
    }

    private String clean(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

    private String buildPolicyDedupKey(
            NotificationType type,
            UUID recipientUserId,
            UUID groupId,
            UUID sourceSessionId
    ) {
        String raw = String.join("|",
                "attendance-policy",
                type.name(),
                recipientUserId.toString(),
                groupId.toString(),
                sourceSessionId == null ? "null" : sourceSessionId.toString()
        );
        return sha256Hex(raw);
    }

    private String buildAbsenceDedupKey(
            UUID recipientUserId,
            UUID groupId,
            UUID sourceSessionId
    ) {
        String raw = String.join("|",
                "absence",
                recipientUserId.toString(),
                groupId.toString(),
                sourceSessionId.toString()
        );
        return sha256Hex(raw);
    }

    private String sha256Hex(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is not available", ex);
        }
    }

    private long nullSafe(Long value) {
        return value == null ? 0L : value;
    }

    private record NotificationContent(String title, String body, NotificationSeverity severity) {
    }

    private record AttendancePolicySnapshot(
            AttendancePolicyStudentAggregateProjection aggregate,
            AttendancePolicyComputation.ComputedPolicyStatus computed,
            Instant computedAt,
            List<AbsenceSessionEvidence> absenceSessions
    ) {
    }

    private record AbsenceSessionEvidence(
            UUID sessionId,
            String title,
            LocalDate sessionDate,
            Instant startAt,
            Instant endAt,
            String room,
            String location,
            String attendanceStatus,
            String source,
            Instant closedAt
    ) {
    }

    private record SourceSessionInfo(
            UUID groupId,
            String groupName,
            String groupCode,
            String classCode,
            String courseName,
            String courseCode,
            UUID sessionId,
            String title,
            Instant startAt,
            Instant endAt,
            LocalDate sessionDate,
            String room,
            String location,
            String lecturerName,
            String attendanceStatus
    ) {
        static SourceSessionInfo empty() {
            return new SourceSessionInfo(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        }

        SourceSessionInfo withAttendanceStatus(String value) {
            return new SourceSessionInfo(
                    groupId,
                    groupName,
                    groupCode,
                    classCode,
                    courseName,
                    courseCode,
                    sessionId,
                    title,
                    startAt,
                    endAt,
                    sessionDate,
                    room,
                    location,
                    lecturerName,
                    value
            );
        }
    }

    private record SessionClosureMemberStatus(UUID userId, String attendanceStatus) {
    }
}
