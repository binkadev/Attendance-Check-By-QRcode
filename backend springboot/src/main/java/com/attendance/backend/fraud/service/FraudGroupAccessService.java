package com.attendance.backend.fraud.service;

import java.util.UUID;

public interface FraudGroupAccessService {

    void requireIncidentReadAccess(UUID actorUserId, UUID groupId);

    void requireIncidentWriteAccess(UUID actorUserId, UUID groupId);
}
