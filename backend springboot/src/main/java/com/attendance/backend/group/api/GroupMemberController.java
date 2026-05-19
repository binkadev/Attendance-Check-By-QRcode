package com.attendance.backend.group.api;

import com.attendance.backend.common.exception.ApiException;
import com.attendance.backend.group.dto.ImportMembersRequest;
import com.attendance.backend.group.dto.ImportMembersResponse;
import com.attendance.backend.group.dto.JoinGroupRequest;
import com.attendance.backend.group.dto.MemberActionRequest;
import com.attendance.backend.group.dto.MemberResponse;
import com.attendance.backend.group.dto.PageMemberResponse;
import com.attendance.backend.group.service.GroupMemberService;
import com.attendance.backend.security.UserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/groups")
@Validated
public class GroupMemberController {

    private final GroupMemberService groupMemberService;

    public GroupMemberController(GroupMemberService groupMemberService) {
        this.groupMemberService = groupMemberService;
    }

    @PostMapping("/join")
    public MemberResponse joinGroup(
            @AuthenticationPrincipal UserPrincipal me,
            @Valid @RequestBody JoinGroupRequest req
    ) {
        if (me == null) {
            throw ApiException.unauthorized("UNAUTHORIZED", "Missing JWT principal");
        }
        return groupMemberService.joinByJoinCode(me.getUserId(), req);
    }

    @GetMapping("/{groupId}/members")
    public PageMemberResponse listMembers(
            @PathVariable UUID groupId,
            @AuthenticationPrincipal UserPrincipal me,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page must be >= 0") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "size must be >= 1")
            @Max(value = 200, message = "size must be <= 200") int size
    ) {
        if (me == null) {
            throw ApiException.unauthorized("UNAUTHORIZED", "Missing JWT principal");
        }
        return groupMemberService.listMembers(me.getUserId(), groupId, page, size);
    }


    @PostMapping("/{groupId}/members/import")
    public ImportMembersResponse importMembers(
            @PathVariable UUID groupId,
            @AuthenticationPrincipal UserPrincipal me,
            @Valid @RequestBody ImportMembersRequest request
    ) {
        if (me == null || me.getUserId() == null) {
            throw ApiException.unauthorized("UNAUTHORIZED", "Missing JWT principal");
        }
        return groupMemberService.importMembers(me.getUserId(), groupId, request);
    }

    @PatchMapping("/{groupId}/members/{userId}")
    public MemberResponse memberAction(
            @PathVariable UUID groupId,
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserPrincipal me,
            @Valid @RequestBody MemberActionRequest req
    ) {
        if (me == null) {
            throw ApiException.unauthorized("UNAUTHORIZED", "Missing JWT principal");
        }
        return groupMemberService.memberAction(me.getUserId(), groupId, userId, req);
    }

    @DeleteMapping("/{groupId}/members/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leaveGroup(
            @PathVariable UUID groupId,
            @AuthenticationPrincipal UserPrincipal me
    ) {
        if (me == null) {
            throw ApiException.unauthorized("UNAUTHORIZED", "Missing JWT principal");
        }
        groupMemberService.leaveGroup(me.getUserId(), groupId);
    }
}