package com.attendance.backend.absence.api;

import com.attendance.backend.absence.service.AbsenceRequestService;
import com.attendance.backend.common.exception.ApiException;
import com.attendance.backend.domain.enums.AbsenceRequestStatus;
import com.attendance.backend.security.UserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Validated
public class AbsenceRequestController {

    private final AbsenceRequestService absenceRequestService;

    public AbsenceRequestController(AbsenceRequestService absenceRequestService) {
        this.absenceRequestService = absenceRequestService;
    }

    @PostMapping("/groups/{groupId}/absence-requests")
    public ResponseEntity<AbsenceResponse> create(
            @PathVariable UUID groupId,
            @AuthenticationPrincipal UserPrincipal me,
            @RequestBody @Valid CreateAbsenceRequest req
    ) {
        requirePrincipal(me);

        var result = absenceRequestService.create(
                groupId,
                me.getUserId(),
                new AbsenceRequestService.CreateAbsenceCommand(
                        req.linkedSessionId(),
                        req.reason(),
                        req.evidenceUrl()
                )
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toResponse(result));
    }

    @GetMapping("/groups/{groupId}/absence-requests")
    public PageAbsenceResponse listGroupRequests(
            @PathVariable UUID groupId,
            @AuthenticationPrincipal UserPrincipal me,
            @RequestParam(required = false) AbsenceRequestStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size
    ) {
        requirePrincipal(me);

        var result = absenceRequestService.listGroupRequests(groupId, me.getUserId(), status, page, size);

        return new PageAbsenceResponse(
                result.items().stream().map(this::toResponse).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }

    @GetMapping("/me/absence-requests")
    public PageAbsenceResponse listMyRequests(
            @AuthenticationPrincipal UserPrincipal me,
            @RequestParam(required = false) AbsenceRequestStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size
    ) {
        requirePrincipal(me);

        var result = absenceRequestService.listMyRequests(me.getUserId(), status, page, size);

        return new PageAbsenceResponse(
                result.items().stream().map(this::toResponse).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }

    @GetMapping("/absence-requests/{requestId}")
    public AbsenceResponse getRequest(
            @PathVariable UUID requestId,
            @AuthenticationPrincipal UserPrincipal me
    ) {
        requirePrincipal(me);
        return toResponse(absenceRequestService.getRequest(requestId, me.getUserId()));
    }

    @PatchMapping("/absence-requests/{requestId}/review")
    public AbsenceResponse review(
            @PathVariable UUID requestId,
            @AuthenticationPrincipal UserPrincipal me,
            @RequestBody @Valid ReviewAbsenceRequest req
    ) {
        requirePrincipal(me);

        return toResponse(absenceRequestService.review(
                requestId,
                me.getUserId(),
                req.action(),
                req.reviewerNote()
        ));
    }

    @PostMapping("/absence-requests/{requestId}/cancel")
    public AbsenceResponse cancel(
            @PathVariable UUID requestId,
            @AuthenticationPrincipal UserPrincipal me
    ) {
        requirePrincipal(me);
        return toResponse(absenceRequestService.cancel(requestId, me.getUserId()));
    }

    @PostMapping("/absence-requests/{requestId}/revert")
    public AbsenceResponse revert(
            @PathVariable UUID requestId,
            @AuthenticationPrincipal UserPrincipal me,
            @RequestBody(required = false) @Valid RevertAbsenceRequest req
    ) {
        requirePrincipal(me);

        return toResponse(absenceRequestService.revert(
                requestId,
                me.getUserId(),
                req == null ? null : req.revertNote()
        ));
    }

    private void requirePrincipal(UserPrincipal me) {
        if (me == null) {
            throw ApiException.unauthorized("UNAUTHORIZED", "Missing JWT principal");
        }
    }

    private AbsenceResponse toResponse(AbsenceRequestService.AbsenceRequestView r) {
        return new AbsenceResponse(
                r.id(),
                r.groupId(),
                r.requesterUserId(),
                r.linkedSessionId(),
                r.requestedDate(),
                r.reason(),
                r.evidenceUrl(),
                r.requestStatus().name(),
                r.reviewerUserId(),
                r.reviewerNote(),
                r.reviewedAt(),
                r.cancelledAt(),
                r.revertedByUserId(),
                r.revertedAt(),
                r.revertNote(),
                r.createdAt(),
                r.updatedAt()
        );
    }

    public record CreateAbsenceRequest(
            @NotNull UUID linkedSessionId,
            @NotBlank @Size(min = 3, max = 500) String reason,
            @Size(max = 500) String evidenceUrl
    ) {
    }

    public record ReviewAbsenceRequest(
            @NotNull AbsenceRequestService.ReviewAction action,
            @Size(max = 500) String reviewerNote
    ) {
    }

    public record RevertAbsenceRequest(
            @Size(max = 500) String revertNote
    ) {
    }

    public record AbsenceResponse(
            UUID id,
            UUID groupId,
            UUID requesterUserId,
            UUID linkedSessionId,
            LocalDate requestedDate,
            String reason,
            String evidenceUrl,
            String requestStatus,
            UUID reviewerUserId,
            String reviewerNote,
            Instant reviewedAt,
            Instant cancelledAt,
            UUID revertedByUserId,
            Instant revertedAt,
            String revertNote,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record PageAbsenceResponse(
            List<AbsenceResponse> items,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
    }
}