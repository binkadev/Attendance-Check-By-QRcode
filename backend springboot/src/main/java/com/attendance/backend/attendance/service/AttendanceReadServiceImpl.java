package com.attendance.backend.attendance.service;

import com.attendance.backend.attendance.dto.MyAttendanceHistoryItemResponse;
import com.attendance.backend.attendance.dto.PageMyAttendanceHistoryResponse;
import com.attendance.backend.attendance.dto.UpcomingSessionResponse;
import com.attendance.backend.attendance.repository.AttendanceReadQueryRepository;
import com.attendance.backend.attendance.support.SimpleXlsxWriter;
import com.attendance.backend.common.exception.ApiException;
import com.attendance.backend.domain.entity.GroupMember;
import com.attendance.backend.domain.enums.MemberRole;
import com.attendance.backend.domain.enums.MemberStatus;
import com.attendance.backend.group.repository.ClassGroupRepository;
import com.attendance.backend.group.repository.GroupMemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AttendanceReadServiceImpl implements AttendanceReadService {

    private static final int MAX_PAGE_SIZE = 200;
    private static final int MAX_UPCOMING_LIMIT = 100;

    private final AttendanceReadQueryRepository attendanceReadQueryRepository;
    private final ClassGroupRepository classGroupRepository;
    private final GroupMemberRepository groupMemberRepository;

    public AttendanceReadServiceImpl(
            AttendanceReadQueryRepository attendanceReadQueryRepository,
            ClassGroupRepository classGroupRepository,
            GroupMemberRepository groupMemberRepository
    ) {
        this.attendanceReadQueryRepository = attendanceReadQueryRepository;
        this.classGroupRepository = classGroupRepository;
        this.groupMemberRepository = groupMemberRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UpcomingSessionResponse> listUpcomingSessions(UUID actorUserId, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), MAX_UPCOMING_LIMIT);
        return attendanceReadQueryRepository.findUpcomingSessions(actorUserId, safeLimit);
    }

    @Override
    @Transactional(readOnly = true)
    public PageMyAttendanceHistoryResponse listMyAttendancesInGroup(
            UUID actorUserId,
            UUID groupId,
            int page,
            int size
    ) {
        requireGroupExists(groupId);
        requireApprovedMember(actorUserId, groupId);

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        List<MyAttendanceHistoryItemResponse> items =
                attendanceReadQueryRepository.findMyAttendancesInGroup(actorUserId, groupId, safePage, safeSize);

        long totalElements = attendanceReadQueryRepository.countMyAttendancesInGroup(actorUserId, groupId);
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / safeSize);

        return new PageMyAttendanceHistoryResponse(
                items,
                safePage,
                safeSize,
                totalElements,
                totalPages
        );
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportGroupAttendance(UUID actorUserId, UUID groupId) {
        requireGroupExists(groupId);
        requireManageAccess(actorUserId, groupId);

        List<MyAttendanceHistoryItemResponse> items =
                attendanceReadQueryRepository.findGroupAttendanceForExport(groupId);

        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of(
                "Group ID",
                "Group Name",
                "Session ID",
                "Session Name",
                "Session Date",
                "Start Time",
                "End Time",
                "Session Status",
                "Attendance Status",
                "Check In At",
                "Check In Method",
                "Suspicious",
                "Suspicious Reason"
        ));

        for (MyAttendanceHistoryItemResponse item : items) {
            rows.add(List.of(
                    value(item.groupId()),
                    value(item.groupName()),
                    value(item.sessionId()),
                    value(item.sessionName()),
                    value(item.sessionDate()),
                    value(item.startTime()),
                    value(item.endTime()),
                    value(item.sessionStatus()),
                    value(item.attendanceStatus()),
                    value(item.checkInAt()),
                    value(item.checkInMethod()),
                    value(item.suspiciousFlag()),
                    value(item.suspiciousReason())
            ));
        }

        return SimpleXlsxWriter.writeSheet("attendance", rows);
    }

    private void requireGroupExists(UUID groupId) {
        classGroupRepository.findActiveById(groupId)
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

    private void requireManageAccess(UUID actorUserId, UUID groupId) {
        GroupMember membership = requireApprovedMember(actorUserId, groupId);

        boolean canManage = membership.getRole() == MemberRole.OWNER
                || membership.getRole() == MemberRole.CO_HOST;

        if (!canManage) {
            throw ApiException.forbidden("FORBIDDEN", "Only OWNER or CO_HOST can export attendance");
        }
    }

    private String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}