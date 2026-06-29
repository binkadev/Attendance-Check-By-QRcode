package com.attendance.backend.attendance.repository;

public interface AttendancePolicyGroupBasicProjection {

    String getGroupId();

    String getGroupName();

    String getGroupStatus();

    Integer getTotalSessions();

    Integer getMaxAllowedAbsences();
}
