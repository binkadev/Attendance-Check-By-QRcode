package com.attendance.backend.attendance.service;

import com.attendance.backend.attendance.dto.CheckinResultResponse;

import java.util.UUID;

public interface CheckinResultService {

    CheckinResultResponse getMyCheckinResult(UUID actorUserId, UUID sessionId);
}