package com.attendance.backend.group.repository;

import com.attendance.backend.domain.entity.GroupWeeklySchedule;
import com.attendance.backend.domain.enums.GroupStatus;
import com.attendance.backend.group.service.conflict.GroupScheduleConflictCandidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface GroupWeeklyScheduleRepository extends JpaRepository<GroupWeeklySchedule, UUID> {

    List<GroupWeeklySchedule> findByGroupIdOrderByDayOfWeekAscStartTimeAsc(UUID groupId);

    void deleteByGroupId(UUID groupId);

    @Query("""
            select new com.attendance.backend.group.service.conflict.GroupScheduleConflictCandidate(
                g.id,
                g.ownerUserId,
                g.name,
                g.courseCode,
                g.classCode,
                g.campus,
                g.room,
                g.startDate,
                g.plannedEndDate,
                s.dayOfWeek,
                s.startTime,
                s.endTime
            )
            from GroupWeeklySchedule s
            join ClassGroup g on g.id = s.groupId
            where g.deletedAt is null
              and s.deletedAt is null
              and g.status = :status
              and g.startDate is not null
              and g.plannedEndDate is not null
              and (:excludeGroupId is null or g.id <> :excludeGroupId)
              and g.startDate <= :requestedEndDate
              and g.plannedEndDate >= :requestedStartDate
            """)
    List<GroupScheduleConflictCandidate> findActiveScheduleCandidatesOverlappingDateRange(
            @Param("excludeGroupId") UUID excludeGroupId,
            @Param("requestedStartDate") LocalDate requestedStartDate,
            @Param("requestedEndDate") LocalDate requestedEndDate,
            @Param("status") GroupStatus status
    );
}
