package com.attendance.backend.fraud.dto;

import com.attendance.backend.auth.repository.UserRepository;
import com.attendance.backend.domain.entity.User;
import com.attendance.backend.fraud.domain.FraudIncidentSeverity;
import com.attendance.backend.fraud.domain.FraudIncidentStatus;
import com.attendance.backend.fraud.entity.FraudIncident;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class FraudIncidentMapper {

    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;

    public FraudIncidentMapper(ObjectMapper objectMapper, UserRepository userRepository) {
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
    }

    public PageFraudIncidentResponse toPage(Page<FraudIncident> page) {
        List<FraudIncidentResponse> items = page.getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        return new PageFraudIncidentResponse(
                items,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    public FraudIncidentResponse toResponse(FraudIncident incident) {
        JsonNode evidence = parseEvidence(incident.getEvidenceJson());

        FraudIncidentEvidenceSummaryResponse evidenceSummary = toEvidenceSummary(incident, evidence);
        FraudIncidentStudentResponse student = toStudent(incident.getUserId());

        String title = titleOf(incident);
        String description = descriptionOf(incident, evidenceSummary);
        Integer confidence = confidenceOf(incident);
        String displayStatus = displayStatusOf(incident.getStatus());

        return new FraudIncidentResponse(
                incident.getId(),
                incident.getGroupId(),
                incident.getSessionId(),
                incident.getUserId(),
                incident.getType(),
                incident.getSeverity(),
                incident.getStatus(),
                displayStatus,
                title,
                description,
                confidence,
                student,
                incident.getFirstDetectedAt(),
                incident.getLastDetectedAt(),
                incident.getOccurrenceCount(),
                evidenceSummary,
                incident.getAssignedToUserId(),
                incident.getLastActionByUserId(),
                incident.getResolvedAt(),
                incident.getResolutionNote(),
                incident.getCreatedAt(),
                incident.getUpdatedAt()
        );
    }

    private JsonNode parseEvidence(String evidenceJson) {
        if (evidenceJson == null || evidenceJson.isBlank()) {
            return objectMapper.createObjectNode();
        }

        try {
            return objectMapper.readTree(evidenceJson);
        } catch (Exception ex) {
            return objectMapper.createObjectNode();
        }
    }

    private FraudIncidentStudentResponse toStudent(UUID userId) {
        if (userId == null) {
            return null;
        }

        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .map(this::toStudent)
                .orElseGet(() -> new FraudIncidentStudentResponse(
                        userId,
                        null,
                        null,
                        null,
                        null
                ));
    }

    private FraudIncidentStudentResponse toStudent(User user) {
        return new FraudIncidentStudentResponse(
                user.getId(),
                user.getUserCode(),
                user.getFullName(),
                user.getEmail(),
                user.getAvatarUrl()
        );
    }

    private FraudIncidentEvidenceSummaryResponse toEvidenceSummary(FraudIncident incident, JsonNode evidence) {
        String ipAddress = firstText(evidence, "ipAddress", "ip_address", "sourceIp");
        String userAgent = firstText(evidence, "userAgent", "user_agent");
        String deviceId = firstText(evidence, "deviceId", "device_id");

        Double geoLat = firstDouble(evidence, "geoLat", "lat", "latitude");
        Double geoLng = firstDouble(evidence, "geoLng", "lng", "longitude");
        String location = buildLocation(geoLat, geoLng);

        Integer distanceMeter = firstInt(evidence, "distanceMeter", "distance", "maxDistanceMeter");
        Integer allowedRadiusMeter = firstInt(evidence, "allowedRadiusMeter", "allowedRadius", "radiusMeter");
        Integer otherUserCount = firstInt(evidence, "otherUserCount");
        List<UUID> otherUserIds = uuidList(evidence, "otherUserIds");

        Integer occurrenceCount = firstNonNull(
                incident.getOccurrenceCount(),
                firstInt(evidence, "occurrenceCount", "totalAttempts")
        );

        Integer threshold = firstInt(evidence, "threshold");
        Integer ruleWindowSeconds = firstInt(evidence, "ruleWindowSeconds", "windowSeconds");

        Integer distinctUserCount = firstInt(evidence, "distinctUserCount");
        Integer distinctDeviceCount = firstInt(evidence, "distinctDeviceCount");
        Integer distinctIpCount = firstInt(evidence, "distinctIpCount");

        String lastFailureCode = firstText(evidence, "lastFailureCode", "dominantFailureCode", "failureCode");
        Integer maxDistanceMeter = firstInt(evidence, "maxDistanceMeter", "distanceMeter");

        List<UUID> sampleAttemptIds = uuidList(evidence, "sampleAttemptIds", "relatedAttemptIds", "attemptIds");
        List<UUID> sampleUserIds = uuidList(evidence, "sampleUserIds", "relatedUserIds", "userIds");
        List<String> sampleIpAddresses = stringList(evidence, "sampleIpAddresses", "ipAddresses");
        List<String> sampleDeviceIds = stringList(evidence, "sampleDeviceIds", "deviceIds");

        String reason = firstText(evidence, "reason", "message", "description");
        List<String> notes = stringList(evidence, "notes");
        if (notes.isEmpty() && reason != null) {
            notes = List.of(reason);
        }

        return new FraudIncidentEvidenceSummaryResponse(
                occurrenceCount,
                threshold,
                ruleWindowSeconds,
                distinctUserCount,
                distinctDeviceCount,
                distinctIpCount,
                lastFailureCode,
                maxDistanceMeter,
                sampleAttemptIds,
                sampleUserIds,
                sampleIpAddresses,
                sampleDeviceIds,
                notes,
                ipAddress,
                userAgent,
                deviceId,
                location,
                geoLat,
                geoLng,
                distanceMeter,
                allowedRadiusMeter,
                otherUserCount,
                otherUserIds,
                reason
        );
    }

    private String titleOf(FraudIncident incident) {
        String type = typeName(incident);

        return switch (type) {
            case "SHARED_DEVICE_MULTI_ACCOUNT" -> "Trùng ID thiết bị";
            case "IP_BURST_MULTI_ATTEMPT", "IP_BURST" -> "Nhiều tài khoản cùng IP";
            case "REPEATED_OUT_OF_RANGE" -> "Điểm danh ngoài phạm vi";
            case "REPEATED_FAILED_QR_TOKEN", "REPEATED_QR_FAILURE" -> "Quét QR thất bại nhiều lần";
            case "WRONG_SESSION_QR_TOKEN" -> "QR không thuộc buổi học";
            case "EXPIRED_QR_TOKEN" -> "QR đã hết hạn";
            case "TOKEN_REPLAY" -> "QR token bị dùng lại bất thường";
            default -> "Sự cố điểm danh đáng ngờ";
        };
    }

    private String descriptionOf(FraudIncident incident, FraudIncidentEvidenceSummaryResponse evidence) {
        String type = typeName(incident);

        return switch (type) {
            case "SHARED_DEVICE_MULTI_ACCOUNT" -> {
                String deviceId = valueOrUnknown(evidence.deviceId(), "thiết bị không xác định");
                int studentCount = estimateSharedDeviceStudentCount(incident, evidence);
                yield "Thiết bị " + deviceId + " được dùng để điểm danh cho "
                        + studentCount + " sinh viên khác nhau";
            }
            case "IP_BURST_MULTI_ATTEMPT", "IP_BURST" -> {
                String ipAddress = valueOrUnknown(evidence.ipAddress(), "IP không xác định");
                Integer count = firstNonNull(evidence.distinctUserCount(), evidence.occurrenceCount());
                yield "IP " + ipAddress + " được dùng để điểm danh cho "
                        + valueOrUnknown(count, "nhiều") + " tài khoản trong thời gian ngắn";
            }
            case "REPEATED_OUT_OF_RANGE" -> {
                Integer distance = firstNonNull(evidence.distanceMeter(), evidence.maxDistanceMeter());
                Integer radius = evidence.allowedRadiusMeter();
                if (distance != null && radius != null) {
                    yield "Tọa độ quét cách lớp học " + distance + "m, vượt bán kính cho phép " + radius + "m";
                }
                if (distance != null) {
                    yield "Tọa độ quét cách lớp học " + distance + "m";
                }
                yield "Tọa độ quét nằm ngoài phạm vi cho phép của lớp học";
            }
            case "REPEATED_FAILED_QR_TOKEN", "REPEATED_QR_FAILURE" -> {
                Integer count = evidence.occurrenceCount();
                String code = valueOrUnknown(evidence.lastFailureCode(), "không xác định");
                yield "Người dùng có " + valueOrUnknown(count, "nhiều")
                        + " lần quét QR thất bại, lỗi gần nhất: " + code;
            }
            case "WRONG_SESSION_QR_TOKEN" -> "QR token được dùng không đúng phiên điểm danh hiện tại";
            case "EXPIRED_QR_TOKEN" -> "Người dùng nhiều lần sử dụng QR token đã hết hạn";
            case "TOKEN_REPLAY" -> "QR token có dấu hiệu bị dùng lại bất thường";
            default -> "Hệ thống phát hiện một dấu hiệu điểm danh bất thường cần được kiểm tra";
        };
    }

    private Integer confidenceOf(FraudIncident incident) {
        String type = typeName(incident);
        FraudIncidentSeverity severity = incident.getSeverity();

        if ("SHARED_DEVICE_MULTI_ACCOUNT".equals(type)) {
            return switch (severityName(severity)) {
                case "CRITICAL" -> 99;
                case "HIGH" -> 98;
                case "MEDIUM" -> 90;
                case "LOW" -> 75;
                default -> 80;
            };
        }

        if ("REPEATED_OUT_OF_RANGE".equals(type)) {
            return switch (severityName(severity)) {
                case "CRITICAL", "HIGH" -> 95;
                case "MEDIUM" -> 85;
                case "LOW" -> 70;
                default -> 80;
            };
        }

        if ("IP_BURST_MULTI_ATTEMPT".equals(type) || "IP_BURST".equals(type)) {
            return switch (severityName(severity)) {
                case "CRITICAL", "HIGH" -> 92;
                case "MEDIUM" -> 80;
                case "LOW" -> 65;
                default -> 70;
            };
        }

        return switch (severityName(severity)) {
            case "CRITICAL" -> 95;
            case "HIGH" -> 90;
            case "MEDIUM" -> 80;
            case "LOW" -> 65;
            default -> 70;
        };
    }

    private String displayStatusOf(FraudIncidentStatus status) {
        if (status == null) {
            return null;
        }

        return switch (status.name()) {
            case "OPEN" -> "PENDING";
            case "ACKNOWLEDGED" -> "ACKNOWLEDGED";
            case "RESOLVED" -> "RESOLVED";
            case "FALSE_POSITIVE" -> "FALSE_POSITIVE";
            default -> status.name();
        };
    }

    private int estimateSharedDeviceStudentCount(FraudIncident incident, FraudIncidentEvidenceSummaryResponse evidence) {
        Integer otherUserCount = evidence.otherUserCount();
        if (otherUserCount != null && otherUserCount >= 1) {
            return otherUserCount + 1;
        }

        Integer distinctUserCount = evidence.distinctUserCount();
        if (distinctUserCount != null && distinctUserCount >= 2) {
            return distinctUserCount;
        }

        if (evidence.otherUserIds() != null && !evidence.otherUserIds().isEmpty()) {
            return evidence.otherUserIds().size() + 1;
        }

        return Math.max(2, firstNonNull(incident.getOccurrenceCount(), 2));
    }

    private String typeName(FraudIncident incident) {
        return incident.getType() == null ? "" : incident.getType().name();
    }

    private String severityName(FraudIncidentSeverity severity) {
        return severity == null ? "" : severity.name();
    }

    private String buildLocation(Double geoLat, Double geoLng) {
        if (geoLat == null || geoLng == null) {
            return null;
        }
        return geoLat + ", " + geoLng;
    }

    private String firstText(JsonNode root, String... keys) {
        for (String key : keys) {
            JsonNode node = root.get(key);
            if (node != null && !node.isNull()) {
                String value = node.asText(null);
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
        }
        return null;
    }

    private Integer firstInt(JsonNode root, String... keys) {
        for (String key : keys) {
            JsonNode node = root.get(key);
            if (node != null && !node.isNull() && node.canConvertToInt()) {
                return node.asInt();
            }
        }
        return null;
    }

    private Double firstDouble(JsonNode root, String... keys) {
        for (String key : keys) {
            JsonNode node = root.get(key);
            if (node != null && !node.isNull() && node.isNumber()) {
                return node.asDouble();
            }
        }
        return null;
    }

    private List<UUID> uuidList(JsonNode root, String... keys) {
        List<UUID> values = new ArrayList<>();

        for (String key : keys) {
            JsonNode node = root.get(key);
            if (node == null || node.isNull()) {
                continue;
            }

            if (node.isArray()) {
                for (JsonNode item : node) {
                    addUuid(values, item.asText(null));
                }
            } else {
                String text = node.asText(null);
                if (text != null) {
                    for (String part : text.split(",")) {
                        addUuid(values, part.trim());
                    }
                }
            }

            if (!values.isEmpty()) {
                return values;
            }
        }

        return values;
    }

    private List<String> stringList(JsonNode root, String... keys) {
        List<String> values = new ArrayList<>();

        for (String key : keys) {
            JsonNode node = root.get(key);
            if (node == null || node.isNull()) {
                continue;
            }

            if (node.isArray()) {
                for (JsonNode item : node) {
                    addString(values, item.asText(null));
                }
            } else {
                addString(values, node.asText(null));
            }

            if (!values.isEmpty()) {
                return values;
            }
        }

        return values;
    }

    private void addUuid(List<UUID> values, String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }

        try {
            values.add(UUID.fromString(raw.trim()));
        } catch (IllegalArgumentException ignored) {
            // Keep response generation resilient even if older evidence_json contains non-UUID values.
        }
    }

    private void addString(List<String> values, String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }

        values.add(raw.trim());
    }

    private <T> T firstNonNull(T first, T second) {
        return first != null ? first : second;
    }

    private String valueOrUnknown(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String valueOrUnknown(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }
}
