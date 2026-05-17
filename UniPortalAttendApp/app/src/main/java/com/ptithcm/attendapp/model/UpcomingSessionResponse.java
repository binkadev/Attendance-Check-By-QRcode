package com.ptithcm.attendapp.model;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class UpcomingSessionResponse {
    @SerializedName("sections")
    private List<UpcomingSection> sections;

    public List<UpcomingSection> getSections() { return sections; }
}