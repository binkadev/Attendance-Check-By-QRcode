package com.attendance.backend.fraud.service;

import com.attendance.backend.common.exception.ApiException;
import com.attendance.backend.fraud.dto.FraudIncidentFilter;
import com.attendance.backend.fraud.entity.FraudIncident;
import com.attendance.backend.fraud.repository.FraudIncidentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class FraudIncidentQueryService {

    private final FraudIncidentRepository fraudIncidentRepository;
    private final FraudGroupAccessService fraudGroupAccessService;

    public FraudIncidentQueryService(
        FraudIncidentRepository fraudIncidentRepository,
        FraudGroupAccessService fraudGroupAccessService
    ) {
        this.fraudIncidentRepository = fraudIncidentRepository;
        this.fraudGroupAccessService = fraudGroupAccessService;
    }

    public Page<FraudIncident> search(UUID actorUserId, UUID groupId, FraudIncidentFilter filter, int page, int size) {
        fraudGroupAccessService.requireIncidentReadAccess(actorUserId, groupId);
        return fraudIncidentRepository.search(groupId, filter, PageRequest.of(page, size));
    }

    public FraudIncident getDetail(UUID actorUserId, UUID groupId, UUID incidentId) {
        fraudGroupAccessService.requireIncidentReadAccess(actorUserId, groupId);
        return fraudIncidentRepository.findByIdAndGroupId(incidentId, groupId)
            .orElseThrow(() -> ApiException.notFound("FRAUD_INCIDENT_NOT_FOUND", "Fraud incident not found"));
    }
}
