package com.attendance.backend.attendance.service;

import com.attendance.backend.attendance.dto.MyAttendanceHistoryItemResponse;
import com.attendance.backend.attendance.dto.PageMyAttendanceHistoryResponse;
import com.attendance.backend.attendance.dto.UpcomingSessionResponse;
import com.attendance.backend.attendance.repository.AttendanceReadQueryRepository;
import com.attendance.backend.common.exception.ApiException;
import com.attendance.backend.domain.entity.ClassGroup;
import com.attendance.backend.domain.entity.GroupMember;
import com.attendance.backend.domain.enums.ApprovalMode;
import com.attendance.backend.domain.enums.GroupStatus;
import com.attendance.backend.domain.enums.MemberRole;
import com.attendance.backend.domain.enums.MemberStatus;
import com.attendance.backend.group.repository.ClassGroupRepository;
import com.attendance.backend.group.repository.GroupMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceReadServiceImplTest {

    @Mock
    private AttendanceReadQueryRepository attendanceReadQueryRepository;

    @Mock
    private ClassGroupRepository classGroupRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @InjectMocks
    private AttendanceReadServiceImpl attendanceReadService;

    private UUID actorUserId;
    private UUID groupId;

    @BeforeEach
    void setUp() {
        actorUserId = UUID.randomUUID();
        groupId = UUID.randomUUID();
    }

    @Test
    void listUpcomingSessions_shouldReturnUpcomingSessions_withSafeLimit() {
        UUID sessionId = UUID.randomUUID();

        List<UpcomingSessionResponse> expected = List.of(
                new UpcomingSessionResponse(
                        sessionId,
                        groupId,
                        "Buổi học số 1",
                        Instant.parse("2026-04-21T01:00:00Z"),
                        Instant.parse("2026-04-21T03:00:00Z"),
                        "A101",
                        "Lập trình Android"
                )
        );

        when(attendanceReadQueryRepository.findUpcomingSessions(actorUserId, 100))
                .thenReturn(expected);

        List<UpcomingSessionResponse> actual =
                attendanceReadService.listUpcomingSessions(actorUserId, 999);

        assertEquals(1, actual.size());
        assertEquals(sessionId, actual.get(0).sessionId());
        assertEquals(groupId, actual.get(0).groupId());
        assertEquals("Buổi học số 1", actual.get(0).sessionName());
        assertEquals("A101", actual.get(0).room());
        assertEquals("Lập trình Android", actual.get(0).groupName());

        verify(attendanceReadQueryRepository).findUpcomingSessions(actorUserId, 100);
    }

    @Test
    void listMyAttendancesInGroup_shouldReturnPagedHistory_whenApprovedMember() {
        ClassGroup group = buildGroup(groupId, UUID.randomUUID(), GroupStatus.ACTIVE);
        GroupMember membership = buildMembership(groupId, actorUserId, MemberRole.MEMBER, MemberStatus.APPROVED);

        UUID sessionId = UUID.randomUUID();

        List<MyAttendanceHistoryItemResponse> items = List.of(
                new MyAttendanceHistoryItemResponse(
                        sessionId,
                        groupId,
                        "Lập trình Android",
                        "Buổi học số 1",
                        LocalDate.of(2026, 4, 21),
                        Instant.parse("2026-04-21T01:00:00Z"),
                        Instant.parse("2026-04-21T03:00:00Z"),
                        "CLOSED",
                        "PRESENT",
                        Instant.parse("2026-04-21T01:05:00Z"),
                        "QR",
                        false,
                        null
                )
        );

        when(classGroupRepository.findActiveById(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(groupId, actorUserId)).thenReturn(Optional.of(membership));
        when(attendanceReadQueryRepository.findMyAttendancesInGroup(actorUserId, groupId, 0, 20))
                .thenReturn(items);
        when(attendanceReadQueryRepository.countMyAttendancesInGroup(actorUserId, groupId))
                .thenReturn(1L);

        PageMyAttendanceHistoryResponse response =
                attendanceReadService.listMyAttendancesInGroup(actorUserId, groupId, 0, 20);

        assertNotNull(response);
        assertEquals(1, response.items().size());
        assertEquals(0, response.page());
        assertEquals(20, response.size());
        assertEquals(1L, response.totalElements());
        assertEquals(1, response.totalPages());

        MyAttendanceHistoryItemResponse item = response.items().get(0);
        assertEquals(sessionId, item.sessionId());
        assertEquals(groupId, item.groupId());
        assertEquals("Lập trình Android", item.groupName());
        assertEquals("Buổi học số 1", item.sessionName());
        assertEquals("PRESENT", item.attendanceStatus());
        assertEquals("QR", item.checkInMethod());
    }

    @Test
    void listMyAttendancesInGroup_shouldThrowForbidden_whenUserIsNotGroupMember() {
        ClassGroup group = buildGroup(groupId, UUID.randomUUID(), GroupStatus.ACTIVE);

        when(classGroupRepository.findActiveById(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(groupId, actorUserId)).thenReturn(Optional.empty());

        assertThrows(ApiException.class, () ->
                attendanceReadService.listMyAttendancesInGroup(actorUserId, groupId, 0, 20)
        );

        verify(attendanceReadQueryRepository, never())
                .findMyAttendancesInGroup(any(UUID.class), any(UUID.class), anyInt(), anyInt());
        verify(attendanceReadQueryRepository, never())
                .countMyAttendancesInGroup(any(UUID.class), any(UUID.class));
    }

    @Test
    void listMyAttendancesInGroup_shouldThrowForbidden_whenMembershipIsNotApproved() {
        ClassGroup group = buildGroup(groupId, UUID.randomUUID(), GroupStatus.ACTIVE);
        GroupMember membership = buildMembership(groupId, actorUserId, MemberRole.MEMBER, MemberStatus.PENDING);

        when(classGroupRepository.findActiveById(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(groupId, actorUserId)).thenReturn(Optional.of(membership));

        assertThrows(ApiException.class, () ->
                attendanceReadService.listMyAttendancesInGroup(actorUserId, groupId, 0, 20)
        );

        verify(attendanceReadQueryRepository, never())
                .findMyAttendancesInGroup(any(UUID.class), any(UUID.class), anyInt(), anyInt());
    }

    @Test
    void exportGroupAttendance_shouldReturnXlsxBytes_whenOwnerHasAccess() {
        ClassGroup group = buildGroup(groupId, actorUserId, GroupStatus.ACTIVE);
        GroupMember membership = buildMembership(groupId, actorUserId, MemberRole.OWNER, MemberStatus.APPROVED);

        List<MyAttendanceHistoryItemResponse> exportRows = List.of(
                new MyAttendanceHistoryItemResponse(
                        UUID.randomUUID(),
                        groupId,
                        "Lập trình Android",
                        "Buổi học số 1",
                        LocalDate.of(2026, 4, 21),
                        Instant.parse("2026-04-21T01:00:00Z"),
                        Instant.parse("2026-04-21T03:00:00Z"),
                        "CLOSED",
                        "ABSENT",
                        null,
                        null,
                        false,
                        null
                )
        );

        when(classGroupRepository.findActiveById(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(groupId, actorUserId)).thenReturn(Optional.of(membership));
        when(attendanceReadQueryRepository.findGroupAttendanceForExport(groupId)).thenReturn(exportRows);

        byte[] file = attendanceReadService.exportGroupAttendance(actorUserId, groupId);

        assertNotNull(file);
        assertTrue(file.length > 0);
        assertEquals('P', file[0]);
        assertEquals('K', file[1]);

        verify(attendanceReadQueryRepository).findGroupAttendanceForExport(groupId);
    }

    @Test
    void exportGroupAttendance_shouldAllowCoHost() {
        ClassGroup group = buildGroup(groupId, UUID.randomUUID(), GroupStatus.ACTIVE);
        GroupMember membership = buildMembership(groupId, actorUserId, MemberRole.CO_HOST, MemberStatus.APPROVED);

        when(classGroupRepository.findActiveById(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(groupId, actorUserId)).thenReturn(Optional.of(membership));
        when(attendanceReadQueryRepository.findGroupAttendanceForExport(groupId)).thenReturn(List.of());

        byte[] file = attendanceReadService.exportGroupAttendance(actorUserId, groupId);

        assertNotNull(file);
        assertTrue(file.length > 0);
        assertEquals('P', file[0]);
        assertEquals('K', file[1]);
    }

    @Test
    void exportGroupAttendance_shouldThrowForbidden_whenMemberTriesToExport() {
        ClassGroup group = buildGroup(groupId, UUID.randomUUID(), GroupStatus.ACTIVE);
        GroupMember membership = buildMembership(groupId, actorUserId, MemberRole.MEMBER, MemberStatus.APPROVED);

        when(classGroupRepository.findActiveById(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(groupId, actorUserId)).thenReturn(Optional.of(membership));

        assertThrows(ApiException.class, () ->
                attendanceReadService.exportGroupAttendance(actorUserId, groupId)
        );

        verify(attendanceReadQueryRepository, never()).findGroupAttendanceForExport(any(UUID.class));
    }

    @Test
    void exportGroupAttendance_shouldThrowNotFound_whenGroupNotFound() {
        when(classGroupRepository.findActiveById(groupId)).thenReturn(Optional.empty());

        assertThrows(ApiException.class, () ->
                attendanceReadService.exportGroupAttendance(actorUserId, groupId)
        );

        verify(groupMemberRepository, never()).findByGroupIdAndUserId(any(UUID.class), any(UUID.class));
        verify(attendanceReadQueryRepository, never()).findGroupAttendanceForExport(any(UUID.class));
    }

    private ClassGroup buildGroup(UUID groupId, UUID ownerUserId, GroupStatus status) {
        ClassGroup group = new ClassGroup();
        group.setId(groupId);
        group.setOwnerUserId(ownerUserId);
        group.setName("Lập trình Android");
        group.setCode("DBG001");
        group.setCourseCode("INT1348");
        group.setClassCode("D22CQCNPM02-N");
        group.setJoinCode("DBG12345");
        group.setDescription("Link nhóm Zalo, tóm tắt đề cương...");
        group.setSemester("HK2");
        group.setAcademicYear("2025-2026");
        group.setCampus("CS Thu Duc");
        group.setRoom("A101");
        group.setTotalSessions(11);
        group.setMaxAllowedAbsences(3);
        group.setApprovalMode(ApprovalMode.AUTO);
        group.setAllowAutoJoinOnCheckin(false);
        group.setStatus(status);
        group.setDeletedAt(null);
        return group;
    }

    private GroupMember buildMembership(UUID groupId, UUID userId, MemberRole role, MemberStatus memberStatus) {
        GroupMember membership = new GroupMember();
        membership.setGroupId(groupId);
        membership.setUserId(userId);
        membership.setRole(role);
        membership.setMemberStatus(memberStatus);
        membership.setJoinedAt(Instant.now());
        return membership;
    }
}