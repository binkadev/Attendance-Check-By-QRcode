package com.attendance.backend.attendance.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "attendance.policy.defaults")
public class AttendancePolicyDefaultsProperties {

    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private BigDecimal lateWeight = new BigDecimal("1.0000");

    @DecimalMin("0.0")
    @DecimalMax("100.0")
    private BigDecimal warningBelowRate = new BigDecimal("85.00");

    @DecimalMin("0.0")
    @DecimalMax("100.0")
    private BigDecimal criticalBelowRate = new BigDecimal("80.00");

    @Min(1)
    private Integer warningAbsentCount;

    @Min(1)
    private Integer criticalAbsentCount;

    public void validateLogicalOrder() {
        if (criticalBelowRate != null
                && warningBelowRate != null
                && criticalBelowRate.compareTo(warningBelowRate) >= 0) {
            throw new IllegalStateException(
                    "attendance.policy.defaults.critical-below-rate must be less than warning-below-rate"
            );
        }

        if (warningAbsentCount != null
                && criticalAbsentCount != null
                && criticalAbsentCount <= warningAbsentCount) {
            throw new IllegalStateException(
                    "attendance.policy.defaults.critical-absent-count must be greater than warning-absent-count"
            );
        }
    }
}