package com.attendance.backend.adminsecurity.service;

import com.attendance.backend.adminsecurity.dto.SecurityAbuseItemResponse;
import com.attendance.backend.adminsecurity.dto.SecurityDeadOutboxItemResponse;
import com.attendance.backend.adminsecurity.dto.SecurityOverviewResponse;
import com.attendance.backend.adminsecurity.repository.AdminSecurityQueryRepository;
import com.attendance.backend.common.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminSecurityServiceImplTest {

    private AdminSecurityQueryRepository adminSecurityQueryRepository;
    private AdminSecurityServiceImpl adminSecurityService;

    @BeforeEach
    void setUp() {
        adminSecurityQueryRepository = mock(AdminSecurityQueryRepository.class);
        adminSecurityService = new AdminSecurityServiceImpl(adminSecurityQueryRepository);
    }

    @Test
    void getOverview_shouldThrowForbidden_whenActorIsNotAdmin() {
        UUID actorUserId = UUID.randomUUID();
        when(adminSecurityQueryRepository.isActiveAdmin(actorUserId)).thenReturn(false);

        ApiException ex = assertThrows(
                ApiException.class,
                () -> adminSecurityService.getOverview(actorUserId, 24)
        );

        assertEquals(403, ex.getStatus().value());
        verify(adminSecurityQueryRepository).isActiveAdmin(actorUserId);
        verify(adminSecurityQueryRepository, never()).fetchOverview(anyInt(), any());
    }

    @Test
    void getOverview_shouldReturnOverview_whenActorIsAdmin() {
        UUID actorUserId = UUID.randomUUID();
        SecurityOverviewResponse expected = new SecurityOverviewResponse(
                24, 10, 2, 1, 20, 5, 3, 7, 4, 1, 2
        );

        when(adminSecurityQueryRepository.isActiveAdmin(actorUserId)).thenReturn(true);
        when(adminSecurityQueryRepository.fetchOverview(eq(24), ArgumentMatchers.any(Instant.class)))
                .thenReturn(expected);

        SecurityOverviewResponse actual = adminSecurityService.getOverview(actorUserId, 24);

        assertEquals(expected, actual);
        verify(adminSecurityQueryRepository).isActiveAdmin(actorUserId);
        verify(adminSecurityQueryRepository).fetchOverview(eq(24), any(Instant.class));
    }

    @Test
    void getPasswordResetAbuse_shouldReturnItems_whenActorIsAdmin() {
        UUID actorUserId = UUID.randomUUID();
        List<SecurityAbuseItemResponse> expected = List.of(
                new SecurityAbuseItemResponse("203.0.113.10", 12, 5)
        );

        when(adminSecurityQueryRepository.isActiveAdmin(actorUserId)).thenReturn(true);
        when(adminSecurityQueryRepository.findPasswordResetAbuse(any(Instant.class), eq(20)))
                .thenReturn(expected);

        List<SecurityAbuseItemResponse> actual =
                adminSecurityService.getPasswordResetAbuse(actorUserId, 24, 20);

        assertEquals(expected, actual);
        verify(adminSecurityQueryRepository).findPasswordResetAbuse(any(Instant.class), eq(20));
    }

    @Test
    void getLoginAbuse_shouldReturnItems_whenActorIsAdmin() {
        UUID actorUserId = UUID.randomUUID();
        List<SecurityAbuseItemResponse> expected = List.of(
                new SecurityAbuseItemResponse("198.51.100.21", 8, 3)
        );

        when(adminSecurityQueryRepository.isActiveAdmin(actorUserId)).thenReturn(true);
        when(adminSecurityQueryRepository.findLoginAbuse(any(Instant.class), eq(10)))
                .thenReturn(expected);

        List<SecurityAbuseItemResponse> actual =
                adminSecurityService.getLoginAbuse(actorUserId, 48, 10);

        assertEquals(expected, actual);
        verify(adminSecurityQueryRepository).findLoginAbuse(any(Instant.class), eq(10));
    }

    @Test
    void getEmailOutbox_shouldReturnItems_whenActorIsAdmin() {
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

        when(adminSecurityQueryRepository.isActiveAdmin(actorUserId)).thenReturn(true);
        when(adminSecurityQueryRepository.findRetryAndDeadEmailOutbox(20)).thenReturn(expected);

        List<SecurityDeadOutboxItemResponse> actual =
                adminSecurityService.getEmailOutbox(actorUserId, 20);

        assertEquals(expected, actual);
        verify(adminSecurityQueryRepository).findRetryAndDeadEmailOutbox(20);
    }
}