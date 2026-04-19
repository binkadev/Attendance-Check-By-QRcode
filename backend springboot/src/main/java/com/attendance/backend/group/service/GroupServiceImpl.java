package com.attendance.backend.group.service;

import com.attendance.backend.common.exception.ApiException;
import com.attendance.backend.domain.entity.ClassGroup;
import com.attendance.backend.domain.entity.GroupMember;
import com.attendance.backend.domain.enums.GroupStatus;
import com.attendance.backend.domain.enums.MemberRole;
import com.attendance.backend.domain.enums.MemberStatus;
import com.attendance.backend.group.dto.CreateGroupRequest;
import com.attendance.backend.group.dto.GroupResponse;
import com.attendance.backend.group.repository.ClassGroupRepository;
import com.attendance.backend.group.repository.GroupMemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
public class GroupServiceImpl implements GroupService {

    private final ClassGroupRepository classGroupRepository;
    private final GroupMemberRepository groupMemberRepository;

    public GroupServiceImpl(
            ClassGroupRepository classGroupRepository,
            GroupMemberRepository groupMemberRepository
    ) {
        this.classGroupRepository = classGroupRepository;
        this.groupMemberRepository = groupMemberRepository;
    }

    @Override
    @Transactional
    public GroupResponse createGroup(UUID callerUserId, CreateGroupRequest req) {
        ClassGroup group = new ClassGroup();
        group.setId(UUID.randomUUID());
        group.setOwnerUserId(callerUserId);
        group.setName(req.getName().trim());
        group.setCode(req.getCode().trim());
        group.setJoinCode(resolveJoinCode(req.getJoinCode()));
        group.setDescription(req.getDescription());
        group.setSemester(req.getSemester());
        group.setRoom(req.getRoom());
        group.setApprovalMode(req.getApprovalMode());
        group.setAllowAutoJoinOnCheckin(req.getAllowAutoJoinOnCheckin());
        group.setStatus(GroupStatus.ACTIVE);

        classGroupRepository.saveAndFlush(group);

        Instant now = Instant.now();

        GroupMember ownerMember = GroupMember.newMember(
                group.getId(),
                callerUserId,
                MemberStatus.APPROVED,
                now
        );
        ownerMember.setRole(MemberRole.OWNER);
        ownerMember.setMemberStatus(MemberStatus.APPROVED);
        ownerMember.setJoinedAt(now);
        ownerMember.setRemovedAt(null);

        groupMemberRepository.saveAndFlush(ownerMember);

        groupMemberRepository.findByGroupIdAndUserId(group.getId(), callerUserId)
                .orElseThrow(() -> ApiException.conflict(
                        "OWNER_MEMBERSHIP_NOT_PERSISTED",
                        "Owner membership was not persisted after group creation"
                ));

        ClassGroup saved = classGroupRepository.findById(group.getId())
                .orElseThrow(() -> ApiException.notFound("GROUP_NOT_FOUND", "Group not found after save"));

        return new GroupResponse(
                saved.getId(),
                saved.getOwnerUserId(),
                saved.getName(),
                saved.getCode(),
                saved.getJoinCode(),
                saved.getDescription(),
                saved.getSemester(),
                saved.getRoom(),
                saved.getApprovalMode(),
                saved.isAllowAutoJoinOnCheckin(),
                saved.getStatus(),
                saved.getCreatedAt(),
                saved.getUpdatedAt()
        );
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
}