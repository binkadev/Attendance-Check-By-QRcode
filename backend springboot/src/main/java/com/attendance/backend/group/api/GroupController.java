package com.attendance.backend.group.api;

import com.attendance.backend.common.exception.ApiException;
import com.attendance.backend.group.dto.CreateGroupRequest;
import com.attendance.backend.group.dto.GroupResponse;
import com.attendance.backend.group.dto.GroupScheduleConflictResponse;
import com.attendance.backend.group.dto.UpdateGroupRequest;
import com.attendance.backend.group.dto.UpdateGroupStatusRequest;
import com.attendance.backend.group.dto.ValidateGroupScheduleRequest;
import com.attendance.backend.group.service.GroupService;
import com.attendance.backend.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GroupResponse createGroup(
            @AuthenticationPrincipal UserPrincipal me,
            @Valid @RequestBody CreateGroupRequest req
    ) {
        if (me == null) {
            throw ApiException.unauthorized("UNAUTHORIZED", "Missing JWT principal");
        }
        return groupService.createGroup(me.getUserId(), req);
    }

    @PostMapping("/schedule/validate")
    public GroupScheduleConflictResponse validateSchedule(
            @AuthenticationPrincipal UserPrincipal me,
            @Valid @RequestBody ValidateGroupScheduleRequest req
    ) {
        if (me == null) {
            throw ApiException.unauthorized("UNAUTHORIZED", "Missing JWT principal");
        }
        return groupService.validateSchedule(me.getUserId(), req);
    }

    @GetMapping("/{groupId}")
    public GroupResponse getGroupDetail(
            @AuthenticationPrincipal UserPrincipal me,
            @PathVariable UUID groupId
    ) {
        if (me == null) {
            throw ApiException.unauthorized("UNAUTHORIZED", "Missing JWT principal");
        }
        return groupService.getGroupDetail(me.getUserId(), groupId);
    }

    @PatchMapping("/{groupId}")
    public GroupResponse updateGroup(
            @AuthenticationPrincipal UserPrincipal me,
            @PathVariable UUID groupId,
            @Valid @RequestBody UpdateGroupRequest req
    ) {
        if (me == null) {
            throw ApiException.unauthorized("UNAUTHORIZED", "Missing JWT principal");
        }
        return groupService.updateGroup(me.getUserId(), groupId, req);
    }

    @PatchMapping("/{groupId}/status")
    public GroupResponse updateGroupStatus(
            @AuthenticationPrincipal UserPrincipal me,
            @PathVariable UUID groupId,
            @Valid @RequestBody UpdateGroupStatusRequest req
    ) {
        if (me == null) {
            throw ApiException.unauthorized("UNAUTHORIZED", "Missing JWT principal");
        }
        return groupService.updateGroupStatus(me.getUserId(), groupId, req);
    }

    @PostMapping("/{groupId}/archive")
    public GroupResponse archiveGroup(
            @AuthenticationPrincipal UserPrincipal me,
            @PathVariable UUID groupId
    ) {
        if (me == null) {
            throw ApiException.unauthorized("UNAUTHORIZED", "Missing JWT principal");
        }
        return groupService.archiveGroup(me.getUserId(), groupId);
    }
}
