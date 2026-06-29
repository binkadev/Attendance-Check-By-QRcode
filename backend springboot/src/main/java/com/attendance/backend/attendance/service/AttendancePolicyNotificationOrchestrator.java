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

        AttendancePolicyStudentAggregateProjection aggregate =
                attendancePolicyQueryRepository.aggregateForApprovedMember(
                        groupId.toString(),
                        userId.toString()
                );

        if (aggregate == null) {
            return;
        }

        AttendancePolicyComputation.ComputedPolicyStatus computed =
                AttendancePolicyComputation.compute(
                        policy,
                        nullSafe(aggregate.getPresentCount()),
                        nullSafe(aggregate.getLateCount()),
                        nullSafe(aggregate.getAbsentCount()),
                        nullSafe(aggregate.getExcusedCount())
                );

        UUID recipientUserId = UUID.fromString(aggregate.getUserId());

        NotificationType notificationType = notificationType(computed.policyStatus());
        if (notificationType == null) {
            return;
        }

        NotificationContent content = notificationContent(computed.policyStatus());
        SourceSessionInfo sourceSession = findSourceSessionInfo(groupId, recipientUserId, sourceSessionId);

        notificationService.createOne(new NotificationService.CreateNotificationCommand(
                recipientUserId,
                groupId,
                sourceSessionId,
                notificationType,
                content.title(),
                content.body(),
                buildPayload(policy, aggregate, computed, sourceSessionId, sourceSession),
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
            UUID sourceSessionId,
            SourceSessionInfo sourceSession
    ) {
        ObjectNode payload = objectMapper.createObjectNode();

        if (sourceSessionId != null) {
            payload.put("sessionId", sourceSessionId.toString());
        } else {
            payload.putNull("sessionId");
        }
        putNullableString(payload, "sessionTitle", sourceSession.title());
        putNullableString(payload, "sessionName", sourceSession.title());
        if (sourceSession.startAt() != null) {
            payload.put("sessionStartAt", sourceSession.startAt().toString());
        } else {
            payload.putNull("sessionStartAt");
        }
        putNullableString(payload, "attendanceStatus", sourceSession.attendanceStatus());

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

        return payload;
    }

    private NotificationType notificationType(AttendancePolicyStatus status) {
        return switch (status) {
            case WARNING -> NotificationType.ATTENDANCE_POLICY_WARNING;
            case CRITICAL -> NotificationType.ATTENDANCE_POLICY_CRITICAL;
            case EXAM_BANNED -> NotificationType.ATTENDANCE_POLICY_EXAM_BANNED;
            case NO_DATA, NORMAL -> null;
        };
    }

    private NotificationContent notificationContent(AttendancePolicyStatus status) {
        return switch (status) {
            case WARNING -> new NotificationContent(
                    "Cảnh báo chuyên cần",
                    "Tỷ lệ vắng của bạn đã chạm mức cảnh báo.",
                    NotificationSeverity.WARNING
            );
            case CRITICAL -> new NotificationContent(
                    "Nguy cơ nghiêm trọng về chuyên cần",
                    "Tình trạng chuyên cần của bạn đang ở mức nguy cơ nghiêm trọng.",
                    NotificationSeverity.CRITICAL
            );
            case EXAM_BANNED -> new NotificationContent(
                    "Không đủ điều kiện dự thi",
                    "Bạn không đủ điều kiện dự thi theo chính sách chuyên cần hiện tại.",
                    NotificationSeverity.CRITICAL
            );
            case NO_DATA, NORMAL -> throw new IllegalArgumentException("No notification content for status " + status);
        };
    }

    private SourceSessionInfo findSourceSessionInfo(UUID groupId, UUID userId, UUID sourceSessionId) {
        if (sourceSessionId == null) {
            return SourceSessionInfo.empty();
        }

        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("""
                select
                    s.title,
                    s.start_at,
                    coalesce(sa.attendance_status, 'ABSENT') as attendance_status
                from attendance_sessions s
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

        if (rows.isEmpty()) {
            return SourceSessionInfo.empty();
        }

        Object[] row = rows.get(0);
        return new SourceSessionInfo(
                row[0] == null ? null : row[0].toString(),
                toInstant(row[1]),
                row[2] == null ? null : row[2].toString()
        );
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

    private void putNullableString(ObjectNode node, String field, String value) {
        if (value == null) {
            node.putNull(field);
        } else {
            node.put(field, value);
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

    private record SourceSessionInfo(String title, Instant startAt, String attendanceStatus) {
        static SourceSessionInfo empty() {
            return new SourceSessionInfo(null, null, null);
        }
    }
}
