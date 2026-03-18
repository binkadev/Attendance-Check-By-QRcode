package com.attendance.backend.attendance.repository;

import java.sql.Timestamp;

public interface AttendancePolicyStudentAggregateProjection {

    String getUserId();

    String getFullName();

    String getEmail();

    Timestamp getJoinedAt();

    Long getClosedSessionCount();

    Long getPresentCount();

    Long getLateCount();

    Long getAbsentCount();

    Long getExcusedCount();
}