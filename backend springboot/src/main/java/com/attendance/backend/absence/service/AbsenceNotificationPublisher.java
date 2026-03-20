package com.attendance.backend.absence.service;

import com.attendance.backend.domain.entity.AbsenceRequest;
import com.attendance.backend.domain.enums.NotificationSeverity;
import com.attendance.backend.domain.enums.NotificationSourceType;
import com.attendance.backend.domain.enums.NotificationType;
import com.attendance.backend.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AbsenceNotificationPublisher {

    private final NotificationService notificationService;
    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;

    public AbsenceNotificationPublisher(
            NotificationService notificationService,
            EntityManager entityManager,
            ObjectMapper objectMapper
    ) {
        this.notificationService = notificationService;
        this.entityManager = entityManager;
        this.objectMapper = objectMapper;
    }

    public void onCreated(AbsenceRequest request) {
        List<UUID> lecturerIds = findLecturerRecipientIds(request.groupId);
        if (lecturerIds.isEmpty()) {
            return;
        }

        List<NotificationService.CreateNotificationCommand> commands = new ArrayList<>();
        for (UUID lecturerId : lecturerIds) {
            commands.add(new NotificationService.CreateNotificationCommand(
                    lecturerId,
                    request.groupId,
                    request.linkedSessionId,
                    NotificationType.ABSENCE_REQUEST_CREATED,
                    "Có đơn xin vắng mới",
                    "Một sinh viên vừa gửi đơn xin vắng cần được xem xét.",
                    buildPayload(request, "PENDING"),
                    NotificationSeverity.INFO,
                    NotificationSourceType.ABSENCE_REQUEST,
                    request.id,
                    buildDedupKey(NotificationType.ABSENCE_REQUEST_CREATED, lecturerId, request.id)
            ));
        }

        notificationService.createMany(commands);
    }

    public void onApproved(AbsenceRequest request, UUID reviewerUserId) {
        notificationService.createOne(new NotificationService.CreateNotificationCommand(
                request.requesterUserId,
                request.groupId,
                request.linkedSessionId,
                NotificationType.ABSENCE_REQUEST_APPROVED,
                "Đơn xin vắng đã được duyệt",
                "Đơn xin vắng của bạn đã được giảng viên duyệt.",
                buildPayload(request, "APPROVED").put("reviewerUserId", reviewerUserId.toString()),
                NotificationSeverity.INFO,
                NotificationSourceType.ABSENCE_REQUEST,
                request.id,
                buildDedupKey(NotificationType.ABSENCE_REQUEST_APPROVED, request.requesterUserId, request.id)
        ));
    }

    public void onRejected(AbsenceRequest request, UUID reviewerUserId) {
        notificationService.createOne(new NotificationService.CreateNotificationCommand(
                request.requesterUserId,
                request.groupId,
                request.linkedSessionId,
                NotificationType.ABSENCE_REQUEST_REJECTED,
                "Đơn xin vắng bị từ chối",
                "Đơn xin vắng của bạn đã bị từ chối.",
                buildPayload(request, "REJECTED").put("reviewerUserId", reviewerUserId.toString()),
                NotificationSeverity.WARNING,
                NotificationSourceType.ABSENCE_REQUEST,
                request.id,
                buildDedupKey(NotificationType.ABSENCE_REQUEST_REJECTED, request.requesterUserId, request.id)
        ));
    }

    public void onCancelled(AbsenceRequest request) {
        List<UUID> lecturerIds = findLecturerRecipientIds(request.groupId);
        if (lecturerIds.isEmpty()) {
            return;
        }

        List<NotificationService.CreateNotificationCommand> commands = new ArrayList<>();
        for (UUID lecturerId : lecturerIds) {
            commands.add(new NotificationService.CreateNotificationCommand(
                    lecturerId,
                    request.groupId,
                    request.linkedSessionId,
                    NotificationType.ABSENCE_REQUEST_CANCELLED,
                    "Đơn xin vắng đã bị hủy",
                    "Sinh viên đã hủy một đơn xin vắng trước đó.",
                    buildPayload(request, "CANCELLED"),
                    NotificationSeverity.INFO,
                    NotificationSourceType.ABSENCE_REQUEST,
                    request.id,
                    buildDedupKey(NotificationType.ABSENCE_REQUEST_CANCELLED, lecturerId, request.id)
            ));
        }

        notificationService.createMany(commands);
    }

    public void onReverted(AbsenceRequest request, UUID actorUserId) {
        notificationService.createOne(new NotificationService.CreateNotificationCommand(
                request.requesterUserId,
                request.groupId,
                request.linkedSessionId,
                NotificationType.ABSENCE_REQUEST_REVERTED,
                "Đơn xin vắng đã bị hoàn tác",
                "Quyết định duyệt đơn xin vắng của bạn đã bị hoàn tác.",
                buildPayload(request, "REVERTED").put("actorUserId", actorUserId.toString()),
                NotificationSeverity.WARNING,
                NotificationSourceType.ABSENCE_REQUEST,
                request.id,
                buildDedupKey(NotificationType.ABSENCE_REQUEST_REVERTED, request.requesterUserId, request.id)
        ));
    }

    private ObjectNode buildPayload(AbsenceRequest request, String status) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("requestId", request.id.toString());
        payload.put("groupId", request.groupId.toString());
        payload.put("requesterUserId", request.requesterUserId.toString());
        payload.put("linkedSessionId", request.linkedSessionId.toString());
        payload.put("requestStatus", status);
        payload.put("reason", request.reason);

        if (request.evidenceUrl != null && !request.evidenceUrl.isBlank()) {
            payload.put("evidenceUrl", request.evidenceUrl);
        }
        if (request.reviewerNote != null && !request.reviewerNote.isBlank()) {
            payload.put("reviewerNote", request.reviewerNote);
        }
        if (request.revertNote != null && !request.revertNote.isBlank()) {
            payload.put("revertNote", request.revertNote);
        }

        return payload;
    }

    private String buildDedupKey(NotificationType type, UUID recipientUserId, UUID requestId) {
        String raw = String.join("|",
                "absence-request",
                type.name(),
                recipientUserId.toString(),
                requestId.toString()
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

    @SuppressWarnings("unchecked")
    private List<UUID> findLecturerRecipientIds(UUID groupId) {
        List<String> rows = entityManager.createNativeQuery("""
            select BIN_TO_UUID(gm.user_id, 1)
            from group_members gm
            where gm.group_id = UUID_TO_BIN(:groupId, 1)
              and gm.member_status = 'APPROVED'
              and gm.role in ('OWNER', 'CO_HOST')
        """)
                .setParameter("groupId", groupId.toString())
                .getResultList();

        return rows.stream().map(UUID::fromString).toList();
    }
}