package com.attendance.backend.group.repository;

import com.attendance.backend.domain.entity.GroupWeeklySchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GroupWeeklyScheduleRepository extends JpaRepository<GroupWeeklySchedule, UUID> {

    List<GroupWeeklySchedule> findByGroupIdOrderByDayOfWeekAscStartTimeAsc(UUID groupId);

    void deleteByGroupId(UUID groupId);
}