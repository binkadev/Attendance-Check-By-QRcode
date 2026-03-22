package com.attendance.backend.fraud.support;

import com.attendance.backend.fraud.dto.FraudIncidentEvidenceSummaryResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class FraudEvidenceSummaryExtractor {

    private final ObjectMapper objectMapper;

    public FraudEvidenceSummaryExtractor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public FraudIncidentEvidenceSummaryResponse extract(String evidenceJson, Integer occurrenceCount) {
        if (evidenceJson == null || evidenceJson.isBlank()) {
            return new FraudIncidentEvidenceSummaryResponse(
                occurrenceCount, null, null, null, null, null, null, null,
                List.of(), List.of(), List.of(), List.of(), List.of()
            );
        }

        try {
            JsonNode root = objectMapper.readTree(evidenceJson);
            return new FraudIncidentEvidenceSummaryResponse(
                occurrenceCount,
                intOrNull(root.get("threshold")),
                intOrNull(root.get("ruleWindowSeconds")),
                intOrNull(root.get("distinctUserCount")),
                intOrNull(root.get("distinctDeviceCount")),
                intOrNull(root.get("distinctIpCount")),
                textOrNull(root.get("lastFailureCode")),
                intOrNull(root.get("maxDistanceMeter")),
                uuidList(root.get("sampleAttemptIds")),
                uuidList(root.get("sampleUserIds")),
                stringList(root.get("sampleIpAddresses")),
                stringList(root.get("sampleDeviceIds")),
                stringList(root.get("notes"))
            );
        } catch (Exception ex) {
            return new FraudIncidentEvidenceSummaryResponse(
                occurrenceCount, null, null, null, null, null, null, null,
                List.of(), List.of(), List.of(), List.of(),
                List.of("EVIDENCE_PARSE_FALLBACK")
            );
        }
    }

    private Integer intOrNull(JsonNode node) {
        return node == null || node.isNull() ? null : node.asInt();
    }

    private String textOrNull(JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }

    private List<String> stringList(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        node.forEach(item -> values.add(item.asText()));
        return values;
    }

    private List<UUID> uuidList(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<UUID> values = new ArrayList<>();
        node.forEach(item -> {
            try {
                values.add(UUID.fromString(item.asText()));
            } catch (IllegalArgumentException ignored) {
            }
        });
        return values;
    }
}
