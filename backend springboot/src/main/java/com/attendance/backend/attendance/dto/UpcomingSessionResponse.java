package com.attendance.backend.attendance.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record UpcomingSessionResponse(
        UUID attendanceSessionId,

        String attendanceStatus,
        String attendanceStatusLabel,
        Boolean checkedIn,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "Asia/Ho_Chi_Minh")
        Instant checkInAt,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "Asia/Ho_Chi_Minh")
        Instant checkinOpenAt,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "Asia/Ho_Chi_Minh")
        Instant checkinCloseAt,

        UUID groupId,
        String sessionName,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "Asia/Ho_Chi_Minh")
        Instant startAt,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "Asia/Ho_Chi_Minh")
        Instant endAt,

        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate sessionDate,

        @JsonFormat(pattern = "HH:mm")
        LocalTime startTime,

        @JsonFormat(pattern = "HH:mm")
        LocalTime endTime,

        String room,
        String groupName,
        String lecturerName
) {
}