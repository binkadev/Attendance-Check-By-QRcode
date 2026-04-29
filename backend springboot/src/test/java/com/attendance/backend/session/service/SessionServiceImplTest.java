package com.attendance.backend.session.service;

import com.attendance.backend.attendance.repository.AttendanceSessionRepository;
import com.attendance.backend.common.exception.ApiException;
import com.attendance.backend.domain.entity.AttendanceSession;
import com.attendance.backend.domain.entity.ClassGroup;
import com.attendance.backend.domain.entity.GroupMember;
import com.attendance.backend.domain.enums.ApprovalMode;
import com.attendance.backend.domain.enums.GroupStatus;
import com.attendance.backend.domain.enums.MemberRole;
import com.attendance.backend.domain.enums.MemberStatus;
import com.attendance.backend.domain.enums.SessionStatus;
import com.attendance.backend.group.repository.ClassGroupRepository;
import com.attendance.backend.group.repository.GroupMemberRepository;
import com.attendance.backend.session.dto.CreateSessionRequest;
import com.attendance.backend.session.dto.PageSessionResponse;
import com.attendance.backend.session.dto.SessionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionServiceImplTest {

    @Mock
    private AttendanceSessionRepository attendanceSessionRepository;

    @Mock
    private ClassGroupRepository classGroupRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @InjectMocks
    private SessionServiceImpl sessionService;

    private UUID actorUserId;
    private UUID groupId;
    private UUID sessionId;

    @BeforeEach
    void setUp() {
        actorUserId = UUID.randomUUID();
        groupId = UUID.randomUUID();
        sessionId = UUID.randomUUID();
    }

    @Test
    void createSession_shouldCreateOpenSession_whenOwnerHasAccess() {
        ClassGroup group = buildGroup(groupId, actorUserId, GroupStatus.ACTIVE);
        GroupMember membership = buildMembership(groupId, actorUserId, MemberRole.OWNER, MemberStatus.APPROVED);
        CreateSessionRequest req = buildValidCreateRequest();

        AtomicReference<AttendanceSession> savedRef = new AtomicReference<>();

        when(classGroupRepository.findActiveById(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(groupId, actorUserId)).thenReturn(Optional.of(membership));
        when(attendanceSessionRepository.existsOpenByGroupId(groupId)).thenReturn(false);
        when(attendanceSessionRepository.saveAndFlush(any(AttendanceSession.class)))
                .thenAnswer(invocation -> {
                    AttendanceSession saved = invocation.getArgument(0);
                    savedRef.set(saved);
                    return saved;
                });
        when(attendanceSessionRepository.findActiveById(any(UUID.class)))
                .thenAnswer(invocation -> {
                    UUID requestedId = invocation.getArgument(0);
                    AttendanceSession saved = savedRef.get();
                    if (saved != null && requestedId.equals(saved.getId())) {
                        return Optional.of(saved);
                    }
                    return Optional.empty();
                });

        SessionResponse response = sessionService.createSession(actorUserId, groupId, req);

        ArgumentCaptor<AttendanceSession> captor = ArgumentCaptor.forClass(AttendanceSession.class);
        verify(attendanceSessionRepository).saveAndFlush(captor.capture());

        AttendanceSession saved = captor.getValue();

        assertNotNull(saved.getId());
        assertEquals(groupId, saved.getGroupId());
        assertEquals(actorUserId, saved.getCreatedByUserId());
        assertEquals("Buổi học số 1", saved.getTitle());
        assertEquals(SessionStatus.OPEN, saved.getStatus());
        assertEquals(req.getStartAt(), saved.getStartAt());
        assertEquals(req.getEndAt(), saved.getEndAt());
        assertEquals(req.getCheckinOpenAt(), saved.getCheckinOpenAt());
        assertEquals(req.getCheckinCloseAt(), saved.getCheckinCloseAt());
        assertEquals(20, saved.getTimeWindowMinutes());
        assertEquals(10, saved.getLateAfterMinutes());
        assertEquals(30, saved.getQrRotateSeconds());
        assertTrue(saved.isAllowManualOverride());
        assertEquals("Ghi chú phiên học", saved.getNote());
        assertNotNull(saved.getSessionSecret());
        assertNull(saved.getDeletedAt());

        LocalDate expectedSessionDate = LocalDate.ofInstant(req.getStartAt(), ZoneId.systemDefault());
        assertEquals(expectedSessionDate, saved.getSessionDate());

        assertNotNull(response);
        assertEquals(saved.getId(), response.id());
        assertEquals(groupId, response.groupId());
        assertEquals(actorUserId, response.createdByUserId());
        assertEquals("Buổi học số 1", response.title());
        assertEquals(SessionStatus.OPEN, response.status());
        assertEquals(req.getStartAt(), response.startAt());
        assertEquals(req.getEndAt(), response.endAt());
        assertEquals(req.getCheckinOpenAt(), response.checkinOpenAt());
        assertEquals(req.getCheckinCloseAt(), response.checkinCloseAt());
        assertEquals(20, response.timeWindowMinutes());
        assertEquals(10, response.lateAfterMinutes());
        assertEquals(30, response.qrRotateSeconds());
        assertTrue(response.allowManualOverride());
        assertEquals("Ghi chú phiên học", response.note());
    }

    @Test
    void createSession_shouldCreateOpenSession_whenCoHostHasAccess() {
        ClassGroup group = buildGroup(groupId, UUID.randomUUID(), GroupStatus.ACTIVE);
        GroupMember membership = buildMembership(groupId, actorUserId, MemberRole.CO_HOST, MemberStatus.APPROVED);
        CreateSessionRequest req = buildValidCreateRequest();

        AtomicReference<AttendanceSession> savedRef = new AtomicReference<>();

        when(classGroupRepository.findActiveById(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(groupId, actorUserId)).thenReturn(Optional.of(membership));
        when(attendanceSessionRepository.existsOpenByGroupId(groupId)).thenReturn(false);
        when(attendanceSessionRepository.saveAndFlush(any(AttendanceSession.class)))
                .thenAnswer(invocation -> {
                    AttendanceSession saved = invocation.getArgument(0);
                    savedRef.set(saved);
                    return saved;
                });
        when(attendanceSessionRepository.findActiveById(any(UUID.class)))
                .thenAnswer(invocation -> Optional.ofNullable(savedRef.get()));

        SessionResponse response = sessionService.createSession(actorUserId, groupId, req);

        assertNotNull(response);
        assertEquals(SessionStatus.OPEN, response.status());
        verify(attendanceSessionRepository).saveAndFlush(any(AttendanceSession.class));
    }

    @Test
    void createSession_shouldUseDefaults_whenOptionalPolicyFieldsAreMissing() {
        ClassGroup group = buildGroup(groupId, actorUserId, GroupStatus.ACTIVE);
        GroupMember membership = buildMembership(groupId, actorUserId, MemberRole.OWNER, MemberStatus.APPROVED);

        Instant startAt = Instant.parse("2026-04-21T01:00:00Z");

        CreateSessionRequest req = new CreateSessionRequest();
        req.setStartAt(startAt);
        req.setTitle("  ");
        req.setNote("");

        AtomicReference<AttendanceSession> savedRef = new AtomicReference<>();

        when(classGroupRepository.findActiveById(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(groupId, actorUserId)).thenReturn(Optional.of(membership));
        when(attendanceSessionRepository.existsOpenByGroupId(groupId)).thenReturn(false);
        when(attendanceSessionRepository.saveAndFlush(any(AttendanceSession.class)))
                .thenAnswer(invocation -> {
                    AttendanceSession saved = invocation.getArgument(0);
                    savedRef.set(saved);
                    return saved;
                });
        when(attendanceSessionRepository.findActiveById(any(UUID.class)))
                .thenAnswer(invocation -> Optional.ofNullable(savedRef.get()));

        SessionResponse response = sessionService.createSession(actorUserId, groupId, req);

        ArgumentCaptor<AttendanceSession> captor = ArgumentCaptor.forClass(AttendanceSession.class);
        verify(attendanceSessionRepository).saveAndFlush(captor.capture());

        AttendanceSession saved = captor.getValue();

        assertNull(saved.getTitle());
        assertNull(saved.getNote());
        assertEquals(30, saved.getTimeWindowMinutes());
        assertEquals(5, saved.getLateAfterMinutes());
        assertEquals(15, saved.getQrRotateSeconds());
        assertEquals(startAt, saved.getCheckinOpenAt());
        assertEquals(startAt.plusSeconds(30 * 60L), saved.getCheckinCloseAt());
        assertTrue(saved.isAllowManualOverride());

        assertEquals(30, response.timeWindowMinutes());
        assertEquals(5, response.lateAfterMinutes());
        assertEquals(15, response.qrRotateSeconds());
        assertNull(response.title());
        assertNull(response.note());
    }

    @Test
    void createSession_shouldThrowForbidden_whenMemberTriesToCreate() {
        ClassGroup group = buildGroup(groupId, actorUserId, GroupStatus.ACTIVE);
        GroupMember membership = buildMembership(groupId, actorUserId, MemberRole.MEMBER, MemberStatus.APPROVED);
        CreateSessionRequest req = buildValidCreateRequest();

        when(classGroupRepository.findActiveById(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(groupId, actorUserId)).thenReturn(Optional.of(membership));

        assertThrows(ApiException.class, () -> sessionService.createSession(actorUserId, groupId, req));

        verify(attendanceSessionRepository, never()).existsOpenByGroupId(any(UUID.class));
        verify(attendanceSessionRepository, never()).saveAndFlush(any(AttendanceSession.class));
    }

    @Test
    void createSession_shouldThrowConflict_whenGroupAlreadyHasOpenSession() {
        ClassGroup group = buildGroup(groupId, actorUserId, GroupStatus.ACTIVE);
        GroupMember membership = buildMembership(groupId, actorUserId, MemberRole.OWNER, MemberStatus.APPROVED);
        CreateSessionRequest req = buildValidCreateRequest();

        when(classGroupRepository.findActiveById(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(groupId, actorUserId)).thenReturn(Optional.of(membership));
        when(attendanceSessionRepository.existsOpenByGroupId(groupId)).thenReturn(true);

        assertThrows(ApiException.class, () -> sessionService.createSession(actorUserId, groupId, req));

        verify(attendanceSessionRepository, never()).saveAndFlush(any(AttendanceSession.class));
    }

    @Test
    void createSession_shouldThrowConflict_whenGroupArchived() {
        ClassGroup group = buildGroup(groupId, actorUserId, GroupStatus.ARCHIVED);
        GroupMember membership = buildMembership(groupId, actorUserId, MemberRole.OWNER, MemberStatus.APPROVED);
        CreateSessionRequest req = buildValidCreateRequest();

        when(classGroupRepository.findActiveById(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(groupId, actorUserId)).thenReturn(Optional.of(membership));

        assertThrows(ApiException.class, () -> sessionService.createSession(actorUserId, groupId, req));

        verify(attendanceSessionRepository, never()).existsOpenByGroupId(any(UUID.class));
        verify(attendanceSessionRepository, never()).saveAndFlush(any(AttendanceSession.class));
    }

    @Test
    void createSession_shouldThrowBadRequest_whenStartAtMissing() {
        ClassGroup group = buildGroup(groupId, actorUserId, GroupStatus.ACTIVE);
        GroupMember membership = buildMembership(groupId, actorUserId, MemberRole.OWNER, MemberStatus.APPROVED);

        CreateSessionRequest req = buildValidCreateRequest();
        req.setStartAt(null);

        when(classGroupRepository.findActiveById(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(groupId, actorUserId)).thenReturn(Optional.of(membership));
        when(attendanceSessionRepository.existsOpenByGroupId(groupId)).thenReturn(false);

        assertThrows(ApiException.class, () -> sessionService.createSession(actorUserId, groupId, req));

        verify(attendanceSessionRepository, never()).saveAndFlush(any(AttendanceSession.class));
    }

    @Test
    void createSession_shouldThrowBadRequest_whenStartAtIsNotBeforeEndAt() {
        ClassGroup group = buildGroup(groupId, actorUserId, GroupStatus.ACTIVE);
        GroupMember membership = buildMembership(groupId, actorUserId, MemberRole.OWNER, MemberStatus.APPROVED);

        CreateSessionRequest req = buildValidCreateRequest();
        req.setStartAt(Instant.parse("2026-04-21T03:00:00Z"));
        req.setEndAt(Instant.parse("2026-04-21T02:00:00Z"));

        when(classGroupRepository.findActiveById(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(groupId, actorUserId)).thenReturn(Optional.of(membership));
        when(attendanceSessionRepository.existsOpenByGroupId(groupId)).thenReturn(false);

        assertThrows(ApiException.class, () -> sessionService.createSession(actorUserId, groupId, req));

        verify(attendanceSessionRepository, never()).saveAndFlush(any(AttendanceSession.class));
    }

    @Test
    void createSession_shouldThrowBadRequest_whenInvalidCheckinWindow() {
        ClassGroup group = buildGroup(groupId, actorUserId, GroupStatus.ACTIVE);
        GroupMember membership = buildMembership(groupId, actorUserId, MemberRole.OWNER, MemberStatus.APPROVED);

        CreateSessionRequest req = buildValidCreateRequest();
        req.setCheckinOpenAt(Instant.parse("2026-04-21T02:30:00Z"));
        req.setCheckinCloseAt(Instant.parse("2026-04-21T02:00:00Z"));

        when(classGroupRepository.findActiveById(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(groupId, actorUserId)).thenReturn(Optional.of(membership));
        when(attendanceSessionRepository.existsOpenByGroupId(groupId)).thenReturn(false);

        assertThrows(ApiException.class, () -> sessionService.createSession(actorUserId, groupId, req));

        verify(attendanceSessionRepository, never()).saveAndFlush(any(AttendanceSession.class));
    }

    @Test
    void createSession_shouldThrowBadRequest_whenLateAfterMinutesGreaterThanTimeWindowMinutes() {
        ClassGroup group = buildGroup(groupId, actorUserId, GroupStatus.ACTIVE);
        GroupMember membership = buildMembership(groupId, actorUserId, MemberRole.OWNER, MemberStatus.APPROVED);

        CreateSessionRequest req = buildValidCreateRequest();
        req.setTimeWindowMinutes(10);
        req.setLateAfterMinutes(15);

        when(classGroupRepository.findActiveById(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(groupId, actorUserId)).thenReturn(Optional.of(membership));
        when(attendanceSessionRepository.existsOpenByGroupId(groupId)).thenReturn(false);

        assertThrows(ApiException.class, () -> sessionService.createSession(actorUserId, groupId, req));

        verify(attendanceSessionRepository, never()).saveAndFlush(any(AttendanceSession.class));
    }

    @Test
    void getOpenSession_shouldReturnOpenSession_whenApprovedMember() {
        ClassGroup group = buildGroup(groupId, actorUserId, GroupStatus.ACTIVE);
        GroupMember membership = buildMembership(groupId, actorUserId, MemberRole.MEMBER, MemberStatus.APPROVED);
        AttendanceSession session = buildSession(sessionId, groupId, actorUserId, SessionStatus.OPEN);

        when(classGroupRepository.findActiveById(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(groupId, actorUserId)).thenReturn(Optional.of(membership));
        when(attendanceSessionRepository.findOpenByGroupId(groupId)).thenReturn(Optional.of(session));

        SessionResponse response = sessionService.getOpenSession(actorUserId, groupId);

        assertNotNull(response);
        assertEquals(sessionId, response.id());
        assertEquals(groupId, response.groupId());
        assertEquals(SessionStatus.OPEN, response.status());
    }

    @Test
    void getOpenSession_shouldThrowNotFound_whenNoOpenSession() {
        ClassGroup group = buildGroup(groupId, actorUserId, GroupStatus.ACTIVE);
        GroupMember membership = buildMembership(groupId, actorUserId, MemberRole.MEMBER, MemberStatus.APPROVED);

        when(classGroupRepository.findActiveById(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(groupId, actorUserId)).thenReturn(Optional.of(membership));
        when(attendanceSessionRepository.findOpenByGroupId(groupId)).thenReturn(Optional.empty());

        assertThrows(ApiException.class, () -> sessionService.getOpenSession(actorUserId, groupId));
    }

    @Test
    void listSessions_shouldReturnPagedSessions_whenApprovedMember() {
        ClassGroup group = buildGroup(groupId, actorUserId, GroupStatus.ACTIVE);
        GroupMember membership = buildMembership(groupId, actorUserId, MemberRole.MEMBER, MemberStatus.APPROVED);

        LocalDate from = LocalDate.of(2026, 4, 1);
        LocalDate to = LocalDate.of(2026, 4, 30);

        AttendanceSession first = buildSession(UUID.randomUUID(), groupId, actorUserId, SessionStatus.CLOSED);
        AttendanceSession second = buildSession(UUID.randomUUID(), groupId, actorUserId, SessionStatus.OPEN);

        when(classGroupRepository.findActiveById(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(groupId, actorUserId)).thenReturn(Optional.of(membership));
        when(attendanceSessionRepository.findPageByGroupId(
                eq(groupId),
                eq(from),
                eq(to),
                eq(null),
                eq(PageRequest.of(0, 20))
        )).thenReturn(new PageImpl<>(List.of(first, second), PageRequest.of(0, 20), 2));

        PageSessionResponse response = sessionService.listSessions(
                actorUserId,
                groupId,
                from,
                to,
                null,
                0,
                20
        );

        assertNotNull(response);
        assertEquals(2, response.items().size());
        assertEquals(0, response.page());
        assertEquals(20, response.size());
        assertEquals(2, response.totalElements());
        assertEquals(1, response.totalPages());
    }

    @Test
    void listSessions_shouldThrowBadRequest_whenFromAfterTo() {
        ClassGroup group = buildGroup(groupId, actorUserId, GroupStatus.ACTIVE);
        GroupMember membership = buildMembership(groupId, actorUserId, MemberRole.MEMBER, MemberStatus.APPROVED);

        when(classGroupRepository.findActiveById(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(groupId, actorUserId)).thenReturn(Optional.of(membership));

        assertThrows(ApiException.class, () -> sessionService.listSessions(
                actorUserId,
                groupId,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 4, 1),
                null,
                0,
                20
        ));

        verify(attendanceSessionRepository, never()).findPageByGroupId(
                any(UUID.class),
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    void getSession_shouldReturnSession_whenApprovedMember() {
        AttendanceSession session = buildSession(sessionId, groupId, actorUserId, SessionStatus.OPEN);
        GroupMember membership = buildMembership(groupId, actorUserId, MemberRole.MEMBER, MemberStatus.APPROVED);

        when(attendanceSessionRepository.findActiveById(sessionId)).thenReturn(Optional.of(session));
        when(groupMemberRepository.findByGroupIdAndUserId(groupId, actorUserId)).thenReturn(Optional.of(membership));

        SessionResponse response = sessionService.getSession(actorUserId, sessionId);

        assertNotNull(response);
        assertEquals(sessionId, response.id());
        assertEquals(groupId, response.groupId());
        assertEquals(SessionStatus.OPEN, response.status());
    }

    @Test
    void getSession_shouldThrowForbidden_whenNotApprovedMember() {
        AttendanceSession session = buildSession(sessionId, groupId, actorUserId, SessionStatus.OPEN);
        GroupMember membership = buildMembership(groupId, actorUserId, MemberRole.MEMBER, MemberStatus.PENDING);

        when(attendanceSessionRepository.findActiveById(sessionId)).thenReturn(Optional.of(session));
        when(groupMemberRepository.findByGroupIdAndUserId(groupId, actorUserId)).thenReturn(Optional.of(membership));

        assertThrows(ApiException.class, () -> sessionService.getSession(actorUserId, sessionId));
    }

    @Test
    void closeSession_shouldCloseOpenSession_whenOwnerHasAccess() {
        AttendanceSession session = buildSession(sessionId, groupId, actorUserId, SessionStatus.OPEN);
        session.setEndAt(null);

        GroupMember membership = buildMembership(groupId, actorUserId, MemberRole.OWNER, MemberStatus.APPROVED);

        AtomicReference<AttendanceSession> savedRef = new AtomicReference<>();

        when(attendanceSessionRepository.findActiveByIdForUpdate(sessionId)).thenReturn(Optional.of(session));
        when(groupMemberRepository.findByGroupIdAndUserId(groupId, actorUserId)).thenReturn(Optional.of(membership));
        when(attendanceSessionRepository.saveAndFlush(any(AttendanceSession.class)))
                .thenAnswer(invocation -> {
                    AttendanceSession saved = invocation.getArgument(0);
                    savedRef.set(saved);
                    return saved;
                });
        when(attendanceSessionRepository.findActiveById(sessionId))
                .thenAnswer(invocation -> Optional.ofNullable(savedRef.get()));

        SessionResponse response = sessionService.closeSession(actorUserId, sessionId);

        assertEquals(SessionStatus.CLOSED, session.getStatus());
        assertNotNull(session.getEndAt());
        assertEquals(SessionStatus.CLOSED, response.status());

        verify(attendanceSessionRepository).saveAndFlush(session);
    }

    @Test
    void closeSession_shouldThrowConflict_whenSessionIsNotOpen() {
        AttendanceSession session = buildSession(sessionId, groupId, actorUserId, SessionStatus.CLOSED);
        GroupMember membership = buildMembership(groupId, actorUserId, MemberRole.OWNER, MemberStatus.APPROVED);

        when(attendanceSessionRepository.findActiveByIdForUpdate(sessionId)).thenReturn(Optional.of(session));
        when(groupMemberRepository.findByGroupIdAndUserId(groupId, actorUserId)).thenReturn(Optional.of(membership));

        assertThrows(ApiException.class, () -> sessionService.closeSession(actorUserId, sessionId));

        verify(attendanceSessionRepository, never()).saveAndFlush(any(AttendanceSession.class));
    }

    @Test
    void closeSession_shouldThrowForbidden_whenMemberTriesToClose() {
        AttendanceSession session = buildSession(sessionId, groupId, actorUserId, SessionStatus.OPEN);
        GroupMember membership = buildMembership(groupId, actorUserId, MemberRole.MEMBER, MemberStatus.APPROVED);

        when(attendanceSessionRepository.findActiveByIdForUpdate(sessionId)).thenReturn(Optional.of(session));
        when(groupMemberRepository.findByGroupIdAndUserId(groupId, actorUserId)).thenReturn(Optional.of(membership));

        assertThrows(ApiException.class, () -> sessionService.closeSession(actorUserId, sessionId));

        verify(attendanceSessionRepository, never()).saveAndFlush(any(AttendanceSession.class));
    }

    @Test
    void cancelSession_shouldCancelOpenSession_whenCoHostHasAccess() {
        AttendanceSession session = buildSession(sessionId, groupId, actorUserId, SessionStatus.OPEN);
        session.setEndAt(null);

        GroupMember membership = buildMembership(groupId, actorUserId, MemberRole.CO_HOST, MemberStatus.APPROVED);

        AtomicReference<AttendanceSession> savedRef = new AtomicReference<>();

        when(attendanceSessionRepository.findActiveByIdForUpdate(sessionId)).thenReturn(Optional.of(session));
        when(groupMemberRepository.findByGroupIdAndUserId(groupId, actorUserId)).thenReturn(Optional.of(membership));
        when(attendanceSessionRepository.saveAndFlush(any(AttendanceSession.class)))
                .thenAnswer(invocation -> {
                    AttendanceSession saved = invocation.getArgument(0);
                    savedRef.set(saved);
                    return saved;
                });
        when(attendanceSessionRepository.findActiveById(sessionId))
                .thenAnswer(invocation -> Optional.ofNullable(savedRef.get()));

        SessionResponse response = sessionService.cancelSession(actorUserId, sessionId);

        assertEquals(SessionStatus.CANCELLED, session.getStatus());
        assertNotNull(session.getEndAt());
        assertEquals(SessionStatus.CANCELLED, response.status());

        verify(attendanceSessionRepository).saveAndFlush(session);
    }

    @Test
    void cancelSession_shouldThrowConflict_whenSessionAlreadyClosed() {
        AttendanceSession session = buildSession(sessionId, groupId, actorUserId, SessionStatus.CLOSED);
        GroupMember membership = buildMembership(groupId, actorUserId, MemberRole.OWNER, MemberStatus.APPROVED);

        when(attendanceSessionRepository.findActiveByIdForUpdate(sessionId)).thenReturn(Optional.of(session));
        when(groupMemberRepository.findByGroupIdAndUserId(groupId, actorUserId)).thenReturn(Optional.of(membership));

        assertThrows(ApiException.class, () -> sessionService.cancelSession(actorUserId, sessionId));

        verify(attendanceSessionRepository, never()).saveAndFlush(any(AttendanceSession.class));
    }

    @Test
    void cancelSession_shouldThrowConflict_whenSessionAlreadyCancelled() {
        AttendanceSession session = buildSession(sessionId, groupId, actorUserId, SessionStatus.CANCELLED);
        GroupMember membership = buildMembership(groupId, actorUserId, MemberRole.OWNER, MemberStatus.APPROVED);

        when(attendanceSessionRepository.findActiveByIdForUpdate(sessionId)).thenReturn(Optional.of(session));
        when(groupMemberRepository.findByGroupIdAndUserId(groupId, actorUserId)).thenReturn(Optional.of(membership));

        assertThrows(ApiException.class, () -> sessionService.cancelSession(actorUserId, sessionId));

        verify(attendanceSessionRepository, never()).saveAndFlush(any(AttendanceSession.class));
    }

    private CreateSessionRequest buildValidCreateRequest() {
        CreateSessionRequest req = new CreateSessionRequest();
        req.setTitle("  Buổi học số 1  ");
        req.setStartAt(Instant.parse("2026-04-21T01:00:00Z"));
        req.setEndAt(Instant.parse("2026-04-21T03:00:00Z"));
        req.setCheckinOpenAt(Instant.parse("2026-04-21T01:00:00Z"));
        req.setCheckinCloseAt(Instant.parse("2026-04-21T01:20:00Z"));
        req.setTimeWindowMinutes(20);
        req.setLateAfterMinutes(10);
        req.setQrRotateSeconds(30);
        req.setAllowManualOverride(true);
        req.setNote("  Ghi chú phiên học  ");
        return req;
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

    private AttendanceSession buildSession(UUID sessionId, UUID groupId, UUID createdByUserId, SessionStatus status) {
        Instant startAt = Instant.parse("2099-04-21T01:00:00Z");
        Instant endAt = status == SessionStatus.OPEN
                ? null
                : Instant.parse("2099-04-21T03:00:00Z");
        Instant checkinOpenAt = startAt;
        Instant checkinCloseAt = startAt.plusSeconds(30 * 60L);

        AttendanceSession session = new AttendanceSession();
        session.setId(sessionId);
        session.setGroupId(groupId);
        session.setCreatedByUserId(createdByUserId);
        session.setTitle("Buổi học số 1");
        session.setSessionDate(LocalDate.ofInstant(startAt, ZoneId.systemDefault()));
        session.setStartAt(startAt);
        session.setEndAt(endAt);
        session.setCheckinOpenAt(checkinOpenAt);
        session.setCheckinCloseAt(checkinCloseAt);
        session.setStatus(status);
        session.setTimeWindowMinutes(30);
        session.setLateAfterMinutes(5);
        session.setQrRotateSeconds(15);
        session.setSessionSecret("test-session-secret");
        session.setAllowManualOverride(true);
        session.setNote("Ghi chú phiên học");
        session.setDeletedAt(null);
        return session;
    }
}