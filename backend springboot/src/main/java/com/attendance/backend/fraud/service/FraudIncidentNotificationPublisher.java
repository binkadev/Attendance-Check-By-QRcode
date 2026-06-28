package com.attendance.backend.fraud.service;

import com.attendance.backend.domain.enums.NotificationSeverity;
import com.attendance.backend.domain.enums.NotificationSourceType;
import com.attendance.backend.domain.enums.NotificationType;
import com.attendance.backend.fraud.domain.FraudIncidentSeverity;
import com.attendance.backend.fraud.domain.FraudIncidentType;
import com.attendance.backend.fraud.entity.FraudIncident;
import com.attendance.backend.notification.service.NotificationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class FraudIncidentNotificationPublisher {

    private static final String SHARED_DEVICE_TITLE = "Cảnh báo gian lận điểm danh";
    private static final String SHARED_DEVICE_BODY =
            "Một thiết bị đã được dùng để điểm danh nhiều tài khoản trong cùng phiên.";

    private final NotificationService notificationService;
    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;

    public FraudIncidentNotificationPublisher(
            NotificationService notificationService,
            EntityManager entityManager,
            ObjectMapper objectMapper
    ) {
        this.notificationService = notificationService;
        this.entityManager = entityManager;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onOpenedOrBumped(FraudIncident incident) {
        if (incident == null || incident.getType() != FraudIncidentType.SHARED_DEVICE_MULTI_ACCOUNT) {
            return;
        }

        List<UUID> recipientIds = findFraudReviewerRecipientIds(incident.getGroupId());
        if (recipientIds.isEmpty()) {
            return;
        }

        List<NotificationService.CreateNotificationCommand> commands = new ArrayList<>();
        for (UUID recipientId : recipientIds) {
            commands.add(new NotificationService.CreateNotificationCommand(
                    recipientId,
                    incident.getGroupId(),
                    incident.getSessionId(),
                    notificationType(incident.getSeverity()),
                    SHARED_DEVICE_TITLE,
                    SHARED_DEVICE_BODY,
                    buildPayload(incident),
                    notificationSeverity(incident.getSeverity()),
                    NotificationSourceType.FRAUD_INCIDENT,
                    incident.getId(),
                    buildDedupKey(recipientId, incident.getId())
            ));
        }

        notificationService.createMany(commands);
    }

    private ObjectNode buildPayload(FraudIncident incident) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("fraudIncidentId", incident.getId().toString());
        payload.put("fraudType", incident.getType().name());
        payload.put("groupId", incident.getGroupId().toString());

        if (incident.getSessionId() != null) {
            payload.put("sessionId", incident.getSessionId().toString());
        }
        if (incident.getUserId() != null) {
            payload.put("userId", incident.getUserId().toString());
        }

        String deviceId = extractDeviceId(incident.getEvidenceJson());
        if (deviceId != null && !deviceId.isBlank()) {
            payload.put("deviceId", deviceId);
        }

        if (incident.getSeverity() != null) {
            payload.put("severity", incident.getSeverity().name());
        }
        if (incident.getOccurrenceCount() != null) {
            payload.put("occurrenceCount", incident.getOccurrenceCount());
        }
        if (incident.getLastDetectedAt() != null) {
            payload.put("lastDetectedAt", incident.getLastDetectedAt().toString());
        }

        return payload;
    }

    private NotificationType notificationType(FraudIncidentSeverity severity) {
        return severity == FraudIncidentSeverity.HIGH || severity == FraudIncidentSeverity.CRITICAL
                ? NotificationType.FRAUD_ALERT_CRITICAL
                : NotificationType.FRAUD_ALERT_RISK;
    }

    private NotificationSeverity notificationSeverity(FraudIncidentSeverity severity) {
        return severity == FraudIncidentSeverity.HIGH || severity == FraudIncidentSeverity.CRITICAL
                ? NotificationSeverity.CRITICAL
                : NotificationSeverity.WARNING;
    }

    private String extractDeviceId(String evidenceJson) {
        if (evidenceJson == null || evidenceJson.isBlank()) {
            return null;
        }

        try {
            JsonNode evidence = objectMapper.readTree(evidenceJson);
            JsonNode deviceId = evidence.get("deviceId");
            if (deviceId != null && deviceId.isTextual() && !deviceId.asText().isBlank()) {
                return deviceId.asText();
            }

            JsonNode sampleDeviceIds = evidence.get("sampleDeviceIds");
            if (sampleDeviceIds != null && sampleDeviceIds.isArray() && !sampleDeviceIds.isEmpty()) {
                String firstDeviceId = sampleDeviceIds.get(0).asText(null);
                if (firstDeviceId != null && !firstDeviceId.isBlank()) {
                    return firstDeviceId;
                }
            }
        } catch (Exception ignored) {
            return null;
        }

        return null;
    }

    private String buildDedupKey(UUID recipientUserId, UUID fraudIncidentId) {
        String raw = String.join("|",
                "fraud-incident",
                FraudIncidentType.SHARED_DEVICE_MULTI_ACCOUNT.name(),
                recipientUserId.toString(),
                fraudIncidentId.toString()
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
    private List<UUID> findFraudReviewerRecipientIds(UUID groupId) {
        List<String> rows = entityManager.createNativeQuery("""
                select distinct BIN_TO_UUID(gm.user_id, 1)
                from group_members gm
                join users u on u.id = gm.user_id
                where gm.group_id = UUID_TO_BIN(:groupId, 1)
                  and gm.member_status = 'APPROVED'
                  and gm.removed_at is null
                  and u.deleted_at is null
                  and (
                        gm.role in ('OWNER', 'CO_HOST')
                        or u.platform_role = 'ADMIN'
                  )
                """)
                .setParameter("groupId", groupId.toString())
                .getResultList();

        return rows.stream().map(UUID::fromString).toList();
    }
}
