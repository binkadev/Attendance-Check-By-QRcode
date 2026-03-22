package com.attendance.backend.fraud.api;

import com.attendance.backend.common.exception.ApiException;
import com.attendance.backend.fraud.domain.FraudIncidentSeverity;
import com.attendance.backend.fraud.domain.FraudIncidentStatus;
import com.attendance.backend.fraud.domain.FraudIncidentType;
import com.attendance.backend.fraud.dto.FraudIncidentFilter;
import com.attendance.backend.fraud.dto.FraudIncidentMapper;
import com.attendance.backend.fraud.dto.FraudIncidentResponse;
import com.attendance.backend.fraud.dto.FraudIncidentSortBy;
import com.attendance.backend.fraud.dto.PageFraudIncidentResponse;
import com.attendance.backend.fraud.dto.PatchFraudIncidentRequest;
import com.attendance.backend.fraud.service.FraudIncidentQueryService;
import com.attendance.backend.fraud.service.FraudIncidentService;
import com.attendance.backend.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/groups/{groupId}/fraud-incidents")
@Validated
public class FraudIncidentController {

    private final FraudIncidentQueryService fraudIncidentQueryService;
    private final FraudIncidentService fraudIncidentService;
    private final FraudIncidentMapper fraudIncidentMapper;

    public FraudIncidentController(
            FraudIncidentQueryService fraudIncidentQueryService,
            FraudIncidentService fraudIncidentService,
            FraudIncidentMapper fraudIncidentMapper
    ) {
        this.fraudIncidentQueryService = fraudIncidentQueryService;
        this.fraudIncidentService = fraudIncidentService;
        this.fraudIncidentMapper = fraudIncidentMapper;
    }

    @GetMapping
    public PageFraudIncidentResponse list(
            @PathVariable UUID groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Set<FraudIncidentStatus> status,
            @RequestParam(required = false) Set<FraudIncidentType> type,
            @RequestParam(required = false) Set<FraudIncidentSeverity> severity,
            @RequestParam(required = false) UUID assignedToUserId,
            @RequestParam(required = false, defaultValue = "LAST_DETECTED_AT") FraudIncidentSortBy sortBy,
            @RequestParam(required = false, defaultValue = "DESC") Sort.Direction sortDir,
            @AuthenticationPrincipal UserPrincipal me
    ) {
        UUID actorUserId = requireActorUserId(me);

        FraudIncidentFilter filter = new FraudIncidentFilter(
                status,
                type,
                severity,
                assignedToUserId,
                sortBy,
                sortDir
        );

        return fraudIncidentMapper.toPage(
                fraudIncidentQueryService.search(actorUserId, groupId, filter, page, size)
        );
    }

    @GetMapping("/{incidentId}")
    public FraudIncidentResponse detail(
            @PathVariable UUID groupId,
            @PathVariable UUID incidentId,
            @AuthenticationPrincipal UserPrincipal me
    ) {
        UUID actorUserId = requireActorUserId(me);

        return fraudIncidentMapper.toResponse(
                fraudIncidentQueryService.getDetail(actorUserId, groupId, incidentId)
        );
    }

    @PatchMapping("/{incidentId}")
    public FraudIncidentResponse patch(
            @PathVariable UUID groupId,
            @PathVariable UUID incidentId,
            @Valid @RequestBody PatchFraudIncidentRequest request,
            @AuthenticationPrincipal UserPrincipal me
    ) {
        UUID actorUserId = requireActorUserId(me);

        return fraudIncidentMapper.toResponse(
                fraudIncidentService.applyAction(
                        groupId,
                        incidentId,
                        actorUserId,
                        request.action(),
                        request.note(),
                        request.assignedToUserId()
                )
        );
    }

    private UUID requireActorUserId(UserPrincipal me) {
        if (me == null || me.getUserId() == null) {
            throw ApiException.unauthorized("UNAUTHORIZED", "Missing JWT principal");
        }
        return me.getUserId();
    }
}