package com.attendance.backend.me.api.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class MyClassTimelineItemResponse {

    private String bucket;
    private UUID groupId;
    private String groupName;
    private String roleLabel;
    private String courseCode;
    private String classCode;
    private String room;
    private String campus;
    private String locationDisplay;
    private String lecturerName;
    private long approvedStudentCount;
    private String semester;
    private String academicYear;
    private String thumbnail;
    private String avatarUrl;
    private String myRole;
    private String myMemberStatus;
    private LocalDate occurrenceDate;
    private String dayOfWeek;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private UUID representativeSessionId;
    private String representativeSessionStatus;
    private LocalDateTime checkinOpenAt;
    private LocalDateTime checkinCloseAt;

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public UUID getGroupId() {
        return groupId;
    }

    public void setGroupId(UUID groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getRoleLabel() {
        return roleLabel;
    }

    public void setRoleLabel(String roleLabel) {
        this.roleLabel = roleLabel;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getClassCode() {
        return classCode;
    }

    public void setClassCode(String classCode) {
        this.classCode = classCode;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public String getCampus() {
        return campus;
    }

    public void setCampus(String campus) {
        this.campus = campus;
    }

    public String getLocationDisplay() {
        return locationDisplay;
    }

    public void setLocationDisplay(String locationDisplay) {
        this.locationDisplay = locationDisplay;
    }

    public String getLecturerName() {
        return lecturerName;
    }

    public void setLecturerName(String lecturerName) {
        this.lecturerName = lecturerName;
    }

    public long getApprovedStudentCount() {
        return approvedStudentCount;
    }

    public void setApprovedStudentCount(long approvedStudentCount) {
        this.approvedStudentCount = approvedStudentCount;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getMyRole() {
        return myRole;
    }

    public void setMyRole(String myRole) {
        this.myRole = myRole;
    }

    public String getMyMemberStatus() {
        return myMemberStatus;
    }

    public void setMyMemberStatus(String myMemberStatus) {
        this.myMemberStatus = myMemberStatus;
    }

    public LocalDate getOccurrenceDate() {
        return occurrenceDate;
    }

    public void setOccurrenceDate(LocalDate occurrenceDate) {
        this.occurrenceDate = occurrenceDate;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public void setStartAt(LocalDateTime startAt) {
        this.startAt = startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public void setEndAt(LocalDateTime endAt) {
        this.endAt = endAt;
    }

    public UUID getRepresentativeSessionId() {
        return representativeSessionId;
    }

    public void setRepresentativeSessionId(UUID representativeSessionId) {
        this.representativeSessionId = representativeSessionId;
    }

    public String getRepresentativeSessionStatus() {
        return representativeSessionStatus;
    }

    public void setRepresentativeSessionStatus(String representativeSessionStatus) {
        this.representativeSessionStatus = representativeSessionStatus;
    }

    public LocalDateTime getCheckinOpenAt() {
        return checkinOpenAt;
    }

    public void setCheckinOpenAt(LocalDateTime checkinOpenAt) {
        this.checkinOpenAt = checkinOpenAt;
    }

    public LocalDateTime getCheckinCloseAt() {
        return checkinCloseAt;
    }

    public void setCheckinCloseAt(LocalDateTime checkinCloseAt) {
        this.checkinCloseAt = checkinCloseAt;
    }
}
