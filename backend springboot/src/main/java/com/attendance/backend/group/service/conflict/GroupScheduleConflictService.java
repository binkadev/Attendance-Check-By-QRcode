package com.attendance.backend.group.service.conflict;

import com.attendance.backend.group.dto.GroupScheduleConflictResponse;
import com.attendance.backend.group.dto.GroupWeeklyScheduleRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface GroupScheduleConflictService {

    GroupScheduleConflictResponse validateSchedule(
            UUID ownerUserId,
            UUID excludeGroupId,
            LocalDate startDate,
            List<GroupWeeklyScheduleRequest> weeklySchedules,
            Integer totalSessions,
            String campus,
            String room
    );

    void assertNoConflict(
            UUID ownerUserId,
            UUID excludeGroupId,
            LocalDate startDate,
            List<GroupWeeklyScheduleRequest> weeklySchedules,
            Integer totalSessions,
            String campus,
            String room
    );
}
