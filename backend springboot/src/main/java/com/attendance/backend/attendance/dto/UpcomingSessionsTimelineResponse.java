package com.attendance.backend.attendance.dto;

import java.time.LocalDate;
import java.util.List;

public record UpcomingSessionsTimelineResponse(List<Section> sections) {
    public record Section(String key, String title, LocalDate date, List<UpcomingSessionResponse> items) {}
}