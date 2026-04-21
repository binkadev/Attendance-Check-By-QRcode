package com.androidapp.attendencecheckqrcode.data.dto.teaching;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class GroupStudentPolicyResponse {
    @SerializedName("policy")
    private Policy policy;

    @SerializedName("items")
    private List<StudentItem> students;

    @SerializedName("page")
    private int page;
    @SerializedName("size")
    private int size;
    @SerializedName("totalElements")
    private long totalElements;
    @SerializedName("totalPages")
    private int totalPages;

    public Policy getPolicy() { return policy; }
    public List<StudentItem> getStudents() { return students; }

    public static class Policy {
        @SerializedName("criticalAbsentCount")
        private Integer criticalAbsentCount; // Dùng Integer để tránh null pointer nếu db trả về null

        public int getCriticalAbsentCount() {
            return criticalAbsentCount != null ? criticalAbsentCount : 0;
        }
    }

    public static class StudentItem {
        @SerializedName("userId") private String userId;
        @SerializedName("fullName") private String fullName;
        @SerializedName("email") private String email;
        @SerializedName("absentCount") private int absentCount;

        public String getFullName() { return fullName; }
        public String getEmail() { return email; }
        public int getAbsentCount() { return absentCount; }
    }
}