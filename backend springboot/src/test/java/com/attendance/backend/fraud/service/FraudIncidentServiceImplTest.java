package com.attendance.backend.fraud.service;

import com.attendance.backend.common.exception.ApiException;
import com.attendance.backend.fraud.domain.FraudIncidentSeverity;
import com.attendance.backend.fraud.domain.FraudIncidentStatus;
import com.attendance.backend.fraud.domain.FraudIncidentType;
import com.attendance.backend.fraud.domain.PatchFraudIncidentAction;
import com.attendance.backend.fraud.entity.FraudIncident;
import com.attendance.backend.fraud.repository.FraudIncidentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class FraudIncidentServiceImplTest {

    @Mock
    private FraudIncidentRepository fraudIncidentRepository;

    @Mock
    private FraudGroupAccessService fraudGroupAccessService;

    @Mock
    private FraudIncidentNotificationPublisher notificationPublisher;

    @InjectMocks
    private FraudIncidentServiceImpl fraudIncidentService;

    @Test
    void applyAction_should_require_write_access_before_update() {
        UUID groupId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();

        FraudIncident incident = baseIncident(groupId, incidentId, FraudIncidentStatus.OPEN);

        doNothing().when(fraudGroupAccessService).requireIncidentWriteAccess(actorUserId, groupId);
        when(fraudIncidentRepository.findByIdAndGroupId(incidentId, groupId)).thenReturn(Optional.of(incident));
        when(fraudIncidentRepository.save(incident)).thenReturn(incident);

        fraudIncidentService.applyAction(
                groupId,
                incidentId,
                actorUserId,
                PatchFraudIncidentAction.ACKNOWLEDGE,
                "reviewing",
                null
        );

        org.mockito.Mockito.verify(fraudGroupAccessService).requireIncidentWriteAccess(actorUserId, groupId);
        org.mockito.Mockito.verify(fraudIncidentRepository).findByIdAndGroupId(incidentId, groupId);
        org.mockito.Mockito.verify(fraudIncidentRepository).save(incident);
    }

    @Test
    void applyAction_acknowledge_should_update_status_actor_note_and_updatedAt() {
        UUID groupId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        UUID assignedToUserId = UUID.randomUUID();

        FraudIncident incident = baseIncident(groupId, incidentId, FraudIncidentStatus.OPEN);
        Instant oldUpdatedAt = incident.getUpdatedAt();

        doNothing().when(fraudGroupAccessService).requireIncidentWriteAccess(actorUserId, groupId);
        when(fraudIncidentRepository.findByIdAndGroupId(incidentId, groupId)).thenReturn(Optional.of(incident));
        when(fraudIncidentRepository.save(incident)).thenReturn(incident);

        FraudIncident actual = fraudIncidentService.applyAction(
                groupId,
                incidentId,
                actorUserId,
                PatchFraudIncidentAction.ACKNOWLEDGE,
                "reviewing",
                assignedToUserId
        );

        assertThat(actual.getStatus()).isEqualTo(FraudIncidentStatus.ACKNOWLEDGED);
        assertThat(actual.getAssignedToUserId()).isEqualTo(assignedToUserId);
        assertThat(actual.getLastActionByUserId()).isEqualTo(actorUserId);
        assertThat(actual.getResolutionNote()).isEqualTo("reviewing");
        assertThat(actual.getResolvedAt()).isNull();
        assertThat(actual.getUpdatedAt()).isAfter(oldUpdatedAt);
    }

    @Test
    void applyAction_resolve_should_set_resolved_fields() {
        UUID groupId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();

        FraudIncident incident = baseIncident(groupId, incidentId, FraudIncidentStatus.ACKNOWLEDGED);

        doNothing().when(fraudGroupAccessService).requireIncidentWriteAccess(actorUserId, groupId);
        when(fraudIncidentRepository.findByIdAndGroupId(incidentId, groupId)).thenReturn(Optional.of(incident));
        when(fraudIncidentRepository.save(incident)).thenReturn(incident);

        FraudIncident actual = fraudIncidentService.applyAction(
                groupId,
                incidentId,
                actorUserId,
                PatchFraudIncidentAction.RESOLVE,
                "done",
                null
        );

        assertThat(actual.getStatus()).isEqualTo(FraudIncidentStatus.RESOLVED);
        assertThat(actual.getResolutionNote()).isEqualTo("done");
        assertThat(actual.getResolvedAt()).isNotNull();
        assertThat(actual.getLastActionByUserId()).isEqualTo(actorUserId);
    }

    @Test
    void applyAction_falsePositive_should_set_false_positive_status() {
        UUID groupId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();

        FraudIncident incident = baseIncident(groupId, incidentId, FraudIncidentStatus.ACKNOWLEDGED);

        doNothing().when(fraudGroupAccessService).requireIncidentWriteAccess(actorUserId, groupId);
        when(fraudIncidentRepository.findByIdAndGroupId(incidentId, groupId)).thenReturn(Optional.of(incident));
        when(fraudIncidentRepository.save(incident)).thenReturn(incident);

        FraudIncident actual = fraudIncidentService.applyAction(
                groupId,
                incidentId,
                actorUserId,
                PatchFraudIncidentAction.FALSE_POSITIVE,
                "safe",
                null
        );

        assertThat(actual.getStatus()).isEqualTo(FraudIncidentStatus.FALSE_POSITIVE);
        assertThat(actual.getResolutionNote()).isEqualTo("safe");
        assertThat(actual.getResolvedAt()).isNotNull();
    }

    @Test
    void applyAction_should_throw_not_found_when_incident_missing() {
        UUID groupId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();

        doNothing().when(fraudGroupAccessService).requireIncidentWriteAccess(actorUserId, groupId);
        when(fraudIncidentRepository.findByIdAndGroupId(incidentId, groupId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fraudIncidentService.applyAction(
                groupId,
                incidentId,
                actorUserId,
                PatchFraudIncidentAction.ACKNOWLEDGE,
                "reviewing",
                null
        )).isInstanceOf(ApiException.class);
    }

    @Test
    void applyAction_should_throw_unprocessable_for_invalid_transition_from_resolved() {
        UUID groupId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();

        FraudIncident incident = baseIncident(groupId, incidentId, FraudIncidentStatus.RESOLVED);

        doNothing().when(fraudGroupAccessService).requireIncidentWriteAccess(actorUserId, groupId);
        when(fraudIncidentRepository.findByIdAndGroupId(incidentId, groupId)).thenReturn(Optional.of(incident));

        assertThatThrownBy(() -> fraudIncidentService.applyAction(
                groupId,
                incidentId,
                actorUserId,
                PatchFraudIncidentAction.ACKNOWLEDGE,
                "again",
                null
        )).isInstanceOf(ApiException.class);
    }

    @Test
    void openOrBump_should_create_new_incident_when_dedup_key_not_found() {
        UUID groupId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        FraudIncidentService.OpenFraudIncidentCommand command = new FraudIncidentService.OpenFraudIncidentCommand(
                groupId,
                sessionId,
                userId,
                FraudIncidentType.SHARED_DEVICE_MULTI_ACCOUNT,
                FraudIncidentSeverity.HIGH,
                "dedup-key-1",
                Instant.parse("2026-03-22T00:00:00Z"),
                "{\"evidence\":true}"
        );

        when(fraudIncidentRepository.findByDedupKey("dedup-key-1")).thenReturn(Optional.empty());
        when(fraudIncidentRepository.save(any(FraudIncident.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FraudIncident actual = fraudIncidentService.openOrBump(command);

        assertThat(actual.getGroupId()).isEqualTo(groupId);
        assertThat(actual.getSessionId()).isEqualTo(sessionId);
        assertThat(actual.getUserId()).isEqualTo(userId);
        assertThat(actual.getType()).isEqualTo(FraudIncidentType.SHARED_DEVICE_MULTI_ACCOUNT);
        assertThat(actual.getSeverity()).isEqualTo(FraudIncidentSeverity.HIGH);
        assertThat(actual.getStatus()).isEqualTo(FraudIncidentStatus.OPEN);
        assertThat(actual.getDedupKey()).isEqualTo("dedup-key-1");
        assertThat(actual.getOccurrenceCount()).isEqualTo(1);
        assertThat(actual.getEvidenceJson()).isEqualTo("{\"evidence\":true}");
    }

    @Test
    void openOrBump_should_bump_existing_incident() {
        UUID groupId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();

        FraudIncident existing = baseIncident(groupId, incidentId, FraudIncidentStatus.OPEN);
        existing.setDedupKey("dedup-key-2");
        existing.setOccurrenceCount(2);
        existing.setSeverity(FraudIncidentSeverity.MEDIUM);
        existing.setLastDetectedAt(Instant.parse("2026-03-21T00:00:00Z"));
        existing.setEvidenceJson("{\"old\":true}");

        FraudIncidentService.OpenFraudIncidentCommand command = new FraudIncidentService.OpenFraudIncidentCommand(
                groupId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                FraudIncidentType.SHARED_DEVICE_MULTI_ACCOUNT,
                FraudIncidentSeverity.HIGH,
                "dedup-key-2",
                Instant.parse("2026-03-22T00:00:00Z"),
                "{\"new\":true}"
        );

        when(fraudIncidentRepository.findByDedupKey("dedup-key-2")).thenReturn(Optional.of(existing));
        when(fraudIncidentRepository.save(existing)).thenReturn(existing);

        FraudIncident actual = fraudIncidentService.openOrBump(command);

        assertThat(actual.getOccurrenceCount()).isEqualTo(3);
        assertThat(actual.getSeverity()).isEqualTo(FraudIncidentSeverity.HIGH);
        assertThat(actual.getEvidenceJson()).isEqualTo("{\"new\":true}");
        assertThat(actual.getLastDetectedAt()).isEqualTo(Instant.parse("2026-03-22T00:00:00Z"));
    }

    @Test
    void openOrBump_should_reopen_when_existing_is_resolved() {
        UUID groupId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();

        FraudIncident existing = baseIncident(groupId, incidentId, FraudIncidentStatus.RESOLVED);
        existing.setDedupKey("dedup-key-3");
        existing.setOccurrenceCount(5);
        existing.setResolvedAt(Instant.parse("2026-03-21T12:00:00Z"));
        existing.setResolutionNote("closed before");

        FraudIncidentService.OpenFraudIncidentCommand command = new FraudIncidentService.OpenFraudIncidentCommand(
                groupId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                FraudIncidentType.SHARED_DEVICE_MULTI_ACCOUNT,
                FraudIncidentSeverity.HIGH,
                "dedup-key-3",
                Instant.parse("2026-03-22T00:00:00Z"),
                "{\"reopen\":true}"
        );

        when(fraudIncidentRepository.findByDedupKey("dedup-key-3")).thenReturn(Optional.of(existing));
        when(fraudIncidentRepository.save(existing)).thenReturn(existing);

        FraudIncident actual = fraudIncidentService.openOrBump(command);

        assertThat(actual.getStatus()).isEqualTo(FraudIncidentStatus.OPEN);
        assertThat(actual.getResolvedAt()).isNull();
        assertThat(actual.getResolutionNote()).isNull();
        assertThat(actual.getOccurrenceCount()).isEqualTo(6);
    }

    private FraudIncident baseIncident(UUID groupId, UUID incidentId, FraudIncidentStatus status) {
        FraudIncident incident = new FraudIncident();
        incident.setId(incidentId);
        incident.setGroupId(groupId);
        incident.setSessionId(UUID.randomUUID());
        incident.setUserId(UUID.randomUUID());
        incident.setType(FraudIncidentType.SHARED_DEVICE_MULTI_ACCOUNT);
        incident.setSeverity(FraudIncidentSeverity.HIGH);
        incident.setStatus(status);
        incident.setDedupKey("dedup");
        incident.setFirstDetectedAt(Instant.parse("2026-03-21T10:00:00Z"));
        incident.setLastDetectedAt(Instant.parse("2026-03-21T10:10:00Z"));
        incident.setOccurrenceCount(1);
        incident.setEvidenceJson("{\"evidence\":true}");
        incident.setCreatedAt(Instant.parse("2026-03-21T10:00:00Z"));
        incident.setUpdatedAt(Instant.parse("2026-03-21T10:00:00Z"));
        return incident;
    }
}
