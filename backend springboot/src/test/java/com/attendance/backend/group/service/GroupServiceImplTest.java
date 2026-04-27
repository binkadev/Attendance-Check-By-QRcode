package com.attendance.backend.group.service;

import com.attendance.backend.common.exception.ApiException;
import com.attendance.backend.domain.entity.ClassGroup;
import com.attendance.backend.domain.entity.GroupMember;
import com.attendance.backend.domain.entity.GroupWeeklySchedule;
import com.attendance.backend.domain.enums.ApprovalMode;
import com.attendance.backend.domain.enums.GroupStatus;
import com.attendance.backend.domain.enums.MemberRole;
import com.attendance.backend.domain.enums.MemberStatus;
import com.attendance.backend.group.dto.CreateGroupRequest;
import com.attendance.backend.group.dto.GroupResponse;
import com.attendance.backend.group.dto.GroupWeeklyScheduleRequest;
import com.attendance.backend.group.dto.GroupWeeklyScheduleResponse;
import com.attendance.backend.group.dto.UpdateGroupRequest;
import com.attendance.backend.group.dto.UpdateGroupStatusRequest;
import com.attendance.backend.group.repository.ClassGroupRepository;
import com.attendance.backend.group.repository.GroupMemberRepository;
import com.attendance.backend.group.repository.GroupWeeklyScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupServiceImplTest {

    @Mock
    private ClassGroupRepository classGroupRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private GroupWeeklyScheduleRepository groupWeeklyScheduleRepository;

    @InjectMocks
    private GroupServiceImpl groupService;

    private UUID callerUserId;

    @BeforeEach
    void setUp() {
        callerUserId = UUID.randomUUID();
    }

    @Test
    void createGroup_shouldPersistStep2Fields_andCreateOwnerMembership_andSchedules() {
        CreateGroupRequest req = buildValidCreateRequest();

        when(classGroupRepository.saveAndFlush(any(ClassGroup.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(groupMemberRepository.saveAndFlush(any(GroupMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(groupWeeklyScheduleRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        doNothing().when(groupWeeklyScheduleRepository).flush();

        when(classGroupRepository.findActiveById(any(UUID.class)))
                .thenAnswer(invocation -> {
                    UUID groupId = invocation.getArgument(0);

                    ArgumentCaptor<ClassGroup> captor = ArgumentCaptor.forClass(ClassGroup.class);
                    verify(classGroupRepository, atLeastOnce()).saveAndFlush(captor.capture());

                    ClassGroup saved = captor.getAllValues().stream()
                            .filter(g -> groupId.equals(g.getId()))
                            .findFirst()
                            .orElseThrow();

                    return Optional.of(saved);
                });

        when(groupWeeklyScheduleRepository.findByGroupIdOrderByDayOfWeekAscStartTimeAsc(any(UUID.class)))
                .thenAnswer(invocation -> {
                    UUID groupId = invocation.getArgument(0);

                    @SuppressWarnings("unchecked")
                    ArgumentCaptor<List<GroupWeeklySchedule>> captor = ArgumentCaptor.forClass(List.class);
                    verify(groupWeeklyScheduleRepository, atLeastOnce()).saveAll(captor.capture());

                    return captor.getAllValues().stream()
                            .flatMap(List::stream)
                            .filter(item -> groupId.equals(item.getGroupId()))
                            .toList();
                });

        GroupResponse response = groupService.createGroup(callerUserId, req);

        ArgumentCaptor<ClassGroup> classGroupCaptor = ArgumentCaptor.forClass(ClassGroup.class);
        verify(classGroupRepository).saveAndFlush(classGroupCaptor.capture());

        ClassGroup savedGroup = classGroupCaptor.getValue();

        assertNotNull(savedGroup.getId());
        assertEquals(callerUserId, savedGroup.getOwnerUserId());
        assertEquals("Lập trình Android", savedGroup.getName());
        assertEquals("DBG001", savedGroup.getCode());
        assertEquals("INT1348", savedGroup.getCourseCode());
        assertEquals("D22CQCNPM02-N", savedGroup.getClassCode());
        assertEquals("DBG12345", savedGroup.getJoinCode());
        assertEquals("Link nhóm Zalo, tóm tắt đề cương...", savedGroup.getDescription());
        assertEquals("HK2", savedGroup.getSemester());
        assertEquals("2025-2026", savedGroup.getAcademicYear());
        assertEquals("CS Thu Duc", savedGroup.getCampus());
        assertEquals("A101", savedGroup.getRoom());
        assertEquals(Integer.valueOf(11), savedGroup.getTotalSessions());
        assertEquals(Integer.valueOf(3), savedGroup.getMaxAllowedAbsences());
        assertEquals(ApprovalMode.AUTO, savedGroup.getApprovalMode());
        assertFalse(savedGroup.isAllowAutoJoinOnCheckin());
        assertEquals(GroupStatus.ACTIVE, savedGroup.getStatus());

        ArgumentCaptor<GroupMember> groupMemberCaptor = ArgumentCaptor.forClass(GroupMember.class);
        verify(groupMemberRepository).saveAndFlush(groupMemberCaptor.capture());

        GroupMember savedOwnerMember = groupMemberCaptor.getValue();

        assertEquals(savedGroup.getId(), savedOwnerMember.getGroupId());
        assertEquals(callerUserId, savedOwnerMember.getUserId());
        assertEquals(MemberRole.OWNER, savedOwnerMember.getRole());
        assertEquals(MemberStatus.APPROVED, savedOwnerMember.getMemberStatus());
        assertNotNull(savedOwnerMember.getJoinedAt());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<GroupWeeklySchedule>> schedulesCaptor = ArgumentCaptor.forClass(List.class);
        verify(groupWeeklyScheduleRepository).saveAll(schedulesCaptor.capture());
        verify(groupWeeklyScheduleRepository).flush();

        List<GroupWeeklySchedule> savedSchedules = schedulesCaptor.getValue();
        assertEquals(2, savedSchedules.size());

        assertTrue(savedSchedules.stream().anyMatch(item ->
                savedGroup.getId().equals(item.getGroupId())
                        && "TUESDAY".equals(item.getDayOfWeek())
                        && LocalTime.of(8, 0).equals(item.getStartTime())
                        && LocalTime.of(10, 0).equals(item.getEndTime())
        ));

        assertTrue(savedSchedules.stream().anyMatch(item ->
                savedGroup.getId().equals(item.getGroupId())
                        && "THURSDAY".equals(item.getDayOfWeek())
                        && LocalTime.of(13, 0).equals(item.getStartTime())
                        && LocalTime.of(15, 0).equals(item.getEndTime())
        ));

        assertNotNull(response);
        assertEquals(savedGroup.getId(), response.id());
        assertEquals(callerUserId, response.ownerUserId());
        assertEquals("Lập trình Android", response.name());
        assertEquals("DBG001", response.code());
        assertEquals("INT1348", response.courseCode());
        assertEquals("D22CQCNPM02-N", response.classCode());
        assertEquals("DBG12345", response.joinCode());
        assertEquals("Link nhóm Zalo, tóm tắt đề cương...", response.description());
        assertEquals("HK2", response.semester());
        assertEquals("2025-2026", response.academicYear());
        assertEquals("CS Thu Duc", response.campus());
        assertEquals("A101", response.room());
        assertEquals(Integer.valueOf(11), response.totalSessions());
        assertEquals(Integer.valueOf(3), response.maxAllowedAbsences());
        assertEquals(ApprovalMode.AUTO, response.approvalMode());
        assertFalse(response.allowAutoJoinOnCheckin());
        assertEquals(GroupStatus.ACTIVE, response.status());
        assertNotNull(response.weeklySchedules());
        assertEquals(2, response.weeklySchedules().size());
        assertTrue(hasSchedule(response.weeklySchedules(), "TUESDAY", "08:00", "10:00"));
        assertTrue(hasSchedule(response.weeklySchedules(), "THURSDAY", "13:00", "15:00"));
    }

    @Test
    void createGroup_shouldThrowBadRequest_whenWeeklySchedulesEmpty() {
        CreateGroupRequest req = buildValidCreateRequest();
        req.setWeeklySchedules(List.of());

        assertThrows(ApiException.class, () -> groupService.createGroup(callerUserId, req));

        verifyNoInteractions(classGroupRepository);
        verifyNoInteractions(groupMemberRepository);
        verifyNoInteractions(groupWeeklyScheduleRepository);
    }

    @Test
    void createGroup_shouldThrowBadRequest_whenStartTimeIsNotEarlierThanEndTime() {
        CreateGroupRequest req = buildValidCreateRequest();
        req.setWeeklySchedules(List.of(schedule("TUESDAY", "10:00", "08:00")));

        assertThrows(ApiException.class, () -> groupService.createGroup(callerUserId, req));

        verifyNoInteractions(classGroupRepository);
        verifyNoInteractions(groupMemberRepository);
        verifyNoInteractions(groupWeeklyScheduleRepository);
    }

    @Test
    void createGroup_shouldThrowBadRequest_whenMaxAllowedAbsencesGreaterThanTotalSessions() {
        CreateGroupRequest req = buildValidCreateRequest();
        req.setTotalSessions(3);
        req.setMaxAllowedAbsences(5);

        assertThrows(ApiException.class, () -> groupService.createGroup(callerUserId, req));

        verifyNoInteractions(classGroupRepository);
        verifyNoInteractions(groupMemberRepository);
        verifyNoInteractions(groupWeeklyScheduleRepository);
    }

    @Test
    void createGroup_shouldThrowBadRequest_whenDuplicateWeeklyScheduleExists() {
        CreateGroupRequest req = buildValidCreateRequest();
        req.setWeeklySchedules(List.of(
                schedule("TUESDAY", "08:00", "10:00"),
                schedule("tuesday", "08:00", "10:00")
        ));

        assertThrows(ApiException.class, () -> groupService.createGroup(callerUserId, req));

        verifyNoInteractions(classGroupRepository);
        verifyNoInteractions(groupMemberRepository);
        verifyNoInteractions(groupWeeklyScheduleRepository);
    }

    @Test
    void createGroup_shouldNormalizeBlankOptionalMetadataFieldsToNull_andStillPersistStep2Fields() {
        CreateGroupRequest req = buildValidCreateRequest();
        req.setCourseCode("   ");
        req.setClassCode("");
        req.setDescription("   ");
        req.setAcademicYear("  ");
        req.setCampus("");
        req.setRoom("A102");

        when(classGroupRepository.saveAndFlush(any(ClassGroup.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(groupMemberRepository.saveAndFlush(any(GroupMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(groupWeeklyScheduleRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        doNothing().when(groupWeeklyScheduleRepository).flush();

        when(classGroupRepository.findActiveById(any(UUID.class)))
                .thenAnswer(invocation -> {
                    UUID groupId = invocation.getArgument(0);

                    ArgumentCaptor<ClassGroup> captor = ArgumentCaptor.forClass(ClassGroup.class);
                    verify(classGroupRepository, atLeastOnce()).saveAndFlush(captor.capture());

                    ClassGroup saved = captor.getAllValues().stream()
                            .filter(g -> groupId.equals(g.getId()))
                            .findFirst()
                            .orElseThrow();

                    return Optional.of(saved);
                });

        when(groupWeeklyScheduleRepository.findByGroupIdOrderByDayOfWeekAscStartTimeAsc(any(UUID.class)))
                .thenAnswer(invocation -> {
                    UUID groupId = invocation.getArgument(0);

                    @SuppressWarnings("unchecked")
                    ArgumentCaptor<List<GroupWeeklySchedule>> captor = ArgumentCaptor.forClass(List.class);
                    verify(groupWeeklyScheduleRepository, atLeastOnce()).saveAll(captor.capture());

                    return captor.getAllValues().stream()
                            .flatMap(List::stream)
                            .filter(item -> groupId.equals(item.getGroupId()))
                            .toList();
                });

        GroupResponse response = groupService.createGroup(callerUserId, req);

        ArgumentCaptor<ClassGroup> classGroupCaptor = ArgumentCaptor.forClass(ClassGroup.class);
        verify(classGroupRepository).saveAndFlush(classGroupCaptor.capture());

        ClassGroup savedGroup = classGroupCaptor.getValue();

        assertNull(savedGroup.getCourseCode());
        assertNull(savedGroup.getClassCode());
        assertNull(savedGroup.getDescription());
        assertNull(savedGroup.getAcademicYear());
        assertNull(savedGroup.getCampus());
        assertEquals("A102", savedGroup.getRoom());
        assertEquals(Integer.valueOf(11), savedGroup.getTotalSessions());
        assertEquals(Integer.valueOf(3), savedGroup.getMaxAllowedAbsences());

        assertNotNull(response);
        assertNull(response.courseCode());
        assertNull(response.classCode());
        assertNull(response.description());
        assertNull(response.academicYear());
        assertNull(response.campus());
        assertEquals("HK2", response.semester());
        assertEquals("A102", response.room());
        assertEquals(Integer.valueOf(11), response.totalSessions());
        assertEquals(Integer.valueOf(3), response.maxAllowedAbsences());
        assertNotNull(response.weeklySchedules());
        assertEquals(2, response.weeklySchedules().size());
        assertTrue(hasSchedule(response.weeklySchedules(), "TUESDAY", "08:00", "10:00"));
        assertTrue(hasSchedule(response.weeklySchedules(), "THURSDAY", "13:00", "15:00"));
    }

    @Test
    void getGroupDetail_shouldReturnFullGroupDetail_whenOwnerHasAccess() {
        UUID groupId = UUID.randomUUID();

        ClassGroup group = buildGroup(groupId, callerUserId);
        GroupMember ownerMembership = buildMembership(groupId, callerUserId, MemberRole.OWNER, MemberStatus.APPROVED);

        List<GroupWeeklySchedule> schedules = List.of(
                buildSchedule(groupId, "MONDAY", "09:00", "11:00"),
                buildSchedule(groupId, "WEDNESDAY", "13:00", "15:00")
        );

        when(classGroupRepository.findActiveById(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(groupId, callerUserId)).thenReturn(Optional.of(ownerMembership));
        when(groupWeeklyScheduleRepository.findByGroupIdOrderByDayOfWeekAscStartTimeAsc(groupId)).thenReturn(schedules);

        GroupResponse response = groupService.getGroupDetail(callerUserId, groupId);

        assertNotNull(response);
        assertEquals(groupId, response.id());
        assertEquals(callerUserId, response.ownerUserId());
        assertEquals("Lập trình Android", response.name());
        assertEquals("DBG001", response.code());
        assertEquals("INT1348", response.courseCode());
        assertEquals("D22CQCNPM02-N", response.classCode());
        assertEquals("DBG12345", response.joinCode());
        assertEquals("Link nhóm Zalo, tóm tắt đề cương...", response.description());
        assertEquals("HK2", response.semester());
        assertEquals("2025-2026", response.academicYear());
        assertEquals("CS Thu Duc", response.campus());
        assertEquals("A101", response.room());
        assertEquals(Integer.valueOf(11), response.totalSessions());
        assertEquals(Integer.valueOf(3), response.maxAllowedAbsences());
        assertEquals(ApprovalMode.AUTO, response.approvalMode());
        assertFalse(response.allowAutoJoinOnCheckin());
        assertEquals(GroupStatus.ACTIVE, response.status());
        assertEquals(2, response.weeklySchedules().size());
        assertTrue(hasSchedule(response.weeklySchedules(), "MONDAY", "09:00", "11:00"));
        assertTrue(hasSchedule(response.weeklySchedules(), "WEDNESDAY", "13:00", "15:00"));
    }

    @Test
    void getGroupDetail_shouldReturnFullGroupDetail_whenApprovedMemberHasReadAccess() {
        UUID groupId = UUID.randomUUID();
        UUID memberUserId = UUID.randomUUID();

        ClassGroup group = buildGroup(groupId, callerUserId);
        GroupMember membership = buildMembership(groupId, memberUserId, MemberRole.MEMBER, MemberStatus.APPROVED);

        List<GroupWeeklySchedule> schedules = List.of(
                buildSchedule(groupId, "MONDAY", "09:00", "11:00"),
                buildSchedule(groupId, "WEDNESDAY", "13:00", "15:00")
        );

        when(classGroupRepository.findActiveById(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(groupId, memberUserId)).thenReturn(Optional.of(membership));
        when(groupWeeklyScheduleRepository.findByGroupIdOrderByDayOfWeekAscStartTimeAsc(groupId)).thenReturn(schedules);
        when(groupMemberRepository.findUserFullNameById(callerUserId)).thenReturn(Optional.of("Owner Test"));
        when(groupMemberRepository.countByGroupIdAndRoleAndMemberStatus(groupId, MemberRole.MEMBER, MemberStatus.APPROVED))
                .thenReturn(1L);

        GroupResponse response = groupService.getGroupDetail(memberUserId, groupId);

        assertNotNull(response);
        assertEquals(groupId, response.id());
        assertEquals("Lập trình Android", response.name());
        assertEquals("Owner Test", response.lecturerName());
        assertEquals(1L, response.studentCount());
        assertEquals(2, response.weeklySchedules().size());
    }

    @Test
    void getGroupDetail_shouldThrowForbidden_whenUserIsNotMember() {
        UUID groupId = UUID.randomUUID();
        UUID outsiderUserId = UUID.randomUUID();

        ClassGroup group = buildGroup(groupId, callerUserId);

        when(classGroupRepository.findActiveById(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(groupId, outsiderUserId)).thenReturn(Optional.empty());

        assertThrows(ApiException.class, () -> groupService.getGroupDetail(outsiderUserId, groupId));

        verify(groupWeeklyScheduleRepository, never()).findByGroupIdOrderByDayOfWeekAscStartTimeAsc(any(UUID.class));
    }

    @Test
    void updateGroup_shouldUpdateFullFormAndReplaceWeeklySchedules() {
        UUID groupId = UUID.randomUUID();

        ClassGroup group = buildGroup(groupId, callerUserId);
        GroupMember ownerMembership = buildMembership(groupId, callerUserId, MemberRole.OWNER, MemberStatus.APPROVED);

        List<GroupWeeklySchedule> existingSchedules = List.of(
                buildSchedule(groupId, "TUESDAY", "08:00", "10:00")
        );

        List<GroupWeeklySchedule> updatedSchedules = List.of(
                buildSchedule(groupId, "MONDAY", "09:00", "11:00"),
                buildSchedule(groupId, "FRIDAY", "14:00", "16:00")
        );

        UpdateGroupRequest req = new UpdateGroupRequest();
        req.setName("Lập trình Java nâng cao");
        req.setCourseCode("INT4999");
        req.setClassCode("D22CQCNPM99-N");
        req.setJoinCode("JOIN9999");
        req.setDescription("After update");
        req.setSemester("HK3");
        req.setAcademicYear("2026-2027");
        req.setCampus("CS Quan 9");
        req.setRoom("C301");
        req.setApprovalMode(ApprovalMode.MANUAL);
        req.setAllowAutoJoinOnCheckin(true);
        req.setTotalSessions(15);
        req.setMaxAllowedAbsences(4);
        req.setWeeklySchedules(List.of(
                schedule("MONDAY", "09:00", "11:00"),
                schedule("FRIDAY", "14:00", "16:00")
        ));

        when(classGroupRepository.findActiveByIdForUpdate(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(groupId, callerUserId)).thenReturn(Optional.of(ownerMembership));
        when(groupWeeklyScheduleRepository.findByGroupIdOrderByDayOfWeekAscStartTimeAsc(groupId))
                .thenReturn(existingSchedules)
                .thenReturn(updatedSchedules);
        when(classGroupRepository.saveAndFlush(any(ClassGroup.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(classGroupRepository.findActiveById(groupId)).thenReturn(Optional.of(group));
        when(groupWeeklyScheduleRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(groupWeeklyScheduleRepository).deleteByGroupId(groupId);
        doNothing().when(groupWeeklyScheduleRepository).flush();

        GroupResponse response = groupService.updateGroup(callerUserId, groupId, req);

        assertEquals("Lập trình Java nâng cao", group.getName());
        assertEquals("INT4999", group.getCourseCode());
        assertEquals("D22CQCNPM99-N", group.getClassCode());
        assertEquals("JOIN9999", group.getJoinCode());
        assertEquals("After update", group.getDescription());
        assertEquals("HK3", group.getSemester());
        assertEquals("2026-2027", group.getAcademicYear());
        assertEquals("CS Quan 9", group.getCampus());
        assertEquals("C301", group.getRoom());
        assertEquals(Integer.valueOf(15), group.getTotalSessions());
        assertEquals(Integer.valueOf(4), group.getMaxAllowedAbsences());
        assertEquals(ApprovalMode.MANUAL, group.getApprovalMode());
        assertTrue(group.isAllowAutoJoinOnCheckin());

        verify(groupWeeklyScheduleRepository).deleteByGroupId(groupId);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<GroupWeeklySchedule>> schedulesCaptor = ArgumentCaptor.forClass(List.class);
        verify(groupWeeklyScheduleRepository).saveAll(schedulesCaptor.capture());

        List<GroupWeeklySchedule> savedSchedules = schedulesCaptor.getValue();
        assertEquals(2, savedSchedules.size());
        assertTrue(savedSchedules.stream().anyMatch(item ->
                groupId.equals(item.getGroupId())
                        && "MONDAY".equals(item.getDayOfWeek())
                        && LocalTime.of(9, 0).equals(item.getStartTime())
                        && LocalTime.of(11, 0).equals(item.getEndTime())
        ));
        assertTrue(savedSchedules.stream().anyMatch(item ->
                groupId.equals(item.getGroupId())
                        && "FRIDAY".equals(item.getDayOfWeek())
                        && LocalTime.of(14, 0).equals(item.getStartTime())
                        && LocalTime.of(16, 0).equals(item.getEndTime())
        ));

        assertNotNull(response);
        assertEquals(groupId, response.id());
        assertEquals("Lập trình Java nâng cao", response.name());
        assertEquals("DBG001", response.code());
        assertEquals("INT4999", response.courseCode());
        assertEquals("D22CQCNPM99-N", response.classCode());
        assertEquals("JOIN9999", response.joinCode());
        assertEquals("After update", response.description());
        assertEquals("HK3", response.semester());
        assertEquals("2026-2027", response.academicYear());
        assertEquals("CS Quan 9", response.campus());
        assertEquals("C301", response.room());
        assertEquals(Integer.valueOf(15), response.totalSessions());
        assertEquals(Integer.valueOf(4), response.maxAllowedAbsences());
        assertEquals(ApprovalMode.MANUAL, response.approvalMode());
        assertTrue(response.allowAutoJoinOnCheckin());
        assertEquals(2, response.weeklySchedules().size());
        assertTrue(hasSchedule(response.weeklySchedules(), "MONDAY", "09:00", "11:00"));
        assertTrue(hasSchedule(response.weeklySchedules(), "FRIDAY", "14:00", "16:00"));
    }

    @Test
    void updateGroup_shouldAllowPartialUpdateWithoutReplacingSchedules_whenWeeklySchedulesNotProvided() {
        UUID groupId = UUID.randomUUID();

        ClassGroup group = buildGroup(groupId, callerUserId);
        GroupMember ownerMembership = buildMembership(groupId, callerUserId, MemberRole.OWNER, MemberStatus.APPROVED);

        List<GroupWeeklySchedule> existingSchedules = List.of(
                buildSchedule(groupId, "TUESDAY", "08:00", "10:00"),
                buildSchedule(groupId, "THURSDAY", "13:00", "15:00")
        );

        UpdateGroupRequest req = new UpdateGroupRequest();
        req.setName("Tên mới");
        req.setRoom("B202");

        when(classGroupRepository.findActiveByIdForUpdate(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(groupId, callerUserId)).thenReturn(Optional.of(ownerMembership));
        when(groupWeeklyScheduleRepository.findByGroupIdOrderByDayOfWeekAscStartTimeAsc(groupId))
                .thenReturn(existingSchedules)
                .thenReturn(existingSchedules);
        when(classGroupRepository.saveAndFlush(any(ClassGroup.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(classGroupRepository.findActiveById(groupId)).thenReturn(Optional.of(group));

        GroupResponse response = groupService.updateGroup(callerUserId, groupId, req);

        assertEquals("Tên mới", group.getName());
        assertEquals("B202", group.getRoom());
        assertEquals("INT1348", group.getCourseCode());
        assertEquals("D22CQCNPM02-N", group.getClassCode());
        assertEquals(Integer.valueOf(11), group.getTotalSessions());
        assertEquals(Integer.valueOf(3), group.getMaxAllowedAbsences());

        verify(groupWeeklyScheduleRepository, never()).deleteByGroupId(any(UUID.class));
        verify(groupWeeklyScheduleRepository, never()).saveAll(anyList());

        assertNotNull(response);
        assertEquals("Tên mới", response.name());
        assertEquals("B202", response.room());
        assertEquals(2, response.weeklySchedules().size());
        assertTrue(hasSchedule(response.weeklySchedules(), "TUESDAY", "08:00", "10:00"));
        assertTrue(hasSchedule(response.weeklySchedules(), "THURSDAY", "13:00", "15:00"));
    }

    @Test
    void updateGroup_shouldThrowBadRequest_whenInvalidSchedule() {
        UUID groupId = UUID.randomUUID();

        ClassGroup group = buildGroup(groupId, callerUserId);
        GroupMember ownerMembership = buildMembership(groupId, callerUserId, MemberRole.OWNER, MemberStatus.APPROVED);

        List<GroupWeeklySchedule> existingSchedules = List.of(
                buildSchedule(groupId, "TUESDAY", "08:00", "10:00")
        );

        UpdateGroupRequest req = new UpdateGroupRequest();
        req.setWeeklySchedules(List.of(
                schedule("MONDAY", "11:00", "09:00")
        ));

        when(classGroupRepository.findActiveByIdForUpdate(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(groupId, callerUserId)).thenReturn(Optional.of(ownerMembership));
        when(groupWeeklyScheduleRepository.findByGroupIdOrderByDayOfWeekAscStartTimeAsc(groupId)).thenReturn(existingSchedules);

        assertThrows(ApiException.class, () -> groupService.updateGroup(callerUserId, groupId, req));

        verify(classGroupRepository, never()).saveAndFlush(any(ClassGroup.class));
        verify(groupWeeklyScheduleRepository, never()).deleteByGroupId(any(UUID.class));
        verify(groupWeeklyScheduleRepository, never()).saveAll(anyList());
    }

    @Test
    void updateGroup_shouldThrowBadRequest_whenInvalidPolicy() {
        UUID groupId = UUID.randomUUID();

        ClassGroup group = buildGroup(groupId, callerUserId);
        GroupMember ownerMembership = buildMembership(groupId, callerUserId, MemberRole.OWNER, MemberStatus.APPROVED);

        List<GroupWeeklySchedule> existingSchedules = List.of(
                buildSchedule(groupId, "TUESDAY", "08:00", "10:00")
        );

        UpdateGroupRequest req = new UpdateGroupRequest();
        req.setTotalSessions(2);
        req.setMaxAllowedAbsences(5);

        when(classGroupRepository.findActiveByIdForUpdate(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(groupId, callerUserId)).thenReturn(Optional.of(ownerMembership));
        when(groupWeeklyScheduleRepository.findByGroupIdOrderByDayOfWeekAscStartTimeAsc(groupId)).thenReturn(existingSchedules);

        assertThrows(ApiException.class, () -> groupService.updateGroup(callerUserId, groupId, req));

        verify(classGroupRepository, never()).saveAndFlush(any(ClassGroup.class));
        verify(groupWeeklyScheduleRepository, never()).deleteByGroupId(any(UUID.class));
        verify(groupWeeklyScheduleRepository, never()).saveAll(anyList());
    }

    @Test
    void updateGroup_shouldThrowForbidden_whenCallerHasNoManageAccess() {
        UUID groupId = UUID.randomUUID();

        ClassGroup group = buildGroup(groupId, callerUserId);
        GroupMember membership = buildMembership(groupId, callerUserId, MemberRole.MEMBER, MemberStatus.APPROVED);

        UpdateGroupRequest req = new UpdateGroupRequest();
        req.setName("Không được sửa");

        when(classGroupRepository.findActiveByIdForUpdate(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(groupId, callerUserId)).thenReturn(Optional.of(membership));

        assertThrows(ApiException.class, () -> groupService.updateGroup(callerUserId, groupId, req));

        verify(groupWeeklyScheduleRepository, never()).findByGroupIdOrderByDayOfWeekAscStartTimeAsc(any(UUID.class));
        verify(classGroupRepository, never()).saveAndFlush(any(ClassGroup.class));
    }


    @Test
    void updateGroupStatus_shouldArchiveGroup_whenOwnerHasAccess() {
        UUID groupId = UUID.randomUUID();

        ClassGroup group = buildGroup(groupId, callerUserId);
        group.setStatus(GroupStatus.ACTIVE);

        GroupMember ownerMembership = buildMembership(groupId, callerUserId, MemberRole.OWNER, MemberStatus.APPROVED);

        UpdateGroupStatusRequest req = new UpdateGroupStatusRequest();
        req.setStatus(GroupStatus.ARCHIVED);

        when(classGroupRepository.findByIdAndDeletedAtIsNullForUpdate(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(groupId, callerUserId)).thenReturn(Optional.of(ownerMembership));
        when(classGroupRepository.saveAndFlush(any(ClassGroup.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(groupWeeklyScheduleRepository.findByGroupIdOrderByDayOfWeekAscStartTimeAsc(groupId))
                .thenReturn(List.of(
                        buildSchedule(groupId, "TUESDAY", "08:00", "10:00")
                ));

        GroupResponse response = groupService.updateGroupStatus(callerUserId, groupId, req);

        assertEquals(GroupStatus.ARCHIVED, group.getStatus());
        assertEquals(GroupStatus.ARCHIVED, response.status());
    }

    @Test
    void updateGroupStatus_shouldRestoreGroupToActive_whenOwnerHasAccess() {
        UUID groupId = UUID.randomUUID();

        ClassGroup group = buildGroup(groupId, callerUserId);
        group.setStatus(GroupStatus.ARCHIVED);

        GroupMember ownerMembership = buildMembership(groupId, callerUserId, MemberRole.OWNER, MemberStatus.APPROVED);

        UpdateGroupStatusRequest req = new UpdateGroupStatusRequest();
        req.setStatus(GroupStatus.ACTIVE);

        when(classGroupRepository.findByIdAndDeletedAtIsNullForUpdate(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(groupId, callerUserId)).thenReturn(Optional.of(ownerMembership));
        when(classGroupRepository.saveAndFlush(any(ClassGroup.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(groupWeeklyScheduleRepository.findByGroupIdOrderByDayOfWeekAscStartTimeAsc(groupId))
                .thenReturn(List.of(
                        buildSchedule(groupId, "TUESDAY", "08:00", "10:00")
                ));

        GroupResponse response = groupService.updateGroupStatus(callerUserId, groupId, req);

        assertEquals(GroupStatus.ACTIVE, group.getStatus());
        assertEquals(GroupStatus.ACTIVE, response.status());
    }

    @Test
    void updateGroupStatus_shouldThrowForbidden_whenCallerIsNotOwner() {
        UUID groupId = UUID.randomUUID();

        ClassGroup group = buildGroup(groupId, callerUserId);
        GroupMember membership = buildMembership(groupId, callerUserId, MemberRole.CO_HOST, MemberStatus.APPROVED);

        UpdateGroupStatusRequest req = new UpdateGroupStatusRequest();
        req.setStatus(GroupStatus.ARCHIVED);

        when(classGroupRepository.findByIdAndDeletedAtIsNullForUpdate(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(groupId, callerUserId)).thenReturn(Optional.of(membership));

        assertThrows(ApiException.class, () -> groupService.updateGroupStatus(callerUserId, groupId, req));

        verify(classGroupRepository, never()).saveAndFlush(any(ClassGroup.class));
    }

    private boolean hasSchedule(List<GroupWeeklyScheduleResponse> schedules, String dayOfWeek, String startTime, String endTime) {
        return schedules.stream().anyMatch(item ->
                dayOfWeek.equals(item.dayOfWeek())
                        && startTime.equals(item.startTime())
                        && endTime.equals(item.endTime())
        );
    }

    private CreateGroupRequest buildValidCreateRequest() {
        CreateGroupRequest req = new CreateGroupRequest();
        req.setName("  Lập trình Android  ");
        req.setCode("  DBG001  ");
        req.setCourseCode("  INT1348  ");
        req.setClassCode("  D22CQCNPM02-N  ");
        req.setJoinCode("  DBG12345  ");
        req.setDescription("  Link nhóm Zalo, tóm tắt đề cương...  ");
        req.setSemester("  HK2  ");
        req.setAcademicYear("  2025-2026  ");
        req.setCampus("  CS Thu Duc  ");
        req.setRoom("  A101  ");
        req.setApprovalMode(ApprovalMode.AUTO);
        req.setAllowAutoJoinOnCheckin(false);
        req.setTotalSessions(11);
        req.setMaxAllowedAbsences(3);
        req.setWeeklySchedules(List.of(
                schedule("tuesday", "08:00", "10:00"),
                schedule("THURSDAY", "13:00", "15:00")
        ));
        return req;
    }

    private GroupWeeklyScheduleRequest schedule(String dayOfWeek, String startTime, String endTime) {
        GroupWeeklyScheduleRequest req = new GroupWeeklyScheduleRequest();
        req.setDayOfWeek(dayOfWeek);
        req.setStartTime(startTime);
        req.setEndTime(endTime);
        return req;
    }

    private ClassGroup buildGroup(UUID groupId, UUID ownerUserId) {
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
        group.setStatus(GroupStatus.ACTIVE);
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

    private GroupWeeklySchedule buildSchedule(UUID groupId, String dayOfWeek, String startTime, String endTime) {
        GroupWeeklySchedule schedule = new GroupWeeklySchedule();
        schedule.setId(UUID.randomUUID());
        schedule.setGroupId(groupId);
        schedule.setDayOfWeek(dayOfWeek);
        schedule.setStartTime(LocalTime.parse(startTime));
        schedule.setEndTime(LocalTime.parse(endTime));
        schedule.setDeletedAt(null);
        return schedule;
    }
}