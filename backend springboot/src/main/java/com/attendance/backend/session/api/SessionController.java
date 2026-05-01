package com.attendance.backend.session.api;

import com.attendance.backend.common.exception.ApiException;
import com.attendance.backend.domain.enums.SessionStatus;
import com.attendance.backend.security.UserPrincipal;
import com.attendance.backend.session.dto.CreateSessionRequest;
import com.attendance.backend.session.dto.PageSessionResponse;
import com.attendance.backend.session.dto.SessionResponse;
import com.attendance.backend.session.service.SessionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Validated
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping("/groups/{groupId}/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public SessionResponse createSession(
            @AuthenticationPrincipal UserPrincipal me,
            @PathVariable UUID groupId,
            @Valid @RequestBody CreateSessionRequest req
    ) {
        if (me == null) {
            throw ApiException.unauthorized("UNAUTHORIZED", "Missing JWT principal");
        }

        return sessionService.createSession(me.getUserId(), groupId, req);
    }

    @GetMapping("/groups/{groupId}/sessions/open")
    public SessionResponse getOpenSession(
            @AuthenticationPrincipal UserPrincipal me,
            @PathVariable UUID groupId
    ) {
        if (me == null) {
            throw ApiException.unauthorized("UNAUTHORIZED", "Missing JWT principal");
        }

        return sessionService.getOpenSession(me.getUserId(), groupId);
    }

    @GetMapping("/groups/{groupId}/sessions")
    public PageSessionResponse listSessions(
            @AuthenticationPrincipal UserPrincipal me,
            @PathVariable UUID groupId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,
            @RequestParam(required = false)
            SessionStatus status,
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "page must be >= 0")
            int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "size must be >= 1")
            @Max(value = 200, message = "size must be <= 200")
            int size
    ) {
        if (me == null) {
            throw ApiException.unauthorized("UNAUTHORIZED", "Missing JWT principal");
        }

        return sessionService.listSessions(
                me.getUserId(),
                groupId,
                from,
                to,
                status,
                page,
                size
        );
    }

    @GetMapping("/groups/{groupId}/sessions/history")
    public PageSessionResponse listSessionHistory(
            @AuthenticationPrincipal UserPrincipal me,
            @PathVariable UUID groupId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,
            @RequestParam(required = false)
            SessionStatus status,
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "page must be >= 0")
            int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "size must be >= 1")
            @Max(value = 200, message = "size must be <= 200")
            int size
    ) {
        return listSessions(me, groupId, from, to, status, page, size);
    }

    @GetMapping("/sessions/{sessionId}")
    public SessionResponse getSession(
            @AuthenticationPrincipal UserPrincipal me,
            @PathVariable UUID sessionId
    ) {
        if (me == null) {
            throw ApiException.unauthorized("UNAUTHORIZED", "Missing JWT principal");
        }

        return sessionService.getSession(me.getUserId(), sessionId);
    }

    @PostMapping("/sessions/{sessionId}/close")
    public SessionResponse closeSession(
            @AuthenticationPrincipal UserPrincipal me,
            @PathVariable UUID sessionId
    ) {
        if (me == null) {
            throw ApiException.unauthorized("UNAUTHORIZED", "Missing JWT principal");
        }

        return sessionService.closeSession(me.getUserId(), sessionId);
    }

    @PostMapping("/sessions/{sessionId}/cancel")
    public SessionResponse cancelSession(
            @AuthenticationPrincipal UserPrincipal me,
            @PathVariable UUID sessionId
    ) {
        if (me == null) {
            throw ApiException.unauthorized("UNAUTHORIZED", "Missing JWT principal");
        }

        return sessionService.cancelSession(me.getUserId(), sessionId);
    }
}
