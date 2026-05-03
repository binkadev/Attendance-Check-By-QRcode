package com.attendance.backend.fraud.dto;

import com.attendance.backend.fraud.entity.FraudIncident;
import com.attendance.backend.fraud.support.FraudEvidenceSummaryExtractor;
import org.springframework.stereotype.Component;

@Component
public class FraudIncidentMapper {

    private final FraudEvidenceSummaryExtractor extractor;

    public FraudIncidentMapper(FraudEvidenceSummaryExtractor extractor) {
        this.extractor = extractor;
    }

    public FraudIncidentResponse toResponse(FraudIncident incident) {
        return new FraudIncidentResponse(
            incident.getId(),
            incident.getGroupId(),
            incident.getSessionId(),
            incident.getUserId(),
            incident.getType(),
            incident.getSeverity(),
            incident.getStatus(),
            incident.getFirstDetectedAt(),
            incident.getLastDetectedAt(),
            incident.getOccurrenceCount(),
            extractor.extract(incident.getEvidenceJson(), incident.getOccurrenceCount()),
            incident.getAssignedToUserId(),
            incident.getLastActionByUserId(),
            incident.getResolvedAt(),
            incident.getResolutionNote(),
            incident.getCreatedAt(),
            incident.getUpdatedAt()
        );
    }

    public PageFraudIncidentResponse toPage(org.springframework.data.domain.Page<FraudIncident> page) {
        return new PageFraudIncidentResponse(
            page.getContent().stream().map(this::toResponse).toList(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages()
        );
    }
}
