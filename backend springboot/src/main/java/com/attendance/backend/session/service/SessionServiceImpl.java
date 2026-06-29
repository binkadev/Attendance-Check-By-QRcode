package com.attendance.backend.session.service;

import com.attendance.backend.attendance.repository.AttendanceSessionRepository;
import com.attendance.backend.attendance.service.AttendancePolicyNotificationOrchestrator;
import com.attendance.backend.common.exception.ApiException;
import com.attendance.backend.domain.entity.AttendanceSession;
import com.attendance.backend.domain.entity.ClassGroup;
import com.attendance.backend.domain.entity.GroupMember;
import com.attendance.backend.domain.enums.GroupStatus;
import com.attendance.backend.domain.enums.MemberRole;
import com.attendance.backend.domain.enums.MemberStatus;
import com.attendance.backend.domain.enums.SessionStatus;
import com.attendance.backend.group.repository.ClassGroupRepository;
import com.attendance.backend.group.repository.GroupMemberRepository;
import com.attendance.backend.session.dto.CreateSessionRequest;
import com.attendance.backend.session.dto.PageSessionResponse;
import com.attendance.backend.session.dto.SessionResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class SessionServiceImpl implements SessionService {

    private static final int DEFAULT_TIME_WINDOW_MINUTES = 30;
    private static final int DEFAULT_LATE_AFTER_MINUTES = 5;
    private static final int DEFAULT_QR_ROTATE_SECONDS = 15;
    private static final int MAX_PAGE_SIZE = 200;

    private final AttendanceSessionRepository attendanceSessionRepository;
    private final ClassGroupRepository classGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final AttendancePolicyNotificationOrchestrator attendancePolicyNotificationOrchestrator;
    private final SecureRandom secureRandom = new SecureRandom();

    public SessionServiceImpl(
            AttendanceSessionRepository attendanceSessionRepository,
            ClassGroupRepository classGroupRepository,
            GroupMemberRepository groupMemberRepository,
            AttendancePolicyNotificationOrchestrator attendancePolicyNotificationOrchestrator
    ) {
        this.attendanceSessionRepository = attendanceSessionRepository;
        this.classGroupRepository = classGroupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.attendancePolicyNotificationOrchestrator = attendancePolicyNotificationOrchestrator;
    }

    @Override
    @Transactional
    public SessionResponse createSession(UUID actorUserId, UUID groupId, CreateSessionRequest req) {
        ClassGroup group = requireGroup(groupId);
        requireManageAccess(actorUserId, groupId);

        Instant now = Instant.now();
        closeExpiredOpenSessionsByGroupId(groupId, now);

        if (group.getStatus() == GroupStatus.ARCHIVED) {
            throw ApiException.conflict("GROUP_ARCHIVED", "Archived group cannot create a new session");
        }

        if (attendanceSessionRepository.existsOpenByGroupId(groupId)) {
            throw ApiException.conflict("SESSION_ALREADY_OPEN", "Group already has an open session");
        }

        validateCreateRequest(req);

        int timeWindowMinutes = valueOrDefault(req.getTimeWindowMinutes(), DEFAULT_TIME_WINDOW_MINUTES);
        int lateAfterMinutes = valueOrDefault(req.getLateAfterMinutes(), DEFAULT_LATE_AFTER_MINUTES);
        int qrRotateSeconds = valueOrDefault(req.getQrRotateSeconds(), DEFAULT_QR_ROTATE_SECONDS);

        if (lateAfterMinutes > timeWindowMinutes) {
            throw ApiException.badRequest(
                    "INVALID_SESSION_POLICY",
                    "lateAfterMinutes must be less than or equal to timeWindowMinutes"
            );
        }

        Instant checkinOpenAt = req.getCheckinOpenAt() == null
                ? req.getStartAt()
                : req.getCheckinOpenAt();

        Instant checkinCloseAt = req.getCheckinCloseAt() == null
                ? checkinOpenAt.plusSeconds(timeWindowMinutes * 60L)
                : req.getCheckinCloseAt();

        validateCheckinWindow(checkinOpenAt, checkinCloseAt);

        AttendanceSession session = new AttendanceSession();
        session.setId(UUID.randomUUID());
        session.setGroupId(groupId);
        session.setCreatedByUserId(actorUserId);
        session.setTitle(normalizeNullable(req.getTitle()));
        session.setSessionDate(LocalDate.ofInstant(req.getStartAt(), ZoneId.systemDefault()));
        session.setStartAt(req.getStartAt());
        session.setEndAt(req.getEndAt());
        session.setStatus(SessionStatus.OPEN);
        session.setTimeWindowMinutes(timeWindowMinutes);
        session.setLateAfterMinutes(lateAfterMinutes);
        session.setQrRotateSeconds(qrRotateSeconds);
        session.setSessionSecret(generateSessionSecret());
        session.setAllowManualOverride(req.getAllowManualOverride() == null || req.getAllowManualOverride());
        session.setCheckinOpenAt(checkinOpenAt);
        session.setCheckinCloseAt(checkinCloseAt);
        session.setNote(normalizeNullable(req.getNote()));
        session.setDeletedAt(null);

        try {
            attendanceSessionRepository.saveAndFlush(session);
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.conflict("SESSION_ALREADY_OPEN", "Group already has an open session");
        }

        return buildSessionResponse(session.getId());
    }

    @Override
    @Transactional
    public SessionResponse getOpenSession(UUID actorUserId, UUID groupId) {
        requireGroup(groupId);
        requireApprovedMember(actorUserId, groupId);

        Instant now = Instant.now();
        closeExpiredOpenSessionsByGroupId(groupId, now);

        AttendanceSession session = attendanceSessionRepository.findOpenByGroupId(groupId)
                .orElseThrow(() -> ApiException.notFound("SESSION_OPEN_NOT_FOUND", "Open session not found"));

        return mapToResponse(session);
    }

    @Override
    @Transactional
    public PageSessionResponse listSessions(
            UUID actorUserId,
            UUID groupId,
            LocalDate from,
            LocalDate to,
            SessionStatus status,
            int page,
            int size
    ) {
        requireGroup(groupId);
        requireApprovedMember(actorUserId, groupId);

        closeExpiredOpenSessionsByGroupId(groupId, Instant.now());

        if (from != null && to != null && from.isAfter(to)) {
            throw ApiException.badRequest("INVALID_DATE_RANGE", "from must be before or equal to to");
        }

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        Page<AttendanceSession> result = attendanceSessionRepository.findPageByGroupId(
                groupId,
                from,
                to,
                status,
                PageRequest.of(safePage, safeSize)
        );

        List<SessionResponse> items = result.getContent()
                .stream()
                .map(this::mapToResponse)
                .toList();

        return new PageSessionResponse(
                items,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Override
    @Transactional
    public SessionResponse getSession(UUID actorUserId, UUID sessionId) {
        AttendanceSession session = attendanceSessionRepository.findActiveById(sessionId)
                .orElseThrow(() -> ApiException.notFound("SESSION_NOT_FOUND", "Session not found"));

        requireApprovedMember(actorUserId, session.getGroupId());

        AttendanceSession normalized = closeExpiredSessionIfNeeded(session, Instant.now());

        return mapToResponse(normalized);
    }

    @Override
    @Transactional
    public SessionResponse closeSession(UUID actorUserId, UUID sessionId) {
        AttendanceSession session = attendanceSessionRepository.findActiveByIdForUpdate(sessionId)
                .orElseThrow(() -> ApiException.notFound("SESSION_NOT_FOUND", "Session not found"));

        requireManageAccess(actorUserId, session.getGroupId());

        if (session.getStatus() != SessionStatus.OPEN) {
            throw ApiException.conflict("SESSION_NOT_OPEN", "Only OPEN session can be closed");
        }

        session.setStatus(SessionStatus.CLOSED);

        if (session.getEndAt() == null) {
            session.setEndAt(Instant.now());
        }

        attendanceSessionRepository.saveAndFlush(session);
        attendancePolicyNotificationOrchestrator.reevaluateClosedSession(session.getGroupId(), session.getId());

        return buildSessionResponse(session.getId());
    }

    @Override
    @Transactional
    public SessionResponse cancelSession(UUID actorUserId, UUID sessionId) {
        AttendanceSession session = attendanceSessionRepository.findActiveByIdForUpdate(sessionId)
                .orElseThrow(() -> ApiException.notFound("SESSION_NOT_FOUND", "Session not found"));

        requireManageAccess(actorUserId, session.getGroupId());

        AttendanceSession normalized = closeExpiredSessionIfNeeded(session, Instant.now());

        if (normalized.getStatus() == SessionStatus.CLOSED) {
            throw ApiException.conflict("SESSION_ALREADY_CLOSED", "Closed session cannot be cancelled");
        }

        if (normalized.getStatus() == SessionStatus.CANCELLED) {
            throw ApiException.conflict("SESSION_ALREADY_CANCELLED", "Session is already cancelled");
        }

        normalized.setStatus(SessionStatus.CANCELLED);

        if (normalized.getEndAt() == null) {
            normalized.setEndAt(Instant.now());
        }

        attendanceSessionRepository.saveAndFlush(normalized);

        return buildSessionResponse(normalized.getId());
    }

    private ClassGroup requireGroup(UUID groupId) {
        return classGroupRepository.findActiveById(groupId)
                .orElseThrow(() -> ApiException.notFound("GROUP_NOT_FOUND", "Group not found"));
    }

    private GroupMember requireApprovedMember(UUID actorUserId, UUID groupId) {
        GroupMember membership = groupMemberRepository.findByGroupIdAndUserId(groupId, actorUserId)
                .orElseThrow(() -> ApiException.forbidden("FORBIDDEN", "You are not a member of this group"));

        if (membership.getMemberStatus() != MemberStatus.APPROVED) {
            throw ApiException.forbidden("FORBIDDEN", "Your group membership is not approved");
        }

        return membership;
    }

    private GroupMember requireManageAccess(UUID actorUserId, UUID groupId) {
        GroupMember membership = requireApprovedMember(actorUserId, groupId);

        boolean canManage = membership.getRole() == MemberRole.OWNER
                || membership.getRole() == MemberRole.CO_HOST;

        if (!canManage) {
            throw ApiException.forbidden("FORBIDDEN", "Only OWNER or CO_HOST can manage sessions");
        }

        return membership;
    }

    private void validateCreateRequest(CreateSessionRequest req) {
        if (req.getStartAt() == null) {
            throw ApiException.badRequest("INVALID_SESSION_START_AT", "startAt is required");
        }

        if (req.getEndAt() != null && !req.getStartAt().isBefore(req.getEndAt())) {
            throw ApiException.badRequest("INVALID_SESSION_TIME_RANGE", "startAt must be before endAt");
        }
    }

    private void validateCheckinWindow(Instant checkinOpenAt, Instant checkinCloseAt) {
        if (checkinOpenAt == null || checkinCloseAt == null) {
            return;
        }

        if (!checkinOpenAt.isBefore(checkinCloseAt)) {
            throw ApiException.badRequest(
                    "INVALID_CHECKIN_WINDOW",
                    "checkinOpenAt must be before checkinCloseAt"
            );
        }
    }

    private AttendanceSession closeExpiredSessionIfNeeded(AttendanceSession session, Instant now) {
        if (session.getStatus() != SessionStatus.OPEN) {
            return session;
        }

        if (session.getCheckinCloseAt() == null) {
            return session;
        }

        if (session.getCheckinCloseAt().isAfter(now)) {
            return session;
        }

        session.setStatus(SessionStatus.CLOSED);

        if (session.getEndAt() == null) {
            session.setEndAt(session.getCheckinCloseAt());
        }

        AttendanceSession saved = attendanceSessionRepository.saveAndFlush(session);
        attendancePolicyNotificationOrchestrator.reevaluateClosedSession(saved.getGroupId(), saved.getId());
        return saved;
    }

    private void closeExpiredOpenSessionsByGroupId(UUID groupId, Instant now) {
        List<UUID> expiredSessionIds = attendanceSessionRepository.findExpiredOpenSessionIdsByGroupId(groupId, now);
        if (expiredSessionIds == null || expiredSessionIds.isEmpty()) {
            return;
        }
        for (UUID expiredSessionId : expiredSessionIds) {
            closeExpiredOpenSession(expiredSessionId, now);
        }
    }

    public void closeExpiredOpenSession(UUID sessionId, Instant now) {
        AttendanceSession session = attendanceSessionRepository.findActiveByIdForUpdate(sessionId)
                .orElse(null);

        if (session == null || session.getStatus() != SessionStatus.OPEN) {
            return;
        }

        if (session.getCheckinCloseAt() == null || session.getCheckinCloseAt().isAfter(now)) {
            return;
        }

        session.setStatus(SessionStatus.CLOSED);

        if (session.getEndAt() == null) {
            session.setEndAt(session.getCheckinCloseAt());
        }

        AttendanceSession saved = attendanceSessionRepository.saveAndFlush(session);
        attendancePolicyNotificationOrchestrator.reevaluateClosedSession(saved.getGroupId(), saved.getId());
    }

    private int valueOrDefault(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    private String normalizeNullable(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String generateSessionSecret() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private SessionResponse buildSessionResponse(UUID sessionId) {
        AttendanceSession session = attendanceSessionRepository.findActiveById(sessionId)
                .orElseThrow(() -> ApiException.notFound("SESSION_NOT_FOUND", "Session not found after save"));

        return mapToResponse(session);
    }

    private SessionResponse mapToResponse(AttendanceSession session) {
        return new SessionResponse(
                session.getId(),
                session.getGroupId(),
                session.getCreatedByUserId(),
                session.getTitle(),
                session.getSessionDate(),
                session.getStatus(),
                session.getStartAt(),
                session.getEndAt(),
                session.getCheckinOpenAt(),
                session.getCheckinCloseAt(),
                session.getTimeWindowMinutes(),
                session.getLateAfterMinutes(),
                session.getQrRotateSeconds(),
                session.isAllowManualOverride(),
                session.getNote(),
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }
}
