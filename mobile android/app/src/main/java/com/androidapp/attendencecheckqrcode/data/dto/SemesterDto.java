package com.androidapp.attendencecheckqrcode.data.dto;

import com.google.gson.annotations.SerializedName;

public class SemesterDto {

    @SerializedName("semester")
    private String semester;

    @SerializedName("academicYear")
    private String academicYear;

    @SerializedName("label")
    private String label;

    public SemesterDto() {
    }

    public String getSemester() { return semester; }
    public String getAcademicYear() { return academicYear; }
    public String getLabel() { return label; }
}