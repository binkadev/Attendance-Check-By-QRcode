package com.attendance.backend.adminsecurity.service;

import com.attendance.backend.adminsecurity.dto.SecurityAbuseItemResponse;
import com.attendance.backend.adminsecurity.dto.SecurityDeadOutboxItemResponse;
import com.attendance.backend.adminsecurity.dto.SecurityOverviewResponse;
import com.attendance.backend.adminsecurity.repository.AdminSecurityQueryRepository;
import com.attendance.backend.common.exception.ApiException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class AdminSecurityServiceImpl implements AdminSecurityService {

    private final AdminSecurityQueryRepository adminSecurityQueryRepository;

    public AdminSecurityServiceImpl(AdminSecurityQueryRepository adminSecurityQueryRepository) {
        this.adminSecurityQueryRepository = adminSecurityQueryRepository;
    }

    @Override
    public SecurityOverviewResponse getOverview(UUID actorUserId, int hours) {
        requireAdmin(actorUserId);
        Instant since = Instant.now().minus(hours, ChronoUnit.HOURS);
        return adminSecurityQueryRepository.fetchOverview(hours, since);
    }

    @Override
    public List<SecurityAbuseItemResponse> getPasswordResetAbuse(UUID actorUserId, int hours, int limit) {
        requireAdmin(actorUserId);
        Instant since = Instant.now().minus(hours, ChronoUnit.HOURS);
        return adminSecurityQueryRepository.findPasswordResetAbuse(since, limit);
    }

    @Override
    public List<SecurityAbuseItemResponse> getLoginAbuse(UUID actorUserId, int hours, int limit) {
        requireAdmin(actorUserId);
        Instant since = Instant.now().minus(hours, ChronoUnit.HOURS);
        return adminSecurityQueryRepository.findLoginAbuse(since, limit);
    }

    @Override
    public List<SecurityDeadOutboxItemResponse> getEmailOutbox(UUID actorUserId, int limit) {
        requireAdmin(actorUserId);
        return adminSecurityQueryRepository.findRetryAndDeadEmailOutbox(limit);
    }

    private void requireAdmin(UUID actorUserId) {
        if (!adminSecurityQueryRepository.isActiveAdmin(actorUserId)) {
            throw ApiException.forbidden("FORBIDDEN", "Admin access required");
        }
    }
}