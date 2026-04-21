package com.androidapp.attendencecheckqrcode.data.dto.group;

public class WeeklySchedule {
    private String dayOfWeek; // "MONDAY", "TUESDAY",...
    private String startTime; // "12:21"
    private String endTime;   // "20:01"

    public WeeklySchedule(String dayOfWeek, String startTime, String endTime) {
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
    }
}
