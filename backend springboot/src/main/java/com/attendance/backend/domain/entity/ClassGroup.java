package com.attendance.backend.domain.entity;

import com.attendance.backend.common.persistence.MysqlUuidBinary16SwapType;
import com.attendance.backend.common.persistence.UuidBinary16SwapConverter;
import com.attendance.backend.domain.enums.ApprovalMode;
import com.attendance.backend.domain.enums.GroupStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "class_groups",
        indexes = {
                @Index(name = "idx_groups_owner", columnList = "owner_user_id"),
                @Index(name = "idx_groups_status_created", columnList = "status, created_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_groups_code", columnNames = {"code"}),
                @UniqueConstraint(name = "uk_groups_join_code", columnNames = {"join_code"})
        }
)
public class ClassGroup {

    @Id
    @Type(value = MysqlUuidBinary16SwapType.class)
    @Column(name = "id", columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "owner_user_id", columnDefinition = "BINARY(16)", nullable = false)
    @Convert(converter = UuidBinary16SwapConverter.class)
    private UUID ownerUserId;

    @Column(name = "name", length = 150, nullable = false)
    private String name;

    @Column(name = "code", length = 20, nullable = false)
    private String code;

    @Column(name = "course_code", length = 50)
    private String courseCode;

    @Column(name = "class_code", length = 50)
    private String classCode;

    @Column(name = "join_code", length = 16, nullable = false)
    private String joinCode;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "semester", length = 30)
    private String semester;

    @Column(name = "academic_year", length = 30)
    private String academicYear;

    @Column(name = "room", length = 80)
    private String room;

    @Column(name = "campus", length = 120)
    private String campus;

    @Column(name = "total_sessions")
    private Integer totalSessions;

    @Column(name = "max_allowed_absences")
    private Integer maxAllowedAbsences;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_mode", nullable = false, length = 20)
    private ApprovalMode approvalMode;

    @Column(name = "allow_auto_join_on_checkin", nullable = false)
    private byte allowAutoJoinOnCheckin;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private GroupStatus status;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public ClassGroup() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(UUID ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
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

    public String getJoinCode() {
        return joinCode;
    }

    public void setJoinCode(String joinCode) {
        this.joinCode = joinCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
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

    public Integer getTotalSessions() {
        return totalSessions;
    }

    public void setTotalSessions(Integer totalSessions) {
        this.totalSessions = totalSessions;
    }

    public Integer getMaxAllowedAbsences() {
        return maxAllowedAbsences;
    }

    public void setMaxAllowedAbsences(Integer maxAllowedAbsences) {
        this.maxAllowedAbsences = maxAllowedAbsences;
    }

    public ApprovalMode getApprovalMode() {
        return approvalMode;
    }

    public void setApprovalMode(ApprovalMode approvalMode) {
        this.approvalMode = approvalMode;
    }

    public boolean isAllowAutoJoinOnCheckin() {
        return allowAutoJoinOnCheckin == 1;
    }

    public void setAllowAutoJoinOnCheckin(boolean allowAutoJoinOnCheckin) {
        this.allowAutoJoinOnCheckin = (byte) (allowAutoJoinOnCheckin ? 1 : 0);
    }

    public GroupStatus getStatus() {
        return status;
    }

    public void setStatus(GroupStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }
}