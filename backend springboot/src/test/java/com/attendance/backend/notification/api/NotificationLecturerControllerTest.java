package com.attendance.backend.notification.api;

import com.attendance.backend.common.exception.ApiException;
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

class NotificationLecturerControllerTest {

    private NotificationQueryService notificationQueryService;
    private NotificationLecturerController controller;
    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        notificationQueryService = mock(NotificationQueryService.class);
        controller = new NotificationLecturerController(notificationQueryService);
        principal = mock(UserPrincipal.class);
    }

    @Test
    void getGroupNotifications_shouldUseJwtUserPrincipalUserId() {
        UUID userId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        PageNotificationResponse expected = new PageNotificationResponse(List.of(), 0, 20, 0, 0);

        when(principal.getUserId()).thenReturn(userId);
        when(notificationQueryService.getLecturerGroupNotifications(userId, groupId, 0, 20))
                .thenReturn(expected);

        PageNotificationResponse actual = controller.getGroupNotifications(principal, groupId, 0, 20);

        assertSame(expected, actual);
        verify(notificationQueryService).getLecturerGroupNotifications(userId, groupId, 0, 20);
    }

    @Test
    void getGroupNotifications_shouldRejectMissingPrincipal() {
        ApiException ex = assertThrows(
                ApiException.class,
                () -> controller.getGroupNotifications(null, UUID.randomUUID(), 0, 20)
        );

        assertEquals("UNAUTHORIZED", ex.getCode());
    }
}
