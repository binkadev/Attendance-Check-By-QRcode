package com.attendance.backend.absence.service;

import com.attendance.backend.absence.repository.AbsenceRequestRepository;
import com.attendance.backend.attendance.repository.AttendanceEventRepository;
import com.attendance.backend.attendance.repository.AttendanceSessionRepository;
import com.attendance.backend.attendance.repository.SessionAttendanceRepository;
import com.attendance.backend.attendance.service.AttendancePolicyNotificationOrchestrator;
import com.attendance.backend.common.exception.ApiException;
import com.attendance.backend.domain.entity.AbsenceRequest;
import com.attendance.backend.domain.entity.AttendanceEvent;
import com.attendance.backend.domain.entity.AttendanceSession;
import com.attendance.backend.domain.entity.SessionAttendance;
import com.attendance.backend.domain.enums.AbsenceRequestStatus;
import com.attendance.backend.domain.enums.AttendanceStatus;
import com.attendance.backend.domain.enums.EventType;
import com.attendance.backend.domain.enums.SessionStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class AbsenceRequestService {

    public enum ReviewAction {
        APPROVE,
        REJECT
    }

    private final AbsenceRequestRepository absenceRequestRepository;
    private final AttendanceSessionRepository attendanceSessionRepository;
    private final SessionAttendanceRepository sessionAttendanceRepository;
    private final AttendanceEventRepository attendanceEventRepository;
    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final AbsenceNotificationPublisher absenceNotificationPublisher;
    private final AttendancePolicyNotificationOrchestrator attendancePolicyNotificationOrchestrator;

    public AbsenceRequestService(
            AbsenceRequestRepository absenceRequestRepository,
            AttendanceSessionRepository attendanceSessionRepository,
            SessionAttendanceRepository sessionAttendanceRepository,
            AttendanceEventRepository attendanceEventRepository,
            EntityManager entityManager,
            ObjectMapper objectMapper,
            Clock clock,
            AbsenceNotificationPublisher absenceNotificationPublisher,
            AttendancePolicyNotificationOrchestrator attendancePolicyNotificationOrchestrator
    ) {
        this.absenceRequestRepository = absenceRequestRepository;
        this.attendanceSessionRepository = attendanceSessionRepository;
        this.sessionAttendanceRepository = sessionAttendanceRepository;
        this.attendanceEventRepository = attendanceEventRepository;
        this.entityManager = entityManager;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.absenceNotificationPublisher = absenceNotificationPublisher;
        this.attendancePolicyNotificationOrchestrator = attendancePolicyNotificationOrchestrator;
    }

    @Transactional
    public AbsenceRequestView create(
            UUID groupId,
            UUID requesterUserId,
            CreateAbsenceCommand cmd
    ) {
        Instant now = Instant.now(clock);

        if (cmd.linkedSessionId() == null) {
            throw ApiException.badRequest("LINKED_SESSION_REQUIRED", "linkedSessionId is required");
        }

        ensureGroupExists(groupId);
        ensureApprovedMember(groupId, requesterUserId);

        AttendanceSession session = attendanceSessionRepository.findByIdForUpdate(cmd.linkedSessionId())
                .orElseThrow(() -> ApiException.notFound("SESSION_NOT_FOUND", "Session not found"));

        if (!groupId.equals(session.getGroupId())) {
            throw ApiException.unprocessable(
                    "SESSION_GROUP_MISMATCH",
                    "linkedSessionId does not belong to the provided groupId"
            );
        }

        if (session.getStatus() == SessionStatus.CANCELLED) {
            throw ApiException.conflict(
                    "SESSION_CANCELLED",
                    "Cannot create absence request for a cancelled session"
            );
        }

        sessionAttendanceRepository.findBySessionAndUserForUpdate(session.getId(), requesterUserId)
                .orElseThrow(() -> ApiException.notFound(
                        "ATTENDANCE_NOT_FOUND",
                        "Attendance row not found for this session/user"
                ));

        if (absenceRequestRepository.existsPendingByRequesterAndSession(requesterUserId, session.getId())) {
            throw ApiException.conflict(
                    "DUPLICATE_PENDING_ABSENCE_REQUEST",
                    "A pending absence request already exists for this session"
            );
        }

        AbsenceRequest request = new AbsenceRequest();
        request.id = UUID.randomUUID();
        request.groupId = groupId;
        request.requesterUserId = requesterUserId;
        request.linkedSessionId = session.getId();
        request.requestedDate = null;
        request.reason = cmd.reason().trim();
        request.evidenceUrl = blankToNull(cmd.evidenceUrl());
        request.requestStatus = AbsenceRequestStatus.PENDING;
        request.reviewerUserId = null;
        request.reviewerNote = null;
        request.reviewedAt = null;
        request.cancelledAt = null;
        request.revertedByUserId = null;
        request.revertedAt = null;
        request.revertNote = null;

        absenceRequestRepository.saveAndFlush(request);

        appendRequestLifecycleEvent(
                request,
                requesterUserId,
                EventType.ABSENCE_REQUEST_CREATED,
                null,
                AbsenceRequestStatus.PENDING,
                null,
                null
        );

        absenceNotificationPublisher.onCreated(request);

        return toView(request);
    }

    @Transactional(readOnly = true)
    public PageResult<AbsenceRequestView> listGroupRequests(
            UUID groupId,
            UUID actorUserId,
            AbsenceRequestStatus requestStatus,
            int page,
            int size
    ) {
        ensureGroupExists(groupId);
        ensureHostOrCoHost(groupId, actorUserId);

        PageRequest pageable = buildPage(page, size);

        Page<AbsenceRequest> rows = requestStatus == null
                ? absenceRequestRepository.findByGroupIdOrderByCreatedAtDesc(groupId, pageable)
                : absenceRequestRepository.findByGroupIdAndRequestStatusOrderByCreatedAtDesc(groupId, requestStatus, pageable);

        return new PageResult<>(
                rows.getContent().stream().map(this::toView).toList(),
                rows.getNumber(),
                rows.getSize(),
                rows.getTotalElements(),
                rows.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public PageResult<AbsenceRequestView> listMyRequests(
            UUID requesterUserId,
            AbsenceRequestStatus requestStatus,
            int page,
            int size
    ) {
        PageRequest pageable = buildPage(page, size);

        Page<AbsenceRequest> rows = requestStatus == null
                ? absenceRequestRepository.findByRequesterUserIdOrderByCreatedAtDesc(requesterUserId, pageable)
                : absenceRequestRepository.findByRequesterUserIdAndRequestStatusOrderByCreatedAtDesc(
                requesterUserId, requestStatus, pageable
        );

        return new PageResult<>(
                rows.getContent().stream().map(this::toView).toList(),
                rows.getNumber(),
                rows.getSize(),
                rows.getTotalElements(),
                rows.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public AbsenceRequestView getRequest(UUID requestId, UUID actorUserId) {
        AbsenceRequest request = absenceRequestRepository.findById(requestId)
                .orElseThrow(() -> ApiException.notFound("ABSENCE_REQUEST_NOT_FOUND", "Absence request not found"));

        if (!canView(request, actorUserId)) {
            throw ApiException.notFound("ABSENCE_REQUEST_NOT_FOUND", "Absence request not found");
        }

        return toView(request);
    }

    @Transactional
    public AbsenceRequestView review(
            UUID requestId,
            UUID actorUserId,
            ReviewAction action,
            String reviewerNote
    ) {
        Instant now = Instant.now(clock);

        AbsenceRequest request = absenceRequestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> ApiException.notFound("ABSENCE_REQUEST_NOT_FOUND", "Absence request not found"));

        ensureSessionScopedMutableRequest(request);
        ensureHostOrCoHost(request.groupId, actorUserId);

        if (action == ReviewAction.APPROVE && request.requestStatus == AbsenceRequestStatus.APPROVED) {
            return toView(request);
        }
        if (action == ReviewAction.REJECT && request.requestStatus == AbsenceRequestStatus.REJECTED) {
            return toView(request);
        }
        if (request.requestStatus != AbsenceRequestStatus.PENDING) {
            throw ApiException.conflict(
                    "ABSENCE_REQUEST_NOT_REVIEWABLE",
                    "Only PENDING absence request can be reviewed"
            );
        }

        AttendanceSession session = attendanceSessionRepository.findByIdForUpdate(request.linkedSessionId)
                .orElseThrow(() -> ApiException.notFound("SESSION_NOT_FOUND", "Session not found"));

        if (!request.groupId.equals(session.getGroupId())) {
            throw ApiException.conflict(
                    "ABSENCE_REQUEST_DATA_CORRUPTED",
                    "Absence request group does not match linked session group"
            );
        }

        request.reviewerUserId = actorUserId;
        request.reviewerNote = blankToNull(reviewerNote);
        request.reviewedAt = now;

        if (action == ReviewAction.REJECT) {
            request.requestStatus = AbsenceRequestStatus.REJECTED;
            absenceRequestRepository.saveAndFlush(request);

            appendRequestLifecycleEvent(
                    request,
                    actorUserId,
                    EventType.ABSENCE_REQUEST_REJECTED,
                    AbsenceRequestStatus.PENDING,
                    AbsenceRequestStatus.REJECTED,
                    request.reviewerNote,
                    null
            );

            absenceNotificationPublisher.onRejected(request, actorUserId);

            return toView(request);
        }

        SessionAttendance attendance = sessionAttendanceRepository.findBySessionAndUserForUpdate(
                        request.linkedSessionId,
                        request.requesterUserId
                )
                .orElseThrow(() -> ApiException.notFound(
                        "ATTENDANCE_NOT_FOUND",
                        "Attendance row not found for this session/user"
                ));

        if (attendance.attendanceStatus == AttendanceStatus.PRESENT
                || attendance.attendanceStatus == AttendanceStatus.LATE) {
            throw ApiException.conflict(
                    "ATTENDANCE_ALREADY_CHECKED_IN",
                    "Cannot approve absence request when attendance is already PRESENT/LATE"
            );
        }

        if (attendance.attendanceStatus == AttendanceStatus.EXCUSED) {
            if (request.id.equals(attendance.excusedByRequestId)) {
                request.requestStatus = AbsenceRequestStatus.APPROVED;
                absenceRequestRepository.saveAndFlush(request);

                appendRequestLifecycleEvent(
                        request,
                        actorUserId,
                        EventType.ABSENCE_REQUEST_APPROVED,
                        AbsenceRequestStatus.PENDING,
                        AbsenceRequestStatus.APPROVED,
                        request.reviewerNote,
                        null
                );

                absenceNotificationPublisher.onApproved(request, actorUserId);

                if (session.getStatus() == SessionStatus.CLOSED) {
                    attendancePolicyNotificationOrchestrator.reevaluateOne(
                            request.groupId,
                            request.requesterUserId,
                            request.linkedSessionId
                    );
                }

                return toView(request);
            }
            throw ApiException.conflict(
                    "ATTENDANCE_ALREADY_EXCUSED",
                    "Attendance is already excused by another request"
            );
        }

        if (attendance.attendanceStatus != AttendanceStatus.ABSENT) {
            throw ApiException.conflict(
                    "ATTENDANCE_STATUS_NOT_APPROVABLE",
                    "Only ABSENT attendance can be converted to EXCUSED"
            );
        }

        AttendanceStatus oldStatus = attendance.attendanceStatus;

        request.requestStatus = AbsenceRequestStatus.APPROVED;
        absenceRequestRepository.saveAndFlush(request);

        attendance.attendanceStatus = AttendanceStatus.EXCUSED;
        attendance.excusedByRequestId = request.id;
        attendance.updatedAt = now;
        sessionAttendanceRepository.saveAndFlush(attendance);

        appendRequestLifecycleEvent(
                request,
                actorUserId,
                EventType.ABSENCE_REQUEST_APPROVED,
                AbsenceRequestStatus.PENDING,
                AbsenceRequestStatus.APPROVED,
                request.reviewerNote,
                null
        );

        appendAttendanceTransitionEvent(
                request,
                actorUserId,
                EventType.MARK_EXCUSED,
                oldStatus,
                AttendanceStatus.EXCUSED,
                null
        );

        absenceNotificationPublisher.onApproved(request, actorUserId);

        if (session.getStatus() == SessionStatus.CLOSED) {
            attendancePolicyNotificationOrchestrator.reevaluateOne(
                    request.groupId,
                    request.requesterUserId,
                    request.linkedSessionId
            );
        }

        return toView(request);
    }

    @Transactional
    public AbsenceRequestView cancel(UUID requestId, UUID requesterUserId) {
        Instant now = Instant.now(clock);

        AbsenceRequest request = absenceRequestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> ApiException.notFound("ABSENCE_REQUEST_NOT_FOUND", "Absence request not found"));

        ensureSessionScopedMutableRequest(request);

        if (!request.requesterUserId.equals(requesterUserId)) {
            throw ApiException.notFound("ABSENCE_REQUEST_NOT_FOUND", "Absence request not found");
        }

        if (request.requestStatus == AbsenceRequestStatus.CANCELLED) {
            return toView(request);
        }

        if (request.requestStatus != AbsenceRequestStatus.PENDING) {
            throw ApiException.conflict(
                    "ABSENCE_REQUEST_NOT_CANCELLABLE",
                    "Only PENDING absence request can be cancelled"
            );
        }

        request.requestStatus = AbsenceRequestStatus.CANCELLED;
        request.cancelledAt = now;
        absenceRequestRepository.saveAndFlush(request);

        appendRequestLifecycleEvent(
                request,
                requesterUserId,
                EventType.ABSENCE_REQUEST_CANCELLED,
                AbsenceRequestStatus.PENDING,
                AbsenceRequestStatus.CANCELLED,
                null,
                null
        );

        absenceNotificationPublisher.onCancelled(request);

        return toView(request);
    }

    @Transactional
    public AbsenceRequestView revert(UUID requestId, UUID actorUserId, String revertNote) {
        Instant now = Instant.now(clock);

        AbsenceRequest request = absenceRequestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> ApiException.notFound("ABSENCE_REQUEST_NOT_FOUND", "Absence request not found"));

        ensureSessionScopedMutableRequest(request);
        ensureHostOrCoHost(request.groupId, actorUserId);

        if (request.requestStatus == AbsenceRequestStatus.REVERTED) {
            return toView(request);
        }

        if (request.requestStatus != AbsenceRequestStatus.APPROVED) {
            throw ApiException.conflict(
                    "ABSENCE_REQUEST_NOT_REVERTABLE",
                    "Only APPROVED absence request can be reverted"
            );
        }

        AttendanceSession session = attendanceSessionRepository.findByIdForUpdate(request.linkedSessionId)
                .orElseThrow(() -> ApiException.notFound("SESSION_NOT_FOUND", "Session not found"));

        if (!request.groupId.equals(session.getGroupId())) {
            throw ApiException.conflict(
                    "ABSENCE_REQUEST_DATA_CORRUPTED",
                    "Absence request group does not match linked session group"
            );
        }

        SessionAttendance attendance = sessionAttendanceRepository.findBySessionAndUserForUpdate(
                        request.linkedSessionId,
                        request.requesterUserId
                )
                .orElseThrow(() -> ApiException.notFound(
                        "ATTENDANCE_NOT_FOUND",
                        "Attendance row not found for this session/user"
                ));

        if (attendance.attendanceStatus != AttendanceStatus.EXCUSED
                || attendance.excusedByRequestId == null
                || !request.id.equals(attendance.excusedByRequestId)) {
            throw ApiException.conflict(
                    "EXCUSED_STATE_MISMATCH",
                    "Attendance is not currently excused by this request"
            );
        }

        request.requestStatus = AbsenceRequestStatus.REVERTED;
        request.revertedByUserId = actorUserId;
        request.revertedAt = now;
        request.revertNote = blankToNull(revertNote);
        absenceRequestRepository.saveAndFlush(request);

        attendance.attendanceStatus = AttendanceStatus.ABSENT;
        attendance.excusedByRequestId = null;
        attendance.updatedAt = now;
        sessionAttendanceRepository.saveAndFlush(attendance);

        appendRequestLifecycleEvent(
                request,
                actorUserId,
                EventType.ABSENCE_REQUEST_REVERTED,
                AbsenceRequestStatus.APPROVED,
                AbsenceRequestStatus.REVERTED,
                null,
                request.revertNote
        );

        appendAttendanceTransitionEvent(
                request,
                actorUserId,
                EventType.REVERT_FROM_EXCUSED,
                AttendanceStatus.EXCUSED,
                AttendanceStatus.ABSENT,
                request.revertNote
        );

        absenceNotificationPublisher.onReverted(request, actorUserId);

        if (session.getStatus() == SessionStatus.CLOSED) {
            attendancePolicyNotificationOrchestrator.reevaluateOne(
                    request.groupId,
                    request.requesterUserId,
                    request.linkedSessionId
            );
        }

        return toView(request);
    }

    private void ensureSessionScopedMutableRequest(AbsenceRequest request) {
        if (request.linkedSessionId == null) {
            throw ApiException.unprocessable(
                    "LEGACY_DATE_BASED_REQUEST_UNSUPPORTED",
                    "Legacy requestedDate absence requests are read-only in Ticket 6.4b"
            );
        }
    }

    private boolean canView(AbsenceRequest request, UUID actorUserId) {
        return request.requesterUserId.equals(actorUserId) || isHostOrCoHost(request.groupId, actorUserId);
    }

    private void ensureGroupExists(UUID groupId) {
        Number cnt = (Number) entityManager.createNativeQuery("""
            select count(*)
            from class_groups g
            where g.id = UUID_TO_BIN(:groupId, 1)
              and g.deleted_at is null
        """)
                .setParameter("groupId", groupId.toString())
                .getSingleResult();

        if (cnt == null || cnt.longValue() == 0L) {
            throw ApiException.notFound("GROUP_NOT_FOUND", "Group not found");
        }
    }

    private void ensureApprovedMember(UUID groupId, UUID actorUserId) {
        Number cnt = (Number) entityManager.createNativeQuery("""
            select count(*)
            from group_members gm
            where gm.group_id = UUID_TO_BIN(:groupId, 1)
              and gm.user_id  = UUID_TO_BIN(:userId, 1)
              and gm.member_status = 'APPROVED'
        """)
                .setParameter("groupId", groupId.toString())
                .setParameter("userId", actorUserId.toString())
                .getSingleResult();

        if (cnt == null || cnt.longValue() == 0L) {
            throw ApiException.forbidden("FORBIDDEN", "Only APPROVED group member can create absence request");
        }
    }

    private void ensureHostOrCoHost(UUID groupId, UUID actorUserId) {
        if (!isHostOrCoHost(groupId, actorUserId)) {
            throw ApiException.forbidden("FORBIDDEN", "Only OWNER/CO_HOST can perform this action");
        }
    }

    private boolean isHostOrCoHost(UUID groupId, UUID actorUserId) {
        Number cnt = (Number) entityManager.createNativeQuery("""
            select count(*)
            from group_members gm
            where gm.group_id = UUID_TO_BIN(:groupId, 1)
              and gm.user_id  = UUID_TO_BIN(:userId, 1)
              and gm.member_status = 'APPROVED'
              and gm.role in ('OWNER','CO_HOST')
        """)
                .setParameter("groupId", groupId.toString())
                .setParameter("userId", actorUserId.toString())
                .getSingleResult();

        return cnt != null && cnt.longValue() > 0L;
    }

    private PageRequest buildPage(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 200));
        return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private AbsenceRequestView toView(AbsenceRequest request) {
        return new AbsenceRequestView(
                request.id,
                request.groupId,
                request.requesterUserId,
                request.linkedSessionId,
                request.requestedDate,
                request.reason,
                request.evidenceUrl,
                request.requestStatus,
                request.reviewerUserId,
                request.reviewerNote,
                request.reviewedAt,
                request.cancelledAt,
                request.revertedByUserId,
                request.revertedAt,
                request.revertNote,
                request.createdAt,
                request.updatedAt
        );
    }

    private void appendRequestLifecycleEvent(
            AbsenceRequest request,
            UUID actorUserId,
            EventType eventType,
            AbsenceRequestStatus oldRequestStatus,
            AbsenceRequestStatus newRequestStatus,
            String reviewerNote,
            String revertNote
    ) {
        AttendanceEvent event = new AttendanceEvent();
        event.id = UUID.randomUUID();
        event.sessionId = request.linkedSessionId;
        event.userId = request.requesterUserId;
        event.actorUserId = actorUserId;
        event.eventType = eventType;
        event.oldStatus = null;
        event.newStatus = null;
        event.qrTokenId = null;

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("requestId", request.id.toString());
        payload.put("groupId", request.groupId.toString());
        payload.put("requesterUserId", request.requesterUserId.toString());
        payload.put("linkedSessionId", request.linkedSessionId.toString());
        payload.put("reason", request.reason);
        putIfNotBlank(payload, "evidenceUrl", request.evidenceUrl);
        if (oldRequestStatus != null) {
            payload.put("oldRequestStatus", oldRequestStatus.name());
        }
        if (newRequestStatus != null) {
            payload.put("newRequestStatus", newRequestStatus.name());
        }
        putIfNotBlank(payload, "reviewerNote", reviewerNote);
        putIfNotBlank(payload, "revertNote", revertNote);
        payload.put("actorUserId", actorUserId.toString());

        event.eventPayload = payload;
        attendanceEventRepository.saveAndFlush(event);
    }

    private void appendAttendanceTransitionEvent(
            AbsenceRequest request,
            UUID actorUserId,
            EventType eventType,
            AttendanceStatus oldStatus,
            AttendanceStatus newStatus,
            String note
    ) {
        AttendanceEvent event = new AttendanceEvent();
        event.id = UUID.randomUUID();
        event.sessionId = request.linkedSessionId;
        event.userId = request.requesterUserId;
        event.actorUserId = actorUserId;
        event.eventType = eventType;
        event.oldStatus = oldStatus;
        event.newStatus = newStatus;
        event.qrTokenId = null;

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("requestId", request.id.toString());
        payload.put("groupId", request.groupId.toString());
        payload.put("requesterUserId", request.requesterUserId.toString());
        payload.put("linkedSessionId", request.linkedSessionId.toString());
        payload.put("actorUserId", actorUserId.toString());
        putIfNotBlank(payload, "note", note);

        event.eventPayload = payload;
        attendanceEventRepository.saveAndFlush(event);
    }

    private void putIfNotBlank(ObjectNode payload, String key, String value) {
        if (value != null && !value.isBlank()) {
            payload.put(key, value);
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record CreateAbsenceCommand(
            UUID linkedSessionId,
            String reason,
            String evidenceUrl
    ) {
    }

    public record AbsenceRequestView(
            UUID id,
            UUID groupId,
            UUID requesterUserId,
            UUID linkedSessionId,
            LocalDate requestedDate,
            String reason,
            String evidenceUrl,
            AbsenceRequestStatus requestStatus,
            UUID reviewerUserId,
            String reviewerNote,
            Instant reviewedAt,
            Instant cancelledAt,
            UUID revertedByUserId,
            Instant revertedAt,
            String revertNote,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record PageResult<T>(
            List<T> items,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
    }
}