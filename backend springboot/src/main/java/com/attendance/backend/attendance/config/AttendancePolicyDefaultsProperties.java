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

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100.00");

    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private BigDecimal lateWeight = new BigDecimal("1.0000");

    @DecimalMin("0.0")
    @DecimalMax("100.0")
    private BigDecimal warningBelowRate = new BigDecimal("80.00");

    @DecimalMin("0.0")
    @DecimalMax("100.0")
    private BigDecimal criticalBelowRate = new BigDecimal("75.00");

    @DecimalMin("0.0")
    @DecimalMax("100.0")
    private BigDecimal examBanAbsenceRate = new BigDecimal("30.00");

    @Min(1)
    private Integer warningAbsentCount;

    @Min(1)
    private Integer criticalAbsentCount;

    @Min(1)
    private Integer examBanAbsentCount;

    public void validateLogicalOrder() {
        if (criticalBelowRate != null
                && warningBelowRate != null
                && criticalBelowRate.compareTo(warningBelowRate) >= 0) {
            throw new IllegalStateException(
                    "attendance.policy.defaults.critical-below-rate must be less than warning-below-rate"
            );
        }

        if (examBanAbsenceRate != null && criticalBelowRate != null) {
            BigDecimal criticalAbsenceRate = ONE_HUNDRED.subtract(criticalBelowRate);
            if (examBanAbsenceRate.compareTo(criticalAbsenceRate) <= 0) {
                throw new IllegalStateException(
                        "attendance.policy.defaults.exam-ban-absence-rate must be greater than the critical absence rate"
                );
            }
        }

        if (examBanAbsenceRate != null && criticalBelowRate == null && warningBelowRate != null) {
            BigDecimal warningAbsenceRate = ONE_HUNDRED.subtract(warningBelowRate);
            if (examBanAbsenceRate.compareTo(warningAbsenceRate) <= 0) {
                throw new IllegalStateException(
                        "attendance.policy.defaults.exam-ban-absence-rate must be greater than the warning absence rate"
                );
            }
        }

        if (warningAbsentCount != null
                && criticalAbsentCount != null
                && criticalAbsentCount <= warningAbsentCount) {
            throw new IllegalStateException(
                "attendance.policy.defaults.critical-absent-count must be greater than warning-absent-count"
            );
        }

        if (criticalAbsentCount != null
                && examBanAbsentCount != null
                && examBanAbsentCount <= criticalAbsentCount) {
            throw new IllegalStateException(
                    "attendance.policy.defaults.exam-ban-absent-count must be greater than critical-absent-count"
            );
        }

        if (criticalAbsentCount == null
                && warningAbsentCount != null
                && examBanAbsentCount != null
                && examBanAbsentCount <= warningAbsentCount) {
            throw new IllegalStateException(
                    "attendance.policy.defaults.exam-ban-absent-count must be greater than warning-absent-count"
            );
        }
    }
}
