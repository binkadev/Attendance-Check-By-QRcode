package com.attendance.backend.me.api.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class MyClassTimelineResponse {

    private LocalDate today;
    private LocalDate tomorrow;
    private LocalDateTime serverTime;
    private List<MyClassTimelineItemResponse> ongoing;
    private List<MyClassTimelineItemResponse> upcomingToday;
    private List<MyClassTimelineItemResponse> upcomingTomorrow;

    public LocalDate getToday() {
        return today;
    }

    public void setToday(LocalDate today) {
        this.today = today;
    }

    public LocalDate getTomorrow() {
        return tomorrow;
    }

    public void setTomorrow(LocalDate tomorrow) {
        this.tomorrow = tomorrow;
    }

    public LocalDateTime getServerTime() {
        return serverTime;
    }

    public void setServerTime(LocalDateTime serverTime) {
        this.serverTime = serverTime;
    }

    public List<MyClassTimelineItemResponse> getOngoing() {
        return ongoing;
    }

    public void setOngoing(List<MyClassTimelineItemResponse> ongoing) {
        this.ongoing = ongoing;
    }

    public List<MyClassTimelineItemResponse> getUpcomingToday() {
        return upcomingToday;
    }

    public void setUpcomingToday(List<MyClassTimelineItemResponse> upcomingToday) {
        this.upcomingToday = upcomingToday;
    }

    public List<MyClassTimelineItemResponse> getUpcomingTomorrow() {
        return upcomingTomorrow;
    }

    public void setUpcomingTomorrow(List<MyClassTimelineItemResponse> upcomingTomorrow) {
        this.upcomingTomorrow = upcomingTomorrow;
    }
}
