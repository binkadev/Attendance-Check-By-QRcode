package com.attendance.backend.fraud.api;

import com.attendance.backend.common.exception.ApiException;
import com.attendance.backend.fraud.domain.FraudIncidentSeverity;
import com.attendance.backend.fraud.domain.FraudIncidentStatus;
import com.attendance.backend.fraud.domain.FraudIncidentType;
import com.attendance.backend.fraud.domain.PatchFraudIncidentAction;
import com.attendance.backend.fraud.dto.FraudIncidentEvidenceSummaryResponse;
import com.attendance.backend.fraud.dto.FraudIncidentFilter;
import com.attendance.backend.fraud.dto.FraudIncidentMapper;
import com.attendance.backend.fraud.dto.FraudIncidentResponse;
import com.attendance.backend.fraud.dto.FraudIncidentSortBy;
import com.attendance.backend.fraud.dto.PageFraudIncidentResponse;
import com.attendance.backend.fraud.dto.PatchFraudIncidentRequest;
import com.attendance.backend.fraud.entity.FraudIncident;
import com.attendance.backend.fraud.service.FraudIncidentQueryService;
import com.attendance.backend.fraud.service.FraudIncidentService;
import com.attendance.backend.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FraudIncidentControllerIT {

    @Mock
    private FraudIncidentQueryService fraudIncidentQueryService;

    @Mock
    private FraudIncidentService fraudIncidentService;

    @Mock
    private FraudIncidentMapper fraudIncidentMapper;

    @InjectMocks
    private FraudIncidentController fraudIncidentController;

    @Test
    void list_should_return_page_and_map_filter_from_query_params() {
        UUID actorUserId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();
        UUID assignedToUserId = UUID.randomUUID();

        UserPrincipal me = principal(actorUserId);

        FraudIncident incident = new FraudIncident();
        incident.setId(incidentId);

        Page<FraudIncident> pageResult = new PageImpl<>(List.of(incident));

        PageFraudIncidentResponse expected = new PageFraudIncidentResponse(
                List.of(sampleResponse(
                        incidentId,
                        groupId,
                        assignedToUserId,
                        actorUserId,
                        FraudIncidentStatus.OPEN,
                        null,
                        "device-shared"
                )),
                0,
                20,
                1L,
                1
        );

        when(fraudIncidentQueryService.search(
                eq(actorUserId),
                eq(groupId),
                any(FraudIncidentFilter.class),
                eq(0),
                eq(20)
        )).thenReturn(pageResult);

        when(fraudIncidentMapper.toPage(pageResult)).thenReturn(expected);

        PageFraudIncidentResponse actual = fraudIncidentController.list(
                groupId,
                0,
                20,
                Set.of(FraudIncidentStatus.OPEN),
                Set.of(FraudIncidentType.SHARED_DEVICE_MULTI_ACCOUNT),
                Set.of(FraudIncidentSeverity.HIGH),
                assignedToUserId,
                FraudIncidentSortBy.LAST_DETECTED_AT,
                Sort.Direction.DESC,
                me
        );

        assertThat(actual).isSameAs(expected);

        ArgumentCaptor<FraudIncidentFilter> filterCaptor = ArgumentCaptor.forClass(FraudIncidentFilter.class);
        verify(fraudIncidentQueryService).search(eq(actorUserId), eq(groupId), filterCaptor.capture(), eq(0), eq(20));

        FraudIncidentFilter filter = filterCaptor.getValue();
        assertThat(filter.statuses()).containsExactly(FraudIncidentStatus.OPEN);
        assertThat(filter.types()).containsExactly(FraudIncidentType.SHARED_DEVICE_MULTI_ACCOUNT);
        assertThat(filter.severities()).containsExactly(FraudIncidentSeverity.HIGH);
        assertThat(filter.assignedToUserId()).isEqualTo(assignedToUserId);
        assertThat(filter.sortBy()).isEqualTo(FraudIncidentSortBy.LAST_DETECTED_AT);
        assertThat(filter.sortDir()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void detail_should_return_incident_detail() {
        UUID actorUserId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();
        UUID assignedToUserId = UUID.randomUUID();

        UserPrincipal me = principal(actorUserId);

        FraudIncident incident = new FraudIncident();
        incident.setId(incidentId);

        FraudIncidentResponse expected = sampleResponse(
                incidentId,
                groupId,
                assignedToUserId,
                actorUserId,
                FraudIncidentStatus.ACKNOWLEDGED,
                null,
                "detail-note"
        );

        when(fraudIncidentQueryService.getDetail(actorUserId, groupId, incidentId)).thenReturn(incident);
        when(fraudIncidentMapper.toResponse(incident)).thenReturn(expected);

        FraudIncidentResponse actual = fraudIncidentController.detail(groupId, incidentId, me);

        assertThat(actual).isSameAs(expected);

        verify(fraudIncidentQueryService).getDetail(actorUserId, groupId, incidentId);
        verify(fraudIncidentMapper).toResponse(incident);
    }

    @Test
    void patch_should_delegate_to_service_and_return_updated_incident() {
        UUID actorUserId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();
        UUID assignedToUserId = UUID.randomUUID();

        UserPrincipal me = principal(actorUserId);

        FraudIncident incident = new FraudIncident();
        incident.setId(incidentId);

        PatchFraudIncidentRequest request = new PatchFraudIncidentRequest(
                PatchFraudIncidentAction.ACKNOWLEDGE,
                "reviewing",
                assignedToUserId
        );

        FraudIncidentResponse expected = sampleResponse(
                incidentId,
                groupId,
                assignedToUserId,
                actorUserId,
                FraudIncidentStatus.ACKNOWLEDGED,
                null,
                "acknowledged"
        );

        when(fraudIncidentService.applyAction(
                groupId,
                incidentId,
                actorUserId,
                PatchFraudIncidentAction.ACKNOWLEDGE,
                "reviewing",
                assignedToUserId
        )).thenReturn(incident);

        when(fraudIncidentMapper.toResponse(incident)).thenReturn(expected);

        FraudIncidentResponse actual = fraudIncidentController.patch(groupId, incidentId, request, me);

        assertThat(actual).isSameAs(expected);

        verify(fraudIncidentService).applyAction(
                groupId,
                incidentId,
                actorUserId,
                PatchFraudIncidentAction.ACKNOWLEDGE,
                "reviewing",
                assignedToUserId
        );
        verify(fraudIncidentMapper).toResponse(incident);
    }

    @Test
    void patch_should_throw_forbidden_when_service_denies_access() {
        UUID actorUserId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();

        UserPrincipal me = principal(actorUserId);

        PatchFraudIncidentRequest request = new PatchFraudIncidentRequest(
                PatchFraudIncidentAction.RESOLVE,
                "done",
                null
        );

        when(fraudIncidentService.applyAction(
                groupId,
                incidentId,
                actorUserId,
                PatchFraudIncidentAction.RESOLVE,
                "done",
                null
        )).thenThrow(ApiException.forbidden(
                "GROUP_ACCESS_DENIED",
                "No permission to update fraud incidents for this group"
        ));

        assertThatThrownBy(() -> fraudIncidentController.patch(groupId, incidentId, request, me))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void list_should_throw_unauthorized_when_missing_principal() {
        UUID groupId = UUID.randomUUID();

        assertThatThrownBy(() -> fraudIncidentController.list(
                groupId,
                0,
                20,
                null,
                null,
                null,
                null,
                FraudIncidentSortBy.LAST_DETECTED_AT,
                Sort.Direction.DESC,
                null
        )).isInstanceOf(ApiException.class);
    }

    @Test
    void patch_should_throw_unauthorized_when_missing_principal() {
        UUID groupId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();

        PatchFraudIncidentRequest request = new PatchFraudIncidentRequest(
                PatchFraudIncidentAction.ACKNOWLEDGE,
                "review",
                null
        );

        assertThatThrownBy(() -> fraudIncidentController.patch(groupId, incidentId, request, null))
                .isInstanceOf(ApiException.class);
    }

    private UserPrincipal principal(UUID userId) {
        UserPrincipal principal = mock(UserPrincipal.class);
        when(principal.getUserId()).thenReturn(userId);
        return principal;
    }

    private FraudIncidentResponse sampleResponse(
            UUID incidentId,
            UUID groupId,
            UUID assignedToUserId,
            UUID actorUserId,
            FraudIncidentStatus status,
            Instant resolvedAt,
            String note
    ) {
        Instant now = Instant.parse("2026-03-21T16:00:00Z");

        return new FraudIncidentResponse(
                incidentId,
                groupId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                FraudIncidentType.SHARED_DEVICE_MULTI_ACCOUNT,
                FraudIncidentSeverity.HIGH,
                status,
                now.minusSeconds(600),
                now.minusSeconds(60),
                3,
                new FraudIncidentEvidenceSummaryResponse(
                        3,
                        2,
                        300,
                        2,
                        1,
                        1,
                        "QR_EXPIRED",
                        120,
                        List.of(UUID.randomUUID()),
                        List.of(UUID.randomUUID()),
                        List.of("10.0.0.1"),
                        List.of("device-01"),
                        List.of(note)
                ),
                assignedToUserId,
                actorUserId,
                resolvedAt,
                resolvedAt == null ? null : "resolved",
                now.minusSeconds(600),
                now
        );
    }
}