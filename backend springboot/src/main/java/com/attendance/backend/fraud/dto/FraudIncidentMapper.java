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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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

    private FraudIncidentEvidenceSummaryResponse toEvidenceSummary(FraudIncident incident, JsonNode evidence) {
        String type = typeName(incident);
        Integer occurrenceCount = firstNonNull(
                incident.getOccurrenceCount(),
                firstInt(evidence, "occurrenceCount", "totalAttempts")
        );

        String reason = reasonOf(incident, evidence);

        return switch (type) {
            case "SHARED_DEVICE_MULTI_ACCOUNT" -> sharedDeviceEvidence(incident, evidence, occurrenceCount, reason);
            case "REPEATED_OUT_OF_RANGE" -> outOfRangeEvidence(evidence, occurrenceCount, reason);
            case "IP_BURST_MULTI_ATTEMPT", "IP_BURST" -> ipBurstEvidence(incident, evidence, occurrenceCount, reason);
            case "REPEATED_FAILED_QR_TOKEN", "REPEATED_QR_FAILURE", "WRONG_SESSION_QR_TOKEN", "EXPIRED_QR_TOKEN", "TOKEN_REPLAY" ->
                    qrTokenEvidence(incident, evidence, occurrenceCount, reason);
            default -> defaultEvidence(incident, evidence, occurrenceCount, reason);
        };
    }

    private FraudIncidentEvidenceSummaryResponse sharedDeviceEvidence(
            FraudIncident incident,
            JsonNode evidence,
            Integer occurrenceCount,
            String reason
    ) {
        String deviceId = firstText(evidence, "deviceId", "device_id", "sampleDeviceIds", "deviceIds");
        List<UUID> involvedUserIds = involvedUserIds(incident.getUserId(), evidence);
        Integer distinctUserCount = firstNonNull(
                firstInt(evidence, "distinctUserCount"),
                plusOne(firstInt(evidence, "otherUserCount"))
        );

        if (distinctUserCount == null && involvedUserIds != null && !involvedUserIds.isEmpty()) {
            distinctUserCount = involvedUserIds.size();
        }

        if (distinctUserCount == null) {
            distinctUserCount = Math.max(2, firstNonNull(occurrenceCount, 2));
        }

        return new FraudIncidentEvidenceSummaryResponse(
                occurrenceCount,
                reason,
                deviceId,
                null,
                null,
                distinctUserCount,
                nullIfEmpty(involvedUserIds),
                toStudents(involvedUserIds),
                null,
                null,
                null,
                null
        );
    }

    private FraudIncidentEvidenceSummaryResponse outOfRangeEvidence(
            JsonNode evidence,
            Integer occurrenceCount,
            String reason
    ) {
        Integer distanceMeter = firstInt(evidence, "distanceMeter", "distance", "maxDistanceMeter");
        Integer allowedRadiusMeter = firstInt(evidence, "allowedRadiusMeter", "allowedRadius", "radiusMeter");
        FraudIncidentLocationResponse location = locationOf(evidence);

        return new FraudIncidentEvidenceSummaryResponse(
                occurrenceCount,
                reason,
                null,
                null,
                null,
                null,
                null,
                null,
                distanceMeter,
                allowedRadiusMeter,
                location,
                null
        );
    }

    private FraudIncidentEvidenceSummaryResponse ipBurstEvidence(
            FraudIncident incident,
            JsonNode evidence,
            Integer occurrenceCount,
            String reason
    ) {
        String ipAddress = firstText(evidence, "ipAddress", "ip_address", "sourceIp", "sampleIpAddresses", "ipAddresses");
        String userAgent = firstText(evidence, "userAgent", "user_agent");
        List<UUID> involvedUserIds = involvedUserIds(incident.getUserId(), evidence);
        Integer distinctUserCount = firstNonNull(firstInt(evidence, "distinctUserCount"), sizeOf(involvedUserIds));

        return new FraudIncidentEvidenceSummaryResponse(
                occurrenceCount,
                reason,
                null,
                ipAddress,
                userAgent,
                distinctUserCount,
                nullIfEmpty(involvedUserIds),
                toStudents(involvedUserIds),
                null,
                null,
                null,
                null
        );
    }

    private FraudIncidentEvidenceSummaryResponse qrTokenEvidence(
            FraudIncident incident,
            JsonNode evidence,
            Integer occurrenceCount,
            String reason
    ) {
        String ipAddress = firstText(evidence, "ipAddress", "ip_address", "sourceIp", "sampleIpAddresses", "ipAddresses");
        String userAgent = firstText(evidence, "userAgent", "user_agent");
        String deviceId = firstText(evidence, "deviceId", "device_id", "sampleDeviceIds", "deviceIds");
        String lastFailureCode = firstText(evidence, "lastFailureCode", "dominantFailureCode", "failureCode");
        List<UUID> involvedUserIds = involvedUserIds(incident.getUserId(), evidence);

        return new FraudIncidentEvidenceSummaryResponse(
                occurrenceCount,
                reason,
                deviceId,
                ipAddress,
                userAgent,
                null,
                nullIfEmpty(involvedUserIds),
                toStudents(involvedUserIds),
                null,
                null,
                null,
                lastFailureCode
        );
    }

    private FraudIncidentEvidenceSummaryResponse defaultEvidence(
            FraudIncident incident,
            JsonNode evidence,
            Integer occurrenceCount,
            String reason
    ) {
        String deviceId = firstText(evidence, "deviceId", "device_id", "sampleDeviceIds", "deviceIds");
        String ipAddress = firstText(evidence, "ipAddress", "ip_address", "sourceIp", "sampleIpAddresses", "ipAddresses");
        String userAgent = firstText(evidence, "userAgent", "user_agent");
        String lastFailureCode = firstText(evidence, "lastFailureCode", "dominantFailureCode", "failureCode");
        List<UUID> involvedUserIds = involvedUserIds(incident.getUserId(), evidence);

        return new FraudIncidentEvidenceSummaryResponse(
                occurrenceCount,
                reason,
                deviceId,
                ipAddress,
                userAgent,
                firstInt(evidence, "distinctUserCount"),
                nullIfEmpty(involvedUserIds),
                toStudents(involvedUserIds),
                firstInt(evidence, "distanceMeter", "distance", "maxDistanceMeter"),
                firstInt(evidence, "allowedRadiusMeter", "allowedRadius", "radiusMeter"),
                locationOf(evidence),
                lastFailureCode
        );
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

    private List<FraudIncidentStudentResponse> toStudents(List<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return null;
        }

        List<FraudIncidentStudentResponse> students = userIds.stream()
                .map(this::toStudent)
                .filter(student -> student != null && student.id() != null)
                .toList();

        return students.isEmpty() ? null : students;
    }

    private List<UUID> involvedUserIds(UUID primaryUserId, JsonNode evidence) {
        Set<UUID> values = new LinkedHashSet<>();

        if (primaryUserId != null) {
            values.add(primaryUserId);
        }

        addUuidValues(values, evidence, "involvedUserIds", "involvedUsers", "otherUserIds", "sampleUserIds", "relatedUserIds", "userIds", "currentUserId");

        if (values.isEmpty()) {
            return null;
        }

        return new ArrayList<>(values);
    }

    private void addUuidValues(Set<UUID> values, JsonNode root, String... keys) {
        for (String key : keys) {
            JsonNode node = root.get(key);
            if (node == null || node.isNull()) {
                continue;
            }

            if (node.isArray()) {
                for (JsonNode item : node) {
                    if (item.isObject()) {
                        addUuid(values, firstText(item, "id", "userId"));
                    } else {
                        addUuid(values, item.asText(null));
                    }
                }
            } else if (node.isObject()) {
                addUuid(values, firstText(node, "id", "userId"));
            } else {
                String text = node.asText(null);
                if (text != null) {
                    for (String part : text.split(",")) {
                        addUuid(values, part.trim());
                    }
                }
            }
        }
    }

    private void addUuid(Set<UUID> values, String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }

        try {
            values.add(UUID.fromString(raw.trim()));
        } catch (IllegalArgumentException ignored) {
            // Keep response generation resilient even if older evidence_json contains non-UUID values.
        }
    }

    private FraudIncidentLocationResponse locationOf(JsonNode evidence) {
        BigDecimal lat = firstDecimal(evidence, "geoLat", "lat", "latitude");
        BigDecimal lng = firstDecimal(evidence, "geoLng", "lng", "longitude");

        JsonNode location = evidence.get("location");
        if (location != null && !location.isNull()) {
            if (location.isObject()) {
                lat = firstNonNull(lat, firstDecimal(location, "lat", "latitude", "geoLat"));
                lng = firstNonNull(lng, firstDecimal(location, "lng", "longitude", "geoLng"));
            } else if (location.isTextual()) {
                BigDecimal[] parsed = parseLocationText(location.asText());
                if (parsed != null) {
                    lat = firstNonNull(lat, parsed[0]);
                    lng = firstNonNull(lng, parsed[1]);
                }
            }
        }

        if (lat == null || lng == null) {
            return null;
        }

        return new FraudIncidentLocationResponse(lat, lng);
    }

    private BigDecimal[] parseLocationText(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        String[] parts = text.split(",");
        if (parts.length != 2) {
            return null;
        }

        try {
            return new BigDecimal[]{
                    new BigDecimal(parts[0].trim()),
                    new BigDecimal(parts[1].trim())
            };
        } catch (NumberFormatException ex) {
            return null;
        }
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
                int studentCount = estimateSharedDeviceStudentCount(evidence);
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
                Integer distance = evidence.distanceMeter();
                Integer radius = evidence.allowedRadiusMeter();
                if (distance != null && radius != null) {
                    yield "Vị trí điểm danh cách lớp học " + distance + "m, vượt bán kính cho phép " + radius + "m";
                }
                if (distance != null) {
                    yield "Vị trí điểm danh cách lớp học " + distance + "m";
                }
                yield "Vị trí điểm danh nằm ngoài phạm vi cho phép của lớp học";
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

    private String reasonOf(FraudIncident incident, JsonNode evidence) {
        String explicitReason = firstText(evidence, "reason", "message", "description");
        if (explicitReason != null) {
            return explicitReason;
        }

        String type = typeName(incident);

        return switch (type) {
            case "SHARED_DEVICE_MULTI_ACCOUNT" -> "Phát hiện một thiết bị điểm danh cho nhiều tài khoản";
            case "IP_BURST_MULTI_ATTEMPT", "IP_BURST" -> "Nhiều tài khoản điểm danh từ cùng một địa chỉ IP trong thời gian ngắn";
            case "REPEATED_OUT_OF_RANGE" -> "Vị trí điểm danh nằm ngoài bán kính cho phép nhiều lần";
            case "REPEATED_FAILED_QR_TOKEN", "REPEATED_QR_FAILURE" -> "Người dùng quét QR thất bại nhiều lần";
            case "WRONG_SESSION_QR_TOKEN" -> "QR token không thuộc phiên điểm danh hiện tại";
            case "EXPIRED_QR_TOKEN" -> "QR token đã hết hạn";
            case "TOKEN_REPLAY" -> "QR token bị dùng lại bất thường";
            default -> "Hệ thống phát hiện dấu hiệu điểm danh bất thường";
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

    private int estimateSharedDeviceStudentCount(FraudIncidentEvidenceSummaryResponse evidence) {
        Integer distinctUserCount = evidence.distinctUserCount();
        if (distinctUserCount != null && distinctUserCount >= 2) {
            return distinctUserCount;
        }

        if (evidence.involvedUserIds() != null && !evidence.involvedUserIds().isEmpty()) {
            return Math.max(2, evidence.involvedUserIds().size());
        }

        return Math.max(2, firstNonNull(evidence.occurrenceCount(), 2));
    }

    private String firstText(JsonNode root, String... keys) {
        for (String key : keys) {
            JsonNode node = root.get(key);
            String value = textValue(node);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String textValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }

        if (node.isArray()) {
            for (JsonNode item : node) {
                String value = textValue(item);
                if (value != null) {
                    return value;
                }
            }
            return null;
        }

        if (node.isObject()) {
            return firstText(node, "value", "id", "deviceId", "ipAddress", "reason", "name");
        }

        String value = node.asText(null);
        return value == null || value.isBlank() ? null : value;
    }

    private Integer firstInt(JsonNode root, String... keys) {
        for (String key : keys) {
            JsonNode node = root.get(key);
            if (node == null || node.isNull()) {
                continue;
            }

            if (node.isInt() || node.canConvertToInt()) {
                return node.asInt();
            }

            if (node.isTextual()) {
                try {
                    return Integer.parseInt(node.asText().trim());
                } catch (NumberFormatException ignored) {
                    // Continue trying other keys.
                }
            }
        }
        return null;
    }

    private BigDecimal firstDecimal(JsonNode root, String... keys) {
        for (String key : keys) {
            JsonNode node = root.get(key);
            if (node == null || node.isNull()) {
                continue;
            }

            if (node.isNumber()) {
                return node.decimalValue();
            }

            if (node.isTextual()) {
                try {
                    return new BigDecimal(node.asText().trim());
                } catch (NumberFormatException ignored) {
                    // Continue trying other keys.
                }
            }
        }
        return null;
    }

    private Integer plusOne(Integer value) {
        return value == null ? null : value + 1;
    }

    private Integer sizeOf(List<?> values) {
        return values == null || values.isEmpty() ? null : values.size();
    }

    private String typeName(FraudIncident incident) {
        return incident.getType() == null ? "" : incident.getType().name();
    }

    private String severityName(FraudIncidentSeverity severity) {
        return severity == null ? "" : severity.name();
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

    private <T> List<T> nullIfEmpty(List<T> values) {
        return values == null || values.isEmpty() ? null : values;
    }
}
