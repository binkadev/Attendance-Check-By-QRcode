package com.attendance.backend.adminsecurity.service;

import com.attendance.backend.adminsecurity.api.dto.SecurityAbuseItemResponse;
import com.attendance.backend.adminsecurity.api.dto.SecurityDeadOutboxItemResponse;
import com.attendance.backend.adminsecurity.api.dto.SecurityOverviewResponse;

import java.util.List;
import java.util.UUID;

public interface AdminSecurityService {

    SecurityOverviewResponse getOverview(UUID actorUserId, int hours);

    List<SecurityAbuseItemResponse> getPasswordResetAbuse(UUID actorUserId, int hours, int limit);

    List<SecurityAbuseItemResponse> getLoginAbuse(UUID actorUserId, int hours, int limit);

    List<SecurityDeadOutboxItemResponse> getEmailOutbox(UUID actorUserId, int limit);
}