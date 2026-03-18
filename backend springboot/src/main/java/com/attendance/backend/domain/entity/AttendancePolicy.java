package com.attendance.backend.domain.entity;

import com.attendance.backend.common.persistence.MysqlUuidBinary16SwapType;
import com.attendance.backend.common.persistence.UuidBinary16SwapConverter;
import org.hibernate.annotations.Type;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "attendance_policies",
        indexes = {
                @Index(name = "idx_ap_updated_at", columnList = "updated_at")
        }
)
public class AttendancePolicy {

    @Id
    @Type(value = MysqlUuidBinary16SwapType.class)
    @Column(name = "id", columnDefinition = "BINARY(16)")
    public UUID id;

    @Column(name = "group_id", columnDefinition = "BINARY(16)", nullable = false, unique = true)
    @Convert(converter = UuidBinary16SwapConverter.class)
    public UUID groupId;

    @Column(name = "late_weight", precision = 5, scale = 4, nullable = false)
    public BigDecimal lateWeight;

    @Column(name = "warning_below_rate", precision = 5, scale = 2, nullable = false)
    public BigDecimal warningBelowRate;

    @Column(name = "critical_below_rate", precision = 5, scale = 2)
    public BigDecimal criticalBelowRate;

    @Column(name = "warning_absent_count")
    public Integer warningAbsentCount;

    @Column(name = "critical_absent_count")
    public Integer criticalAbsentCount;

    @Column(name = "created_by_user_id", columnDefinition = "BINARY(16)", nullable = false)
    @Convert(converter = UuidBinary16SwapConverter.class)
    public UUID createdByUserId;

    @Column(name = "updated_by_user_id", columnDefinition = "BINARY(16)", nullable = false)
    @Convert(converter = UuidBinary16SwapConverter.class)
    public UUID updatedByUserId;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    public Instant updatedAt;

    public AttendancePolicy() {
    }
}