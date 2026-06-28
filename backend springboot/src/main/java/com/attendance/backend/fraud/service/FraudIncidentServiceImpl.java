package com.attendance.backend.fraud.service;

import com.attendance.backend.common.exception.ApiException;
import com.attendance.backend.fraud.domain.FraudIncidentStatus;
import com.attendance.backend.fraud.domain.PatchFraudIncidentAction;
import com.attendance.backend.fraud.entity.FraudIncident;
import com.attendance.backend.fraud.repository.FraudIncidentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.UUID;

@Service
public class FraudIncidentServiceImpl implements FraudIncidentService {

    private static final Logger log = LoggerFactory.getLogger(FraudIncidentServiceImpl.class);

    private final FraudIncidentRepository fraudIncidentRepository;
    private final FraudGroupAccessService fraudGroupAccessService;
    private final FraudIncidentNotificationPublisher notificationPublisher;

    public FraudIncidentServiceImpl(
            FraudIncidentRepository fraudIncidentRepository,
            FraudGroupAccessService fraudGroupAccessService,
            FraudIncidentNotificationPublisher notificationPublisher
    ) {
        this.fraudIncidentRepository = fraudIncidentRepository;
        this.fraudGroupAccessService = fraudGroupAccessService;
        this.notificationPublisher = notificationPublisher;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public FraudIncident openOrBump(OpenFraudIncidentCommand command) {
        FraudIncident incident = fraudIncidentRepository.findByDedupKey(command.dedupKey())
                .map(existing -> bumpExisting(existing, command))
                .orElseGet(() -> createOrReload(command));

        publishNotificationAfterCommit(incident);
        return incident;
    }

    @Override
    @Transactional
    public FraudIncident applyAction(
            UUID groupId,
            UUID incidentId,
            UUID actorUserId,
            PatchFraudIncidentAction action,
            String note,
            UUID assignedToUserId
    ) {
        fraudGroupAccessService.requireIncidentWriteAccess(actorUserId, groupId);

        FraudIncident incident = fraudIncidentRepository.findByIdAndGroupId(incidentId, groupId)
                .orElseThrow(() -> ApiException.notFound("FRAUD_INCIDENT_NOT_FOUND", "Fraud incident not found"));

        validateTransition(incident.getStatus(), action);

        Instant now = Instant.now();

        if (assignedToUserId != null) {
            incident.setAssignedToUserId(assignedToUserId);
        }

        incident.setLastActionByUserId(actorUserId);
        incident.setUpdatedAt(now);

        switch (action) {
            case ACKNOWLEDGE -> {
                incident.setStatus(FraudIncidentStatus.ACKNOWLEDGED);
                incident.setResolvedAt(null);
                incident.setResolutionNote(note);
            }
            case RESOLVE -> {
                incident.setStatus(FraudIncidentStatus.RESOLVED);
                incident.setResolvedAt(now);
                incident.setResolutionNote(note);
            }
            case FALSE_POSITIVE -> {
                incident.setStatus(FraudIncidentStatus.FALSE_POSITIVE);
                incident.setResolvedAt(now);
                incident.setResolutionNote(note);
            }
        }

        return fraudIncidentRepository.save(incident);
    }

    private FraudIncident createOrReload(OpenFraudIncidentCommand command) {
        FraudIncident incident = new FraudIncident();
        incident.setId(UUID.randomUUID());
        incident.setGroupId(command.groupId());
        incident.setSessionId(command.sessionId());
        incident.setUserId(command.userId());
        incident.setType(command.type());
        incident.setSeverity(command.severity());
        incident.setStatus(FraudIncidentStatus.OPEN);
        incident.setDedupKey(command.dedupKey());
        incident.setFirstDetectedAt(command.detectedAt());
        incident.setLastDetectedAt(command.detectedAt());
        incident.setOccurrenceCount(1);
        incident.setEvidenceJson(command.evidenceJson());
        incident.setCreatedAt(command.detectedAt());
        incident.setUpdatedAt(command.detectedAt());

        try {
            return fraudIncidentRepository.save(incident);
        } catch (DataIntegrityViolationException ex) {
            FraudIncident existing = fraudIncidentRepository.findByDedupKey(command.dedupKey())
                    .orElseThrow(() -> ex);
            return bumpExisting(existing, command);
        }
    }

    private FraudIncident bumpExisting(FraudIncident existing, OpenFraudIncidentCommand command) {
        existing.setLastDetectedAt(command.detectedAt());
        existing.setOccurrenceCount(existing.getOccurrenceCount() + 1);
        existing.setEvidenceJson(command.evidenceJson());
        existing.setUpdatedAt(command.detectedAt());

        if (existing.getStatus() == FraudIncidentStatus.RESOLVED
                || existing.getStatus() == FraudIncidentStatus.FALSE_POSITIVE) {
            existing.setStatus(FraudIncidentStatus.OPEN);
            existing.setResolvedAt(null);
            existing.setResolutionNote(null);
        }

        if (command.severity().ordinal() > existing.getSeverity().ordinal()) {
            existing.setSeverity(command.severity());
        }

        return fraudIncidentRepository.save(existing);
    }

    private void publishNotificationAfterCommit(FraudIncident incident) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishNotificationNow(incident);
                }
            });
            return;
        }

        publishNotificationNow(incident);
    }

    private void publishNotificationNow(FraudIncident incident) {
        try {
            notificationPublisher.onOpenedOrBumped(incident);
        } catch (RuntimeException ex) {
            log.warn(
                    "Failed to publish fraud incident notification for incidentId={}, groupId={}, type={}",
                    incident == null ? null : incident.getId(),
                    incident == null ? null : incident.getGroupId(),
                    incident == null ? null : incident.getType(),
                    ex
            );
        }
    }

    private void validateTransition(FraudIncidentStatus currentStatus, PatchFraudIncidentAction action) {
        boolean valid = switch (currentStatus) {
            case OPEN -> true;
            case ACKNOWLEDGED -> action == PatchFraudIncidentAction.RESOLVE
                    || action == PatchFraudIncidentAction.FALSE_POSITIVE;
            case RESOLVED, FALSE_POSITIVE -> false;
        };

        if (!valid) {
            throw ApiException.unprocessable(
                    "FRAUD_INVALID_TRANSITION",
                    "Invalid fraud incident transition"
            );
        }
    }
}
