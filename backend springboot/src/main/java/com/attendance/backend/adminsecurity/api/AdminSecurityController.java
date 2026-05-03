package com.attendance.backend.adminsecurity.api;

import com.attendance.backend.adminsecurity.dto.SecurityAbuseItemResponse;
import com.attendance.backend.adminsecurity.dto.SecurityDeadOutboxItemResponse;
import com.attendance.backend.adminsecurity.dto.SecurityOverviewResponse;
import com.attendance.backend.adminsecurity.service.AdminSecurityService;
import com.attendance.backend.security.UserPrincipal;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/security")
@Validated
@ConditionalOnMissingBean(name = "securityDashboardController")
public class AdminSecurityController {

    private final AdminSecurityService adminSecurityService;

    public AdminSecurityController(AdminSecurityService adminSecurityService) {
        this.adminSecurityService = adminSecurityService;
    }

    @GetMapping("/overview")
    public SecurityOverviewResponse getOverview(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(name = "hours", defaultValue = "24") @Min(1) @Max(168) int hours
    ) {
        UUID actorUserId = principal.getUserId();
        return adminSecurityService.getOverview(actorUserId, hours);
    }

    @GetMapping("/password-reset-abuse")
    public List<SecurityAbuseItemResponse> getPasswordResetAbuse(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(name = "hours", defaultValue = "24") @Min(1) @Max(168) int hours,
            @RequestParam(name = "limit", defaultValue = "20") @Min(1) @Max(100) int limit
    ) {
        UUID actorUserId = principal.getUserId();
        return adminSecurityService.getPasswordResetAbuse(actorUserId, hours, limit);
    }

    @GetMapping("/login-abuse")
    public List<SecurityAbuseItemResponse> getLoginAbuse(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(name = "hours", defaultValue = "24") @Min(1) @Max(168) int hours,
            @RequestParam(name = "limit", defaultValue = "20") @Min(1) @Max(100) int limit
    ) {
        UUID actorUserId = principal.getUserId();
        return adminSecurityService.getLoginAbuse(actorUserId, hours, limit);
    }

    @GetMapping("/email-outbox")
    public List<SecurityDeadOutboxItemResponse> getEmailOutbox(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(name = "limit", defaultValue = "20") @Min(1) @Max(100) int limit
    ) {
        UUID actorUserId = principal.getUserId();
        return adminSecurityService.getEmailOutbox(actorUserId, limit);
    }
}