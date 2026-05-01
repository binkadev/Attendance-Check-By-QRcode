package com.attendance.backend.group.service;

import com.attendance.backend.common.exception.ApiException;
import com.attendance.backend.domain.entity.ClassGroup;
import com.attendance.backend.domain.entity.GroupMember;
import com.attendance.backend.domain.entity.GroupWeeklySchedule;
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
import com.attendance.backend.group.service.schedule.GroupSchedulePlanner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class GroupServiceImpl implements GroupService {

    private final ClassGroupRepository classGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupWeeklyScheduleRepository groupWeeklyScheduleRepository;
    private final GroupSchedulePlanner groupSchedulePlanner;

    public GroupServiceImpl(
            ClassGroupRepository classGroupRepository,
            GroupMemberRepository groupMemberRepository,
            GroupWeeklyScheduleRepository groupWeeklyScheduleRepository,
            GroupSchedulePlanner groupSchedulePlanner
    ) {
        this.classGroupRepository = classGroupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.groupWeeklyScheduleRepository = groupWeeklyScheduleRepository;
        this.groupSchedulePlanner = groupSchedulePlanner;
    }

    @Override
    @Transactional
    public GroupResponse createGroup(UUID callerUserId, CreateGroupRequest req) {
        String normalizedCampus = normalizeNullable(req.getCampus());
        String normalizedRoom = normalizeNullable(req.getRoom());

        LocalDate plannedEndDate = calculateAndValidatePlannedEndDate(
                req.getStartDate(),
                req.getWeeklySchedules(),
                req.getTotalSessions(),
                req.getMaxAllowedAbsences(),
                normalizedCampus,
                normalizedRoom
        );

        ClassGroup group = new ClassGroup();
        group.setId(UUID.randomUUID());
        group.setOwnerUserId(callerUserId);
        group.setName(req.getName().trim());
        group.setCode(req.getCode().trim());
        group.setCourseCode(normalizeNullable(req.getCourseCode()));
        group.setClassCode(normalizeNullable(req.getClassCode()));
        group.setJoinCode(resolveJoinCode(req.getJoinCode()));
        group.setDescription(normalizeNullable(req.getDescription()));
        group.setSemester(normalizeNullable(req.getSemester()));
        group.setAcademicYear(normalizeNullable(req.getAcademicYear()));
        group.setCampus(normalizedCampus);
        group.setRoom(normalizedRoom);
        group.setStartDate(req.getStartDate());
        group.setPlannedEndDate(plannedEndDate);
        group.setTotalSessions(req.getTotalSessions());
        group.setMaxAllowedAbsences(req.getMaxAllowedAbsences());
        group.setApprovalMode(req.getApprovalMode());
        group.setAllowAutoJoinOnCheckin(req.getAllowAutoJoinOnCheckin());
        group.setStatus(GroupStatus.ACTIVE);

        classGroupRepository.saveAndFlush(group);

        GroupMember ownerMember = new GroupMember();
        ownerMember.setGroupId(group.getId());
        ownerMember.setUserId(callerUserId);
        ownerMember.setRole(MemberRole.OWNER);
        ownerMember.setMemberStatus(MemberStatus.APPROVED);
        ownerMember.setJoinedAt(Instant.now());

        groupMemberRepository.saveAndFlush(ownerMember);

        saveSchedules(group.getId(), req.getWeeklySchedules());

        return buildGroupResponse(group.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public GroupResponse getGroupDetail(UUID callerUserId, UUID groupId) {
        ClassGroup group = classGroupRepository.findActiveById(groupId)
                .orElseThrow(() -> ApiException.notFound("GROUP_NOT_FOUND", "Group not found"));

        requireReadAccess(callerUserId, groupId);

        return mapToResponse(group, groupWeeklyScheduleRepository.findByGroupIdOrderByDayOfWeekAscStartTimeAsc(groupId));
    }

    @Override
    @Transactional
    public GroupResponse updateGroup(UUID callerUserId, UUID groupId, UpdateGroupRequest req) {
        ClassGroup group = classGroupRepository.findActiveByIdForUpdate(groupId)
                .orElseThrow(() -> ApiException.notFound("GROUP_NOT_FOUND", "Group not found"));

        requireManageAccess(callerUserId, groupId);

        List<GroupWeeklySchedule> existingSchedules =
                groupWeeklyScheduleRepository.findByGroupIdOrderByDayOfWeekAscStartTimeAsc(groupId);

        List<GroupWeeklyScheduleRequest> effectiveSchedules =
                req.getWeeklySchedules() != null ? req.getWeeklySchedules() : toScheduleRequests(existingSchedules);

        Integer effectiveTotalSessions =
                req.getTotalSessions() != null ? req.getTotalSessions() : group.getTotalSessions();

        Integer effectiveMaxAllowedAbsences =
                req.getMaxAllowedAbsences() != null ? req.getMaxAllowedAbsences() : group.getMaxAllowedAbsences();

        LocalDate effectiveStartDate =
                req.getStartDate() != null ? req.getStartDate() : group.getStartDate();

        String effectiveCampus =
                req.getCampus() != null ? normalizeNullable(req.getCampus()) : group.getCampus();

        String effectiveRoom =
                req.getRoom() != null ? normalizeNullable(req.getRoom()) : group.getRoom();

        boolean scheduleFieldsTouched = req.getStartDate() != null
                || req.getWeeklySchedules() != null
                || req.getTotalSessions() != null
                || req.getMaxAllowedAbsences() != null
                || req.getCampus() != null
                || req.getRoom() != null;

        LocalDate effectivePlannedEndDate = group.getPlannedEndDate();
        if (effectiveStartDate != null) {
            effectivePlannedEndDate = calculateAndValidatePlannedEndDate(
                    effectiveStartDate,
                    effectiveSchedules,
                    effectiveTotalSessions,
                    effectiveMaxAllowedAbsences,
                    effectiveCampus,
                    effectiveRoom
            );
        } else if (scheduleFieldsTouched) {
            throw ApiException.unprocessable("START_DATE_REQUIRED", "startDate is required before updating schedule fields");
        }

        if (req.getName() != null) {
            group.setName(normalizeRequired(req.getName(), "name"));
        }
        if (req.getCourseCode() != null) {
            group.setCourseCode(normalizeNullable(req.getCourseCode()));
        }
        if (req.getClassCode() != null) {
            group.setClassCode(normalizeNullable(req.getClassCode()));
        }
        if (req.getJoinCode() != null) {
            group.setJoinCode(resolveJoinCode(req.getJoinCode()));
        }
        if (req.getDescription() != null) {
            group.setDescription(normalizeNullable(req.getDescription()));
        }
        if (req.getSemester() != null) {
            group.setSemester(normalizeNullable(req.getSemester()));
        }
        if (req.getAcademicYear() != null) {
            group.setAcademicYear(normalizeNullable(req.getAcademicYear()));
        }
        if (req.getCampus() != null) {
            group.setCampus(effectiveCampus);
        }
        if (req.getRoom() != null) {
            group.setRoom(effectiveRoom);
        }
        if (req.getStartDate() != null) {
            group.setStartDate(req.getStartDate());
        }
        if (effectivePlannedEndDate != null) {
            group.setPlannedEndDate(effectivePlannedEndDate);
        }
        if (req.getApprovalMode() != null) {
            group.setApprovalMode(req.getApprovalMode());
        }
        if (req.getAllowAutoJoinOnCheckin() != null) {
            group.setAllowAutoJoinOnCheckin(req.getAllowAutoJoinOnCheckin());
        }
        if (req.getTotalSessions() != null) {
            group.setTotalSessions(req.getTotalSessions());
        }
        if (req.getMaxAllowedAbsences() != null) {
            group.setMaxAllowedAbsences(req.getMaxAllowedAbsences());
        }

        classGroupRepository.saveAndFlush(group);

        if (req.getWeeklySchedules() != null) {
            groupWeeklyScheduleRepository.deleteByGroupId(groupId);
            groupWeeklyScheduleRepository.flush();
            saveSchedules(groupId, req.getWeeklySchedules());
        }

        return buildGroupResponse(groupId);
    }

    @Override
    @Transactional
    public GroupResponse updateGroupStatus(UUID callerUserId, UUID groupId, UpdateGroupStatusRequest req) {
        if (req.getStatus() == null) {
            throw ApiException.badRequest("INVALID_GROUP_STATUS", "status is required");
        }

        if (req.getStatus() != GroupStatus.ACTIVE && req.getStatus() != GroupStatus.ARCHIVED) {
            throw ApiException.badRequest("INVALID_GROUP_STATUS", "status must be ACTIVE or ARCHIVED");
        }

        ClassGroup group = classGroupRepository.findByIdAndDeletedAtIsNullForUpdate(groupId)
                .orElseThrow(() -> ApiException.notFound("GROUP_NOT_FOUND", "Group not found"));

        requireOwnerStatusAccess(callerUserId, groupId);

        group.setStatus(req.getStatus());
        classGroupRepository.saveAndFlush(group);

        List<GroupWeeklySchedule> schedules =
                groupWeeklyScheduleRepository.findByGroupIdOrderByDayOfWeekAscStartTimeAsc(groupId);

        return mapToResponse(group, schedules);
    }

    @Override
    @Transactional
    public GroupResponse archiveGroup(UUID callerUserId, UUID groupId) {
        UpdateGroupStatusRequest req = new UpdateGroupStatusRequest();
        req.setStatus(GroupStatus.ARCHIVED);
        return updateGroupStatus(callerUserId, groupId, req);
    }

    private void requireReadAccess(UUID callerUserId, UUID groupId) {
        GroupMember membership = groupMemberRepository.findByGroupIdAndUserId(groupId, callerUserId)
                .orElseThrow(() -> ApiException.forbidden("FORBIDDEN", "You do not have permission to access this group"));

        if (membership.getMemberStatus() != MemberStatus.APPROVED) {
            throw ApiException.forbidden("FORBIDDEN", "You do not have permission to access this group");
        }
    }

    private void requireManageAccess(UUID callerUserId, UUID groupId) {
        GroupMember membership = groupMemberRepository.findByGroupIdAndUserId(groupId, callerUserId)
                .orElseThrow(() -> ApiException.forbidden("FORBIDDEN", "You do not have permission to access this group"));

        if (membership.getMemberStatus() != MemberStatus.APPROVED) {
            throw ApiException.forbidden("FORBIDDEN", "You do not have permission to access this group");
        }

        if (membership.getRole() != MemberRole.OWNER && membership.getRole() != MemberRole.CO_HOST) {
            throw ApiException.forbidden("FORBIDDEN", "You do not have permission to access this group");
        }
    }

    private void requireOwnerStatusAccess(UUID callerUserId, UUID groupId) {
        GroupMember membership = groupMemberRepository.findByGroupIdAndUserId(groupId, callerUserId)
                .orElseThrow(() -> ApiException.forbidden("FORBIDDEN", "You do not have permission to change group status"));

        if (membership.getMemberStatus() != MemberStatus.APPROVED) {
            throw ApiException.forbidden("FORBIDDEN", "You do not have permission to change group status");
        }

        if (membership.getRole() != MemberRole.OWNER) {
            throw ApiException.forbidden("FORBIDDEN", "Only owner can change group status");
        }
    }

    private LocalDate calculateAndValidatePlannedEndDate(
            LocalDate startDate,
            List<GroupWeeklyScheduleRequest> weeklySchedules,
            Integer totalSessions,
            Integer maxAllowedAbsences,
            String campus,
            String room
    ) {
        validateCreateOrUpdateStep2(
                weeklySchedules,
                totalSessions,
                maxAllowedAbsences
        );

        return groupSchedulePlanner.calculatePlannedEndDate(
                startDate,
                weeklySchedules,
                totalSessions,
                campus,
                room
        );
    }

    private void validateCreateOrUpdateStep2(
            List<GroupWeeklyScheduleRequest> weeklySchedules,
            Integer totalSessions,
            Integer maxAllowedAbsences
    ) {
        if (weeklySchedules == null || weeklySchedules.isEmpty()) {
            throw ApiException.unprocessable("WEEKLY_SCHEDULE_REQUIRED", "weeklySchedules must not be empty");
        }

        if (totalSessions == null || totalSessions <= 0) {
            throw ApiException.unprocessable("TOTAL_SESSIONS_INVALID", "totalSessions must be greater than 0");
        }

        if (maxAllowedAbsences == null || maxAllowedAbsences < 0) {
            throw ApiException.unprocessable(
                    "INVALID_ABSENCE_POLICY",
                    "maxAllowedAbsences must be greater than or equal to 0"
            );
        }

        if (maxAllowedAbsences > totalSessions) {
            throw ApiException.unprocessable(
                    "INVALID_ABSENCE_POLICY",
                    "maxAllowedAbsences must be less than or equal to totalSessions"
            );
        }

        Set<String> seen = new HashSet<>();

        for (GroupWeeklyScheduleRequest item : weeklySchedules) {
            if (item == null) {
                throw ApiException.unprocessable("INVALID_WEEKLY_SCHEDULE", "weeklySchedules contains invalid item");
            }

            String normalizedDayOfWeek = normalizeDayOfWeek(item.getDayOfWeek());
            LocalTime startTime = LocalTime.parse(item.getStartTime());
            LocalTime endTime = LocalTime.parse(item.getEndTime());

            if (!startTime.isBefore(endTime)) {
                throw ApiException.unprocessable(
                        "INVALID_WEEKLY_SCHEDULE_TIME",
                        "startTime must be earlier than endTime"
                );
            }

            String dedupKey = normalizedDayOfWeek + "|" + startTime + "|" + endTime;
            if (!seen.add(dedupKey)) {
                throw ApiException.unprocessable(
                        "DUPLICATE_WEEKLY_SCHEDULE",
                        "weeklySchedules contains duplicate entries"
                );
            }
        }
    }

    private void saveSchedules(UUID groupId, List<GroupWeeklyScheduleRequest> scheduleRequests) {
        List<GroupWeeklySchedule> schedules = scheduleRequests.stream()
                .map(item -> {
                    GroupWeeklySchedule schedule = new GroupWeeklySchedule();
                    schedule.setId(UUID.randomUUID());
                    schedule.setGroupId(groupId);
                    schedule.setDayOfWeek(normalizeDayOfWeek(item.getDayOfWeek()));
                    schedule.setStartTime(LocalTime.parse(item.getStartTime()));
                    schedule.setEndTime(LocalTime.parse(item.getEndTime()));
                    return schedule;
                })
                .toList();

        groupWeeklyScheduleRepository.saveAll(schedules);
        groupWeeklyScheduleRepository.flush();
    }

    private GroupResponse buildGroupResponse(UUID groupId) {
        ClassGroup saved = classGroupRepository.findActiveById(groupId)
                .orElseThrow(() -> ApiException.notFound("GROUP_NOT_FOUND", "Group not found"));

        List<GroupWeeklySchedule> schedules =
                groupWeeklyScheduleRepository.findByGroupIdOrderByDayOfWeekAscStartTimeAsc(groupId);

        return mapToResponse(saved, schedules);
    }

    private GroupResponse mapToResponse(ClassGroup group, List<GroupWeeklySchedule> schedules) {
        List<GroupWeeklyScheduleResponse> weeklyScheduleResponses = schedules.stream()
                .map(item -> new GroupWeeklyScheduleResponse(
                        item.getDayOfWeek(),
                        item.getStartTime().toString(),
                        item.getEndTime().toString()
                ))
                .toList();

        String lecturerName = groupMemberRepository.findUserFullNameById(group.getOwnerUserId())
                .orElse(null);

        long studentCount = groupMemberRepository.countByGroupIdAndRoleAndMemberStatus(
                group.getId(),
                MemberRole.MEMBER,
                MemberStatus.APPROVED
        );

        return new GroupResponse(
                group.getId(),
                group.getOwnerUserId(),
                group.getName(),
                group.getCode(),
                group.getCourseCode(),
                group.getClassCode(),
                group.getJoinCode(),
                group.getDescription(),
                group.getSemester(),
                group.getAcademicYear(),
                group.getCampus(),
                group.getRoom(),
                group.getStartDate(),
                group.getPlannedEndDate(),
                lecturerName,
                studentCount,
                group.getTotalSessions(),
                group.getMaxAllowedAbsences(),
                weeklyScheduleResponses,
                group.getApprovalMode(),
                group.isAllowAutoJoinOnCheckin(),
                group.getStatus(),
                group.getCreatedAt(),
                group.getUpdatedAt()
        );
    }

    private List<GroupWeeklyScheduleRequest> toScheduleRequests(List<GroupWeeklySchedule> schedules) {
        return schedules.stream()
                .map(item -> {
                    GroupWeeklyScheduleRequest req = new GroupWeeklyScheduleRequest();
                    req.setDayOfWeek(item.getDayOfWeek());
                    req.setStartTime(item.getStartTime().toString());
                    req.setEndTime(item.getEndTime().toString());
                    return req;
                })
                .toList();
    }

    private String resolveJoinCode(String joinCode) {
        if (StringUtils.hasText(joinCode)) {
            return joinCode.trim().toUpperCase(Locale.ROOT);
        }

        return UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 8)
                .toUpperCase(Locale.ROOT);
    }

    private String normalizeNullable(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String normalizeRequired(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw ApiException.badRequest("INVALID_" + fieldName.toUpperCase(Locale.ROOT), fieldName + " must not be blank");
        }
        return value.trim();
    }

    private String normalizeDayOfWeek(String value) {
        if (!StringUtils.hasText(value)) {
            throw ApiException.unprocessable("INVALID_DAY_OF_WEEK", "dayOfWeek is required");
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
