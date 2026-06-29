package com.attendance.backend.attendance.service;

import com.attendance.backend.attendance.config.AttendancePolicyDefaultsProperties;
import com.attendance.backend.attendance.repository.AttendancePolicyRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@Primary
public class AttendancePolicyEffectiveThresholdService extends AttendancePolicyService {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    @PersistenceContext
    private EntityManager entityManager;

    public AttendancePolicyEffectiveThresholdService(
            AttendancePolicyRepository attendancePolicyRepository,
            AttendancePolicyDefaultsProperties defaults
    ) {
        super(attendancePolicyRepository, defaults);
    }

    @Override
    @Transactional(readOnly = true)
    public EffectivePolicy getEffectivePolicy(UUID groupId) {
        return withEffectiveCount(groupId, super.getEffectivePolicy(groupId));
    }

    @Override
    @Transactional(readOnly = true)
    public PolicyView getGroupPolicy(UUID groupId, UUID actorUserId) {
        PolicyView base = super.getGroupPolicy(groupId, actorUserId);
        GroupThreshold threshold = findGroupThreshold(groupId);
        Integer effectiveCount = resolveEffectiveCount(
                base.examBanAbsentCount(),
                threshold.maxAllowedAbsences(),
                threshold.totalSessions(),
                base.examBanAbsenceRate()
        );
        return new PolicyView(
                base.groupId(),
                base.source(),
                base.lateWeight(),
                base.warningBelowRate(),
                base.criticalBelowRate(),
                base.examBanAbsenceRate(),
                base.warningAbsentCount(),
                base.criticalAbsentCount(),
                effectiveCount,
                base.requireLocation(),
                base.locationLat(),
                base.locationLng(),
                base.allowedRadiusMeter(),
                base.excusedHandling(),
                base.sessionScope(),
                base.membershipScope(),
                base.createdAt(),
                base.createdByUserId(),
                base.updatedAt(),
                base.updatedByUserId()
        );
    }

    private EffectivePolicy withEffectiveCount(UUID groupId, EffectivePolicy base) {
        GroupThreshold threshold = findGroupThreshold(groupId);
        Integer effectiveCount = resolveEffectiveCount(
                base.examBanAbsentCount(),
                threshold.maxAllowedAbsences(),
                threshold.totalSessions(),
                base.examBanAbsenceRate()
        );
        return new EffectivePolicy(
                base.source(),
                base.lateWeight(),
                base.warningBelowRate(),
                base.criticalBelowRate(),
                base.examBanAbsenceRate(),
                base.warningAbsentCount(),
                base.criticalAbsentCount(),
                effectiveCount,
                base.requireLocation(),
                base.locationLat(),
                base.locationLng(),
                base.allowedRadiusMeter(),
                base.createdAt(),
                base.createdByUserId(),
                base.updatedAt(),
                base.updatedByUserId()
        );
    }

    private GroupThreshold findGroupThreshold(UUID groupId) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("""
                select total_sessions, max_allowed_absences
                from class_groups
                where id = UUID_TO_BIN(:groupId, 1)
                  and deleted_at is null
                limit 1
                """)
                .setParameter("groupId", groupId.toString())
                .getResultList();

        if (rows.isEmpty()) {
            return new GroupThreshold(0L, null);
        }

        Object[] row = rows.get(0);
        return new GroupThreshold(toPositiveLong(row[0]), toPositiveInteger(row[1]));
    }

    private Integer resolveEffectiveCount(
            Integer configuredCount,
            Integer groupConfiguredCount,
            long totalSessions,
            BigDecimal percent
    ) {
        if (configuredCount != null && configuredCount > 0) {
            return configuredCount;
        }
        if (groupConfiguredCount != null && groupConfiguredCount > 0) {
            return groupConfiguredCount;
        }
        if (totalSessions <= 0 || percent == null) {
            return null;
        }
        BigDecimal raw = BigDecimal.valueOf(totalSessions)
                .multiply(percent)
                .divide(ONE_HUNDRED, 10, RoundingMode.HALF_UP);
        return Math.max(raw.setScale(0, RoundingMode.CEILING).intValue(), 1);
    }

    private long toPositiveLong(Object value) {
        if (!(value instanceof Number number)) {
            return 0L;
        }
        long result = number.longValue();
        return result > 0 ? result : 0L;
    }

    private Integer toPositiveInteger(Object value) {
        if (!(value instanceof Number number)) {
            return null;
        }
        int result = number.intValue();
        return result > 0 ? result : null;
    }

    private record GroupThreshold(long totalSessions, Integer maxAllowedAbsences) {
    }
}
