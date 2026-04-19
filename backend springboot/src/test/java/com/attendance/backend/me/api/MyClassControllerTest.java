package com.attendance.backend.me.api;

import com.attendance.backend.me.model.MyClassQueryCriteria;
import com.attendance.backend.me.service.MyClassQueryService;
import com.attendance.backend.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MyClassControllerTest {

    @Mock
    private MyClassQueryService myClassQueryService;

    @InjectMocks
    private MyClassController myClassController;

    @Test
    void teaching_wrapper_maps_scope_to_teaching() {
        UUID actorUserId = UUID.randomUUID();

        UserPrincipal principal = mock(UserPrincipal.class);
        when(principal.getUserId()).thenReturn(actorUserId);

        when(myClassQueryService.listMyClasses(eq(actorUserId), any(MyClassQueryCriteria.class)))
                .thenReturn(null);

        myClassController.listMyTeachingClasses(
                principal,
                "java",
                "ACTIVE",
                "HK2",
                "2025-2026",
                1,
                10,
                "createdAt",
                "asc"
        );

        ArgumentCaptor<MyClassQueryCriteria> criteriaCaptor = ArgumentCaptor.forClass(MyClassQueryCriteria.class);
        verify(myClassQueryService).listMyClasses(eq(actorUserId), criteriaCaptor.capture());

        MyClassQueryCriteria criteria = criteriaCaptor.getValue();

        assertEquals("java", criteria.getQ());
        assertEquals(MyClassQueryCriteria.Scope.TEACHING, criteria.getScope());
        assertEquals("ACTIVE", criteria.getStatus());
        assertNull(criteria.getMemberStatus());
        assertEquals("HK2", criteria.getSemester());
        assertEquals("2025-2026", criteria.getAcademicYear());
        assertEquals(1, criteria.getPage());
        assertEquals(10, criteria.getSize());
        assertEquals(MyClassQueryCriteria.SortBy.CREATED_AT, criteria.getSortBy());
        assertEquals(MyClassQueryCriteria.SortDir.ASC, criteria.getSortDir());
    }


}