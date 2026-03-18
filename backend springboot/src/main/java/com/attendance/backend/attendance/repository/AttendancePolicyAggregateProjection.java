package com.attendance.backend.attendance.repository;

import java.sql.Timestamp;
import java.util.UUID;

public interface AttendancePolicyAggregateProjection {

    UUID getUserId();

    String getFullName();

    String getEmail();

    Timestamp getJoinedAt();

    Long getClosedSessionCount();

    Long getPresentCount();

    Long getLateCount();

    Long getAbsentCount();

    Long getExcusedCount();
}