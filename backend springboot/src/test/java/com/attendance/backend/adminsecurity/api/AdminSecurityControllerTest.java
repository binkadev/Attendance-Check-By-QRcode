package com.attendance.backend.adminsecurity.api;

import com.attendance.backend.adminsecurity.api.dto.SecurityAbuseItemResponse;
import com.attendance.backend.adminsecurity.api.dto.SecurityDeadOutboxItemResponse;
import com.attendance.backend.adminsecurity.api.dto.SecurityOverviewResponse;
import com.attendance.backend.adminsecurity.service.AdminSecurityService;
import com.attendance.backend.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class AdminSecurityControllerTest {

    private AdminSecurityService adminSecurityService;
    private AdminSecurityController adminSecurityController;
    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        adminSecurityService = mock(AdminSecurityService.class);
        adminSecurityController = new AdminSecurityController(adminSecurityService);
        principal = mock(UserPrincipal.class);
    }

    @Test
    void getOverview_shouldDelegateToService() {
        UUID actorUserId = UUID.randomUUID();
        SecurityOverviewResponse expected = new SecurityOverviewResponse(
                24, 10, 2, 1, 20, 5, 3, 7, 4, 1, 2
        );

        when(principal.getUserId()).thenReturn(actorUserId);
        when(adminSecurityService.getOverview(actorUserId, 24)).thenReturn(expected);

        SecurityOverviewResponse actual = adminSecurityController.getOverview(principal, 24);

        assertEquals(expected, actual);
        verify(adminSecurityService).getOverview(actorUserId, 24);
    }

    @Test
    void getPasswordResetAbuse_shouldDelegateToService() {
        UUID actorUserId = UUID.randomUUID();
        List<SecurityAbuseItemResponse> expected = List.of(
                new SecurityAbuseItemResponse("203.0.113.10", 12, 5)
        );

        when(principal.getUserId()).thenReturn(actorUserId);
        when(adminSecurityService.getPasswordResetAbuse(actorUserId, 24, 20)).thenReturn(expected);

        List<SecurityAbuseItemResponse> actual =
                adminSecurityController.getPasswordResetAbuse(principal, 24, 20);

        assertEquals(expected, actual);
        verify(adminSecurityService).getPasswordResetAbuse(actorUserId, 24, 20);
    }

    @Test
    void getLoginAbuse_shouldDelegateToService() {
        UUID actorUserId = UUID.randomUUID();
        List<SecurityAbuseItemResponse> expected = List.of(
                new SecurityAbuseItemResponse("198.51.100.21", 8, 3)
        );

        when(principal.getUserId()).thenReturn(actorUserId);
        when(adminSecurityService.getLoginAbuse(actorUserId, 48, 10)).thenReturn(expected);

        List<SecurityAbuseItemResponse> actual =
                adminSecurityController.getLoginAbuse(principal, 48, 10);

        assertEquals(expected, actual);
        verify(adminSecurityService).getLoginAbuse(actorUserId, 48, 10);
    }

    @Test
    void getEmailOutbox_shouldDelegateToService() {
        UUID actorUserId = UUID.randomUUID();
        List<SecurityDeadOutboxItemResponse> expected = List.of(
                new SecurityDeadOutboxItemResponse(
                        UUID.randomUUID(),
                        "dead@demo.local",
                        "DEAD",
                        4,
                        "SMTP_TIMEOUT",
                        "Upstream timeout",
                        Instant.now(),
                        Instant.now()
                )
        );

        when(principal.getUserId()).thenReturn(actorUserId);
        when(adminSecurityService.getEmailOutbox(actorUserId, 20)).thenReturn(expected);

        List<SecurityDeadOutboxItemResponse> actual =
                adminSecurityController.getEmailOutbox(principal, 20);

        assertEquals(expected, actual);
        verify(adminSecurityService).getEmailOutbox(actorUserId, 20);
    }
}