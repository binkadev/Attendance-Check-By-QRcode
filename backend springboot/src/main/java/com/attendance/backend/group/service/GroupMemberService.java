package com.attendance.backend.group.service;

import com.attendance.backend.auth.repository.UserRepository;
import com.attendance.backend.common.exception.ApiException;
import com.attendance.backend.domain.entity.ClassGroup;
import com.attendance.backend.domain.entity.GroupMember;
import com.attendance.backend.domain.entity.User;
import com.attendance.backend.domain.enums.*;
import com.attendance.backend.group.dto.*;
import com.attendance.backend.group.exception.MemberImportValidationException;
import com.attendance.backend.group.repository.ClassGroupRepository;
import com.attendance.backend.group.repository.GroupMemberRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class GroupMemberService {

    private final ClassGroupRepository classGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private static final String DEFAULT_PASSWORD_RULE = "STUDENT_CODE_PLUS_UNACCENTED_LOWERCASE_NAME";
    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public GroupMemberService(ClassGroupRepository classGroupRepository,
                              GroupMemberRepository groupMemberRepository,
                              UserRepository userRepository,
                              PasswordEncoder passwordEncoder,
                              Clock clock) {
        this.classGroupRepository = classGroupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional
    public MemberResponse joinByJoinCode(UUID actorUserId, JoinGroupRequest req) {
        String joinCode = normalizeJoinCode(req.joinCode());
        if (!StringUtils.hasText(joinCode)) {
            throw ApiException.badRequest("JOIN_CODE_REQUIRED", "joinCode is required");
        }

        ClassGroup group = classGroupRepository.findByJoinCodeActiveForUpdate(joinCode)
                .orElseThrow(() -> ApiException.notFound("GROUP_NOT_FOUND", "Group not found"));

        if (group.getStatus() != GroupStatus.ACTIVE) {
            throw ApiException.conflict("GROUP_ARCHIVED", "Group is archived");
        }

        GroupMember existing = groupMemberRepository
                .findByGroupIdAndUserIdForUpdate(group.getId(), actorUserId)
                .orElse(null);

        Instant now = Instant.now(clock);

        if (existing == null) {
            MemberStatus initialStatus = group.getApprovalMode() == ApprovalMode.AUTO
                    ? MemberStatus.APPROVED
                    : MemberStatus.PENDING;

            GroupMember gm = GroupMember.newMember(
                    group.getId(),
                    actorUserId,
                    initialStatus,
                    initialStatus == MemberStatus.APPROVED ? now : null
            );

            try {
                GroupMember saved = saveAndReload(gm);
                return toResponse(saved, loadUsersMap(List.of(saved)));
            } catch (DataIntegrityViolationException ex) {
                GroupMember concurrent = groupMemberRepository
                        .findByGroupIdAndUserId(group.getId(), actorUserId)
                        .orElse(null);

                if (concurrent != null) {
                    throw mapExistingJoinConflict(concurrent);
                }

                throw ApiException.conflict("JOIN_CONFLICT", "Join request conflicted with another transaction");
            }
        }

        return switch (existing.getMemberStatus()) {
            case APPROVED -> throw ApiException.conflict("ALREADY_JOINED", "Already joined");
            case PENDING -> throw ApiException.conflict("JOIN_REQUEST_ALREADY_PENDING", "Join request already pending");
            case REJECTED, REMOVED -> {
                existing.setRole(MemberRole.MEMBER);

                if (group.getApprovalMode() == ApprovalMode.AUTO) {
                    existing.setMemberStatus(MemberStatus.APPROVED);
                    existing.setJoinedAt(now);
                } else {
                    existing.setMemberStatus(MemberStatus.PENDING);
                    existing.setJoinedAt(null);
                }

                existing.setRemovedAt(null);

                GroupMember saved = saveAndReload(existing);
                yield toResponse(saved, loadUsersMap(List.of(saved)));
            }
        };
    }

    @Transactional(readOnly = true)
    public PageMemberResponse listMembers(UUID actorUserId, UUID groupId, int page, int size) {
        classGroupRepository.findActiveById(groupId)
                .orElseThrow(() -> ApiException.notFound("GROUP_NOT_FOUND", "Group not found"));

        GroupMember actor = groupMemberRepository.findByGroupIdAndUserId(groupId, actorUserId)
                .orElseThrow(() -> ApiException.forbidden("FORBIDDEN", "Not allowed"));

        boolean allowed = actor.getMemberStatus() == MemberStatus.APPROVED
                && (actor.getRole() == MemberRole.OWNER || actor.getRole() == MemberRole.CO_HOST);

        if (!allowed) {
            throw ApiException.forbidden("FORBIDDEN", "Not allowed");
        }

        var rs = groupMemberRepository.findPageByGroupId(groupId, PageRequest.of(page, size));
        Map<UUID, User> usersById = loadUsersMap(rs.getContent());

        var items = rs.getContent().stream()
                .map(gm -> toResponse(gm, usersById))
                .toList();

        return new PageMemberResponse(
                items,
                rs.getNumber(),
                rs.getSize(),
                rs.getTotalElements(),
                rs.getTotalPages()
        );
    }

    @Transactional
    public MemberResponse memberAction(UUID actorUserId, UUID groupId, UUID targetUserId, MemberActionRequest req) {
        String action = normalizeAction(req.action());
        if (!StringUtils.hasText(action)) {
            throw ApiException.badRequest("ACTION_REQUIRED", "action is required");
        }

        ClassGroup group = classGroupRepository.findActiveByIdForUpdate(groupId)
                .orElseThrow(() -> ApiException.notFound("GROUP_NOT_FOUND", "Group not found"));

        GroupMember actor = groupMemberRepository.findByGroupIdAndUserIdForUpdate(groupId, actorUserId)
                .orElseThrow(() -> ApiException.forbidden("FORBIDDEN", "Not allowed"));

        GroupMember target = groupMemberRepository.findByGroupIdAndUserIdForUpdate(groupId, targetUserId)
                .orElseThrow(() -> ApiException.notFound("TARGET_MEMBER_NOT_FOUND", "Target member not found"));

        if (actorUserId.equals(targetUserId)) {
            throw ApiException.unprocessable("CANNOT_ACTION_SELF", "Cannot perform this action on yourself");
        }

        ensureActorCanManage(actor, action);

        Instant now = Instant.now(clock);

        switch (action) {
            case "APPROVE" -> {
                if (target.getMemberStatus() != MemberStatus.PENDING) {
                    throw ApiException.unprocessable("CANNOT_APPROVE_NON_PENDING_MEMBER", "Target member is not pending");
                }
                target.setMemberStatus(MemberStatus.APPROVED);
                target.setJoinedAt(now);
                target.setRemovedAt(null);
            }

            case "REJECT" -> {
                if (target.getMemberStatus() != MemberStatus.PENDING) {
                    throw ApiException.unprocessable("CANNOT_REJECT_NON_PENDING_MEMBER", "Target member is not pending");
                }
                target.setMemberStatus(MemberStatus.REJECTED);
                target.setJoinedAt(null);
                target.setRemovedAt(null);
            }

            case "REMOVE" -> {
                if (target.getRole() == MemberRole.OWNER) {
                    throw ApiException.unprocessable("CANNOT_REMOVE_OWNER", "Cannot remove owner");
                }
                if (target.getMemberStatus() == MemberStatus.REMOVED) {
                    throw ApiException.conflict("MEMBER_ALREADY_REMOVED", "Member already removed");
                }
                target.setMemberStatus(MemberStatus.REMOVED);
                target.setRemovedAt(now);
            }

            case "PROMOTE" -> {
                if (actor.getRole() != MemberRole.OWNER) {
                    throw ApiException.forbidden("FORBIDDEN", "Only owner can perform this action");
                }
                if (target.getMemberStatus() != MemberStatus.APPROVED) {
                    throw ApiException.unprocessable("TARGET_MUST_BE_APPROVED", "Target must be approved");
                }
                if (target.getRole() != MemberRole.MEMBER) {
                    throw ApiException.unprocessable("TARGET_NOT_MEMBER", "Target is not MEMBER");
                }
                target.setRole(MemberRole.CO_HOST);
            }

            case "DEMOTE" -> {
                if (actor.getRole() != MemberRole.OWNER) {
                    throw ApiException.forbidden("FORBIDDEN", "Only owner can perform this action");
                }
                if (target.getMemberStatus() != MemberStatus.APPROVED) {
                    throw ApiException.unprocessable("TARGET_MUST_BE_APPROVED", "Target must be approved");
                }
                if (target.getRole() != MemberRole.CO_HOST) {
                    throw ApiException.unprocessable("TARGET_NOT_CO_HOST", "Target is not CO_HOST");
                }
                target.setRole(MemberRole.MEMBER);
            }

            case "TRANSFER_OWNERSHIP" -> {
                if (actor.getRole() != MemberRole.OWNER) {
                    throw ApiException.forbidden("FORBIDDEN", "Only owner can perform this action");
                }
                if (target.getMemberStatus() != MemberStatus.APPROVED) {
                    throw ApiException.unprocessable("TARGET_MUST_BE_APPROVED", "Target must be approved");
                }
                if (target.getRole() == MemberRole.OWNER) {
                    throw ApiException.unprocessable("TARGET_ALREADY_OWNER", "Target is already owner");
                }

                actor.setRole(MemberRole.CO_HOST);
                target.setRole(MemberRole.OWNER);
                group.setOwnerUserId(target.getUserId());

                classGroupRepository.save(group);
                groupMemberRepository.save(actor);
            }

            default -> throw ApiException.badRequest("INVALID_MEMBER_ACTION", "Invalid member action");
        }

        GroupMember saved = saveAndReload(target);
        return toResponse(saved, loadUsersMap(List.of(saved)));
    }


    @Transactional
    public ImportMembersResponse importMembers(UUID actorUserId, UUID groupId, ImportMembersRequest request) {
        if (request == null || request.members() == null || request.members().isEmpty()) {
            throw ApiException.badRequest("MEMBER_IMPORT_EMPTY", "members must not be empty");
        }

        ClassGroup group = classGroupRepository.findActiveByIdForUpdate(groupId)
                .orElseThrow(() -> ApiException.notFound("GROUP_NOT_FOUND", "Group not found"));

        if (group.getStatus() != GroupStatus.ACTIVE) {
            throw ApiException.conflict("GROUP_ARCHIVED", "Group is archived");
        }

        GroupMember actor = groupMemberRepository.findByGroupIdAndUserIdForUpdate(groupId, actorUserId)
                .orElseThrow(() -> ApiException.forbidden("FORBIDDEN", "Not allowed"));

        ensureActorCanManage(actor, "IMPORT_MEMBERS");

        if (request.effectiveSyncMode() != MemberImportSyncMode.APPEND_ONLY) {
            throw ApiException.badRequest("MEMBER_IMPORT_SYNC_MODE_UNSUPPORTED", "Only APPEND_ONLY syncMode is currently supported");
        }

        List<NormalizedImportMember> rows = normalizeImportRows(request.members());
        validateDuplicateRows(rows);
        validateDbConflicts(rows, request.effectiveAccountProvisioningMode());

        if (request.effectiveImportMode() == MemberImportMode.VALIDATE_ONLY) {
            List<ImportMemberItemResponse> items = rows.stream()
                    .map(row -> new ImportMemberItemResponse(
                            row.rowIndex(),
                            row.studentCode(),
                            row.fullName(),
                            row.email(),
                            null,
                            null,
                            null,
                            ImportMemberAction.VALIDATED_ONLY,
                            row.email(),
                            null
                    ))
                    .toList();

            return new ImportMembersResponse(groupId, rows.size(), 0, 0, 0, 0, 0, 0, items);
        }

        int createdUsers = 0;
        int linkedExistingUsers = 0;
        int addedMembers = 0;
        int skippedExistingMembers = 0;
        int restoredMembers = 0;
        int invitationEmailsQueued = 0;
        List<ImportMemberItemResponse> items = new ArrayList<>();

        Instant now = Instant.now(clock);

        for (NormalizedImportMember row : rows) {
            User user = resolveExistingUser(row).orElse(null);
            boolean created = false;

            if (user == null) {
                if (request.effectiveAccountProvisioningMode() == AccountProvisioningMode.LINK_EXISTING_ONLY) {
                    throw new MemberImportValidationException(
                            "Danh sách import có dữ liệu không hợp lệ.",
                            List.of(new MemberImportValidationError(
                                    row.rowIndex(),
                                    "studentCode",
                                    "USER_NOT_FOUND_FOR_IMPORTED_MEMBER",
                                    "Không tìm thấy tài khoản có mã sinh viên hoặc email tương ứng."
                            ))
                    );
                }

                user = createProvisionedUser(row);
                created = true;
                createdUsers++;
            } else {
                linkedExistingUsers++;
                syncExistingUserProfileIfEmpty(user, row);
            }

            GroupMember existing = groupMemberRepository
                    .findByGroupIdAndUserIdForUpdate(groupId, user.getId())
                    .orElse(null);

            ImportMemberAction action;
            GroupMember savedMember;

            if (existing == null) {
                GroupMember gm = GroupMember.newMember(groupId, user.getId(), MemberStatus.APPROVED, now);
                gm.setInvitedBy(actorUserId);
                savedMember = saveAndReload(gm);
                addedMembers++;
                action = created
                        ? ImportMemberAction.CREATED_USER_AND_ADDED
                        : ImportMemberAction.EXISTING_USER_ADDED;
            } else if (existing.getMemberStatus() == MemberStatus.REMOVED
                    || existing.getMemberStatus() == MemberStatus.REJECTED) {
                existing.setRole(MemberRole.MEMBER);
                existing.setMemberStatus(MemberStatus.APPROVED);
                existing.setJoinedAt(now);
                existing.setInvitedBy(actorUserId);
                existing.setRemovedAt(null);
                savedMember = saveAndReload(existing);
                restoredMembers++;
                action = ImportMemberAction.REMOVED_MEMBER_RESTORED;
            } else {
                savedMember = existing;
                skippedExistingMembers++;
                action = ImportMemberAction.ALREADY_MEMBER_SKIPPED;
            }

            if (created && request.effectiveNotifyStudents()) {
                // Phase 1.5 intentionally does not send the activation email yet.
                // The account is provisioned with requirePasswordChange=true and can be communicated by class policy.
                invitationEmailsQueued += 0;
            }

            items.add(new ImportMemberItemResponse(
                    row.rowIndex(),
                    row.studentCode(),
                    row.fullName(),
                    row.email(),
                    user.getId(),
                    savedMember.getMemberStatus().name(),
                    user.getStatus().name(),
                    action,
                    user.getEmail(),
                    created ? DEFAULT_PASSWORD_RULE : null
            ));
        }

        return new ImportMembersResponse(
                groupId,
                rows.size(),
                createdUsers,
                linkedExistingUsers,
                addedMembers,
                skippedExistingMembers,
                restoredMembers,
                invitationEmailsQueued,
                items
        );
    }

    @Transactional
    public void leaveGroup(UUID actorUserId, UUID groupId) {
        classGroupRepository.findActiveById(groupId)
                .orElseThrow(() -> ApiException.notFound("GROUP_NOT_FOUND", "Group not found"));

        GroupMember actor = groupMemberRepository.findByGroupIdAndUserIdForUpdate(groupId, actorUserId)
                .orElseThrow(() -> ApiException.notFound("MEMBER_NOT_FOUND", "Member not found"));

        if (actor.getRole() == MemberRole.OWNER) {
            throw ApiException.conflict("OWNER_CANNOT_LEAVE", "Owner cannot leave group");
        }

        if (actor.getMemberStatus() == MemberStatus.REMOVED) {
            throw ApiException.conflict("MEMBER_ALREADY_REMOVED", "Member already removed");
        }

        if (actor.getMemberStatus() != MemberStatus.APPROVED) {
            throw ApiException.unprocessable("ONLY_APPROVED_MEMBER_CAN_LEAVE", "Only approved member can leave group");
        }

        actor.setMemberStatus(MemberStatus.REMOVED);
        actor.setRemovedAt(Instant.now(clock));
        groupMemberRepository.save(actor);
    }


    private List<NormalizedImportMember> normalizeImportRows(List<ImportMemberRowRequest> members) {
        List<MemberImportValidationError> errors = new ArrayList<>();
        List<NormalizedImportMember> rows = new ArrayList<>();

        for (int i = 0; i < members.size(); i++) {
            ImportMemberRowRequest raw = members.get(i);
            int rowIndex = raw.rowIndex() == null ? i + 1 : raw.rowIndex();
            String studentCode = normalizeStudentCode(raw.studentCode());
            String fullName = normalizeText(raw.fullName());
            String email = normalizeEmail(raw.email());

            if (!StringUtils.hasText(studentCode)) {
                errors.add(new MemberImportValidationError(rowIndex, "studentCode", "STUDENT_CODE_REQUIRED", "Mã sinh viên là bắt buộc."));
            }
            if (!StringUtils.hasText(fullName) || fullName.length() < 2) {
                errors.add(new MemberImportValidationError(rowIndex, "fullName", "FULL_NAME_INVALID", "Họ tên phải có ít nhất 2 ký tự."));
            }
            if (!StringUtils.hasText(email) || !email.contains("@")) {
                errors.add(new MemberImportValidationError(rowIndex, "email", "INVALID_EMAIL", "Email không đúng định dạng."));
            }

            rows.add(new NormalizedImportMember(rowIndex, studentCode, fullName, email));
        }

        throwIfImportErrors(errors);
        return rows;
    }

    private void validateDuplicateRows(List<NormalizedImportMember> rows) {
        List<MemberImportValidationError> errors = new ArrayList<>();
        Map<String, Integer> studentCodeRows = new HashMap<>();
        Map<String, Integer> emailRows = new HashMap<>();

        for (NormalizedImportMember row : rows) {
            Integer firstStudentCodeRow = studentCodeRows.putIfAbsent(row.studentCode(), row.rowIndex());
            if (firstStudentCodeRow != null) {
                errors.add(new MemberImportValidationError(
                        row.rowIndex(),
                        "studentCode",
                        "DUPLICATE_STUDENT_CODE_IN_FILE",
                        "Mã sinh viên " + row.studentCode() + " bị trùng với dòng " + firstStudentCodeRow + "."
                ));
            }

            Integer firstEmailRow = emailRows.putIfAbsent(row.email(), row.rowIndex());
            if (firstEmailRow != null) {
                errors.add(new MemberImportValidationError(
                        row.rowIndex(),
                        "email",
                        "DUPLICATE_EMAIL_IN_FILE",
                        "Email " + row.email() + " bị trùng với dòng " + firstEmailRow + "."
                ));
            }
        }

        throwIfImportErrors(errors);
    }

    private void validateDbConflicts(
            List<NormalizedImportMember> rows,
            AccountProvisioningMode accountProvisioningMode
    ) {
        List<MemberImportValidationError> errors = new ArrayList<>();

        for (NormalizedImportMember row : rows) {
            Optional<User> byStudentCode = userRepository.findByUserCodeAndDeletedAtIsNull(row.studentCode());
            Optional<User> byEmail = userRepository.findByEmailNorm(row.email());

            if (byStudentCode.isPresent()
                    && byEmail.isPresent()
                    && !byStudentCode.get().getId().equals(byEmail.get().getId())) {
                errors.add(new MemberImportValidationError(
                        row.rowIndex(),
                        "email",
                        "STUDENT_CODE_EMAIL_MATCH_DIFFERENT_USERS",
                        "Mã sinh viên và email đang thuộc hai tài khoản khác nhau."
                ));
                continue;
            }

            if (accountProvisioningMode == AccountProvisioningMode.LINK_EXISTING_ONLY
                    && byStudentCode.isEmpty()
                    && byEmail.isEmpty()) {
                errors.add(new MemberImportValidationError(
                        row.rowIndex(),
                        "studentCode",
                        "USER_NOT_FOUND_FOR_IMPORTED_MEMBER",
                        "Không tìm thấy tài khoản có mã sinh viên hoặc email tương ứng."
                ));
            }
        }

        throwIfImportErrors(errors);
    }

    private Optional<User> resolveExistingUser(NormalizedImportMember row) {
        Optional<User> byStudentCode = userRepository.findByUserCodeAndDeletedAtIsNull(row.studentCode());
        if (byStudentCode.isPresent()) {
            return byStudentCode;
        }
        return userRepository.findByEmailNorm(row.email());
    }

    private User createProvisionedUser(NormalizedImportMember row) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(row.email());
        user.setPasswordHash(passwordEncoder.encode(defaultPasswordFor(row)));
        user.setFullName(row.fullName());
        user.setUserCode(row.studentCode());
        user.setPlatformRole(PlatformRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setRequirePasswordChange(true);

        return userRepository.saveAndFlush(user);
    }

    private void syncExistingUserProfileIfEmpty(User user, NormalizedImportMember row) {
        boolean changed = false;

        if (!StringUtils.hasText(user.getUserCode())) {
            user.setUserCode(row.studentCode());
            changed = true;
        }

        if (!StringUtils.hasText(user.getFullName())) {
            user.setFullName(row.fullName());
            changed = true;
        }

        if (changed) {
            userRepository.saveAndFlush(user);
        }
    }

    private String defaultPasswordFor(NormalizedImportMember row) {
        return row.studentCode() + normalizeNameForPassword(row.fullName());
    }

    private String normalizeNameForPassword(String fullName) {
        String decomposed = Normalizer.normalize(fullName, Normalizer.Form.NFD);
        String withoutAccent = COMBINING_MARKS.matcher(decomposed).replaceAll("");
        return withoutAccent
                .replace("đ", "d")
                .replace("Đ", "D")
                .replaceAll("[^A-Za-z0-9]", "")
                .toLowerCase(Locale.ROOT);
    }

    private void throwIfImportErrors(List<MemberImportValidationError> errors) {
        if (!errors.isEmpty()) {
            throw new MemberImportValidationException(
                    "Danh sách import có dữ liệu không hợp lệ.",
                    errors
            );
        }
    }

    private void ensureActorCanManage(GroupMember actor, String action) {
        boolean approved = actor.getMemberStatus() == MemberStatus.APPROVED;
        boolean isOwner = actor.getRole() == MemberRole.OWNER;
        boolean isCoHost = actor.getRole() == MemberRole.CO_HOST;

        if (!approved || (!isOwner && !isCoHost)) {
            throw ApiException.forbidden("FORBIDDEN", "Not allowed");
        }

        if ((action.equals("PROMOTE") || action.equals("DEMOTE") || action.equals("TRANSFER_OWNERSHIP")) && !isOwner) {
            throw ApiException.forbidden("FORBIDDEN", "Only owner can perform this action");
        }
    }

    private ApiException mapExistingJoinConflict(GroupMember existing) {
        return switch (existing.getMemberStatus()) {
            case APPROVED -> ApiException.conflict("ALREADY_JOINED", "Already joined");
            case PENDING -> ApiException.conflict("JOIN_REQUEST_ALREADY_PENDING", "Join request already pending");
            case REJECTED, REMOVED -> ApiException.conflict(
                    "JOIN_STATE_CONFLICT",
                    "Membership changed concurrently, please retry"
            );
        };
    }

    private GroupMember saveAndReload(GroupMember gm) {
        var id = gm.getId();
        groupMemberRepository.saveAndFlush(gm);
        return groupMemberRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("MEMBER_NOT_FOUND", "Member not found after save"));
    }

    private Map<UUID, User> loadUsersMap(List<GroupMember> members) {
        List<UUID> userIds = members.stream()
                .map(GroupMember::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<UUID, User> usersById = new HashMap<>();
        userRepository.findAllById(userIds).forEach(user -> usersById.put(user.getId(), user));
        return usersById;
    }

    private MemberResponse toResponse(GroupMember gm, Map<UUID, User> usersById) {
        User user = usersById.get(gm.getUserId());

        return new MemberResponse(
                gm.getGroupId(),
                gm.getUserId(),
                gm.getRole().name(),
                gm.getMemberStatus().name(),
                gm.getJoinedAt(),
                gm.getInvitedBy(),
                gm.getCreatedAt(),
                gm.getUpdatedAt(),
                gm.getRemovedAt(),
                user == null ? null : user.getEmail(),
                user == null ? null : user.getFullName(),
                user == null ? null : user.getAvatarUrl()
        );
    }

    private String normalizeStudentCode(String studentCode) {
        return studentCode == null ? "" : studentCode.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private record NormalizedImportMember(
            int rowIndex,
            String studentCode,
            String fullName,
            String email
    ) {
    }

    private String normalizeJoinCode(String joinCode) {
        return joinCode == null ? "" : joinCode.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeAction(String action) {
        return action == null ? "" : action.trim().toUpperCase(Locale.ROOT);
    }
}

