package com.attendance.backend.group.service;

import com.attendance.backend.group.dto.CreateGroupRequest;
import com.attendance.backend.group.dto.GroupResponse;
import com.attendance.backend.group.dto.UpdateGroupRequest;
import com.attendance.backend.group.dto.UpdateGroupStatusRequest;

import java.util.UUID;

public interface GroupService {

    GroupResponse createGroup(UUID callerUserId, CreateGroupRequest req);

    GroupResponse getGroupDetail(UUID callerUserId, UUID groupId);

    GroupResponse updateGroup(UUID callerUserId, UUID groupId, UpdateGroupRequest req);

    GroupResponse updateGroupStatus(UUID callerUserId, UUID groupId, UpdateGroupStatusRequest req);

    GroupResponse archiveGroup(UUID callerUserId, UUID groupId);
}