package com.attendance.backend.session.service;

import com.attendance.backend.domain.enums.SessionStatus;
import com.attendance.backend.session.dto.CreateSessionRequest;
import com.attendance.backend.session.dto.PageSessionResponse;
import com.attendance.backend.session.dto.SessionResponse;

import java.time.LocalDate;
import java.util.UUID;

public interface SessionService {

    SessionResponse createSession(
            UUID actorUserId,
            UUID groupId,
            CreateSessionRequest req
    );

    SessionResponse getOpenSession(
            UUID actorUserId,
            UUID groupId
    );

    PageSessionResponse listSessions(
            UUID actorUserId,
            UUID groupId,
            LocalDate from,
            LocalDate to,
            SessionStatus status,
            int page,
            int size
    );

    SessionResponse getSession(
            UUID actorUserId,
            UUID sessionId
    );

    SessionResponse closeSession(
            UUID actorUserId,
            UUID sessionId
    );

    SessionResponse cancelSession(
            UUID actorUserId,
            UUID sessionId
    );
}