package com.attendance.backend.notification.api;

import com.attendance.backend.common.exception.ApiException;
import com.attendance.backend.domain.enums.NotificationSeverity;
import com.attendance.backend.domain.enums.NotificationType;
import com.attendance.backend.notification.dto.NotificationResponse;
import com.attendance.backend.notification.dto.NotificationUnreadCountResponse;
import com.attendance.backend.notification.dto.PageNotificationResponse;
import com.attendance.backend.notification.service.NotificationQueryService;
import com.attendance.backend.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationMeControllerTest {

    private NotificationQueryService notificationQueryService;
    private NotificationMeController controller;
    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        notificationQueryService = mock(NotificationQueryService.class);
        controller = new NotificationMeController(notificationQueryService);
        principal = mock(UserPrincipal.class);
    }

    @Test
    void getMyNotifications_shouldUseJwtUserPrincipalUserId() {
        UUID userId = UUID.randomUUID();
        PageNotificationResponse expected = new PageNotificationResponse(List.of(), 0, 20, 0, 0);

        when(principal.getUserId()).thenReturn(userId);
        when(notificationQueryService.getMyNotifications(userId, 0, 20, false, null, null))
                .thenReturn(expected);

        PageNotificationResponse actual = controller.getMyNotifications(
                principal,
                0,
                20,
                false,
                null,
                null
        );

        assertSame(expected, actual);
        verify(notificationQueryService).getMyNotifications(userId, 0, 20, false, null, null);
    }

    @Test
    void getUnreadCount_shouldUseJwtUserPrincipalUserId() {
        UUID userId = UUID.randomUUID();
        NotificationUnreadCountResponse expected = new NotificationUnreadCountResponse(3);

        when(principal.getUserId()).thenReturn(userId);
        when(notificationQueryService.getUnreadCount(userId)).thenReturn(expected);

        NotificationUnreadCountResponse actual = controller.getUnreadCount(principal);

        assertSame(expected, actual);
        verify(notificationQueryService).getUnreadCount(userId);
    }

    @Test
    void markRead_shouldUseJwtUserPrincipalUserId() {
        UUID userId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        NotificationResponse expected = new NotificationResponse(
                notificationId,
                null,
                null,
                NotificationType.CHECKIN_SUCCESS,
                "Check-in success",
                "You checked in successfully",
                null,
                NotificationSeverity.INFO,
                null,
                null,
                false,
                null,
                null
        );

        when(principal.getUserId()).thenReturn(userId);
        when(notificationQueryService.markRead(userId, notificationId)).thenReturn(expected);

        NotificationResponse actual = controller.markRead(principal, notificationId);

        assertSame(expected, actual);
        verify(notificationQueryService).markRead(userId, notificationId);
    }

    @Test
    void getMyNotifications_shouldRejectMissingPrincipal() {
        ApiException ex = assertThrows(
                ApiException.class,
                () -> controller.getMyNotifications(null, 0, 20, false, null, null)
        );

        assertEquals("UNAUTHORIZED", ex.getCode());
    }
}
