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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@Service
public class AttendancePolicyNotificationOrchestrator {

    private final AttendancePolicyService attendancePolicyService;
    private final AttendancePolicyQueryRepository attendancePolicyQueryRepository;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

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

        if (computed.policyStatus() == AttendancePolicyStatus.WARNING) {
            notificationService.createOne(new NotificationService.CreateNotificationCommand(
                    recipientUserId,
                    groupId,
                    sourceSessionId,
                    NotificationType.ATTENDANCE_POLICY_WARNING,
                    "Cảnh báo điểm danh",
                    "Tỷ lệ điểm danh của bạn đang ở mức cảnh báo.",
                    buildPayload(policy, aggregate, computed),
                    NotificationSeverity.WARNING,
                    NotificationSourceType.ATTENDANCE_POLICY,
                    sourceSessionId,
                    buildPolicyDedupKey(
                            NotificationType.ATTENDANCE_POLICY_WARNING,
                            recipientUserId,
                            groupId,
                            sourceSessionId
                    )
            ));
            return;
        }

        if (computed.policyStatus() == AttendancePolicyStatus.CRITICAL) {
            notificationService.createOne(new NotificationService.CreateNotificationCommand(
                    recipientUserId,
                    groupId,
                    sourceSessionId,
                    NotificationType.ATTENDANCE_POLICY_CRITICAL,
                    "Nguy cơ vi phạm điểm danh",
                    "Tình trạng điểm danh của bạn đã ở mức nghiêm trọng.",
                    buildPayload(policy, aggregate, computed),
                    NotificationSeverity.CRITICAL,
                    NotificationSourceType.ATTENDANCE_POLICY,
                    sourceSessionId,
                    buildPolicyDedupKey(
                            NotificationType.ATTENDANCE_POLICY_CRITICAL,
                            recipientUserId,
                            groupId,
                            sourceSessionId
                    )
            ));
        }
    }

    private ObjectNode buildPayload(
            AttendancePolicyService.EffectivePolicy policy,
            AttendancePolicyStudentAggregateProjection aggregate,
            AttendancePolicyComputation.ComputedPolicyStatus computed
    ) {
        ObjectNode payload = objectMapper.createObjectNode();

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

        if (computed.attendanceRate() != null) {
            payload.put("attendanceRate", computed.attendanceRate());
        } else {
            payload.putNull("attendanceRate");
        }

        payload.put("policyStatus", computed.policyStatus().name());

        ArrayNode reasons = payload.putArray("breachReasons");
        for (var reason : computed.breachReasons()) {
            reasons.add(reason.name());
        }

        payload.put("lateWeight", policy.lateWeight());
        payload.put("warningBelowRate", policy.warningBelowRate());

        if (policy.criticalBelowRate() != null) {
            payload.put("criticalBelowRate", policy.criticalBelowRate());
        } else {
            payload.putNull("criticalBelowRate");
        }

        if (policy.warningAbsentCount() != null) {
            payload.put("warningAbsentCount", policy.warningAbsentCount());
        } else {
            payload.putNull("warningAbsentCount");
        }

        if (policy.criticalAbsentCount() != null) {
            payload.put("criticalAbsentCount", policy.criticalAbsentCount());
        } else {
            payload.putNull("criticalAbsentCount");
        }

        return payload;
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
}