package com.attendance.backend.me.repository;

import com.attendance.backend.me.dto.MyClassTimelineItemResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface MyClassTimelineRepository {

    List<MyClassTimelineItemResponse> findMyClassTimeline(
            UUID actorUserId,
            LocalDate today,
            LocalDate tomorrow,
            LocalDateTime now
    );
}
