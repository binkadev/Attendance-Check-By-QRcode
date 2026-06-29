package com.attendance.backend.attendance.service;

import com.attendance.backend.attendance.dto.AttendancePolicyQueryDtos;
import com.attendance.backend.attendance.config.AttendancePolicyDefaultsProperties;
import com.attendance.backend.attendance.repository.AttendancePolicyGroupBasicProjection;
import com.attendance.backend.attendance.repository.AttendancePolicyMembershipAccessProjection;
import com.attendance.backend.attendance.repository.AttendancePolicyQueryRepository;
import com.attendance.backend.attendance.repository.AttendancePolicyRepository;
import com.attendance.backend.attendance.repository.AttendancePolicyStudentAggregateProjection;
import com.attendance.backend.common.exception.ApiException;
import com.attendance.backend.domain.entity.AttendancePolicy;
import com.attendance.backend.domain.enums.AttendancePolicySource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class AttendancePolicyQueryService {

    private static final String EXCUSED_HANDLING = "EXCLUDE_FROM_DENOMINATOR";
    private static final String SESSION_SCOPE = "CLOSED_NON_DELETED_ONLY";
    private static final String MEMBERSHIP_SCOPE = "AFTER_MEMBERSHIP_EFFECTIVE_AT";

    private final AttendancePolicyRepository attendancePolicyRepository;
    private final AttendancePolicyQueryRepository attendancePolicyQueryRepository;
    private final AttendancePolicyDefaultsProperties defaults;

    public AttendancePolicyQueryService(
            AttendancePolicyRepository attendancePolicyRepository,
            AttendancePolicyQueryRepository attendancePolicyQueryRepository,
            AttendancePolicyDefaultsProperties defaults
    ) {
        this.attendancePolicyRepository = attendancePolicyRepository;
        this.attendancePolicyQueryRepository = attendancePolicyQueryRepository;
        this.defaults = defaults;
    }

    @Transactional(readOnly = true)
    public AttendancePolicyQueryDtos.MyAttendancePolicyStatusResponse getMyPolicyStatus(
            UUID groupId,
            UUID actorUserId
    ) {
        AttendancePolicyGroupBasicProjection group = requireGroup(groupId);
        AttendancePolicyStudentAggregateProjection aggregate =
                attendancePolicyQueryRepository.aggregateForApprovedMember(groupId.toString(), actorUserId.toString());

        if (aggregate == null) {
            throw ApiException.forbidden(
                    "FORBIDDEN",
                    "Only approved members can view their attendance policy status"
            );
        }

        AttendancePolicyQueryDtos.AttendancePolicyView policy = resolvePolicy(groupId, group);
        AttendancePolicyQueryDtos.AttendancePolicyStudentStatusView item = toStudentStatusView(policy, aggregate, group);

        return new AttendancePolicyQueryDtos.MyAttendancePolicyStatusResponse(
                groupId,
                group.getGroupName(),
                policy,
                item.closedSessionCount(),
                item.eligibleSessionCount(),
                item.totalPlannedSessionCount(),
                item.maxAllowedAbsences(),
                item.presentCount(),
                item.lateCount(),
                item.absentCount(),
                item.excusedCount(),
                item.earnedAttendancePoints(),
                item.attendanceRate(),
                item.absenceRate(),
                item.policyStatus(),
                item.riskLevel(),
                item.examEligibility(),
                item.computedWarningAbsentCount(),
                item.computedCriticalAbsentCount(),
                item.computedExamBanAbsentCount(),
                item.breachReasons()
        );
    }

    @Transactional(readOnly = true)
    public AttendancePolicyQueryDtos.AttendancePolicyStudentsPageResponse listGroupStudentStatuses(
            UUID groupId,
            UUID actorUserId,
            String q,
            int page,
            int size,
            String sort
    ) {
        AttendancePolicyGroupBasicProjection group = requireGroup(groupId);
        requireOwnerOrCoHost(groupId, actorUserId);

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        SortSpec sortSpec = parseSort(sort);
        String keyword = normalizeKeyword(q);

        long totalElements = attendancePolicyQueryRepository.countApprovedMembers(groupId.toString(), keyword);

        List<AttendancePolicyStudentAggregateProjection> rows =
                attendancePolicyQueryRepository.aggregatePageForApprovedMembers(
                        groupId.toString(),
                        keyword,
                        sortSpec.sortBy(),
                        sortSpec.sortDir(),
                        safeSize,
                        safePage * safeSize
                );

        AttendancePolicyQueryDtos.AttendancePolicyView policy = resolvePolicy(groupId, group);

        List<AttendancePolicyQueryDtos.AttendancePolicyStudentStatusView> items = rows.stream()
                .map(row -> toStudentStatusView(policy, row, group))
                .toList();

        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / safeSize);

        return new AttendancePolicyQueryDtos.AttendancePolicyStudentsPageResponse(
                policy,
                items,
                safePage,
                safeSize,
                totalElements,
                totalPages
        );
    }

    private AttendancePolicyGroupBasicProjection requireGroup(UUID groupId) {
        AttendancePolicyGroupBasicProjection group =
                attendancePolicyQueryRepository.findGroupBasic(groupId.toString());

        if (group == null) {
            throw ApiException.notFound("GROUP_NOT_FOUND", "Group not found");
        }

        return group;
    }

    private void requireOwnerOrCoHost(UUID groupId, UUID actorUserId) {
        AttendancePolicyMembershipAccessProjection access =
                attendancePolicyQueryRepository.findMembershipAccess(groupId.toString(), actorUserId.toString());

        if (access == null) {
            throw ApiException.forbidden("FORBIDDEN", "Only OWNER/CO_HOST can view group policy statuses");
        }

        boolean approved = "APPROVED".equalsIgnoreCase(access.getMemberStatus());
        boolean owner = "OWNER".equalsIgnoreCase(access.getRole());
        boolean coHost = "CO_HOST".equalsIgnoreCase(access.getRole());

        if (!approved || (!owner && !coHost)) {
            throw ApiException.forbidden("FORBIDDEN", "Only OWNER/CO_HOST can view group policy statuses");
        }
    }

    private AttendancePolicyQueryDtos.AttendancePolicyView resolvePolicy(
            UUID groupId,
            AttendancePolicyGroupBasicProjection group
    ) {
        AttendancePolicyQueryDtos.AttendancePolicyView raw = attendancePolicyRepository.findByGroupId(groupId)
                .map(this::toCustomPolicyView)
                .orElseGet(() -> new AttendancePolicyQueryDtos.AttendancePolicyView(
                        groupId,
                        AttendancePolicySource.SYSTEM_DEFAULT,
                        normalizeWeight(defaults.getLateWeight()),
                        normalizePercent(defaults.getWarningBelowRate()),
                        normalizeNullablePercent(defaults.getCriticalBelowRate()),
                        normalizeNullablePercent(defaults.getExamBanAbsenceRate()),
                        defaults.getWarningAbsentCount(),
                        defaults.getCriticalAbsentCount(),
                        defaults.getExamBanAbsentCount(),
                        EXCUSED_HANDLING,
                        SESSION_SCOPE,
                        MEMBERSHIP_SCOPE,
                        null,
                        null,
                        null,
                        null
                ));

        Integer effectiveExamBanAbsentCount = AttendancePolicyComputation.resolveEffectiveExamBanAbsentCount(
                raw.examBanAbsentCount(),
                positive(group.getMaxAllowedAbsences()),
                positiveLong(group.getTotalSessions()),
                raw.examBanAbsenceRate()
        );

        return new AttendancePolicyQueryDtos.AttendancePolicyView(
                raw.groupId(),
                raw.source(),
                raw.lateWeight(),
                raw.warningBelowRate(),
                raw.criticalBelowRate(),
                raw.examBanAbsenceRate(),
                raw.warningAbsentCount(),
                raw.criticalAbsentCount(),
                effectiveExamBanAbsentCount,
                raw.excusedHandling(),
                raw.sessionScope(),
                raw.membershipScope(),
                raw.createdAt(),
                raw.createdByUserId(),
                raw.updatedAt(),
                raw.updatedByUserId()
        );
    }

    private AttendancePolicyQueryDtos.AttendancePolicyView toCustomPolicyView(AttendancePolicy policy) {
        return new AttendancePolicyQueryDtos.AttendancePolicyView(
                policy.groupId,
                AttendancePolicySource.GROUP_POLICY,
                normalizeWeight(policy.lateWeight),
                normalizePercent(policy.warningBelowRate),
                normalizeNullablePercent(policy.criticalBelowRate),
                normalizeNullablePercent(policy.examBanAbsenceRate != null
                        ? policy.examBanAbsenceRate
                        : defaults.getExamBanAbsenceRate()),
                policy.warningAbsentCount,
                policy.criticalAbsentCount,
                policy.examBanAbsentCount != null
                        ? policy.examBanAbsentCount
                        : defaults.getExamBanAbsentCount(),
                EXCUSED_HANDLING,
                SESSION_SCOPE,
                MEMBERSHIP_SCOPE,
                policy.createdAt,
                policy.createdByUserId,
                policy.updatedAt,
                policy.updatedByUserId
        );
    }

    private AttendancePolicyQueryDtos.AttendancePolicyStudentStatusView toStudentStatusView(
            AttendancePolicyQueryDtos.AttendancePolicyView policy,
            AttendancePolicyStudentAggregateProjection row,
            AttendancePolicyGroupBasicProjection group
    ) {
        long closedSessionCount = nvl(row.getClosedSessionCount());
        long presentCount = nvl(row.getPresentCount());
        long lateCount = nvl(row.getLateCount());
        long absentCount = nvl(row.getAbsentCount());
        long excusedCount = nvl(row.getExcusedCount());
        long totalPlannedSessionCount = positiveLong(group.getTotalSessions());
        Integer maxAllowedAbsences = positive(group.getMaxAllowedAbsences());

        AttendancePolicyComputation.ComputedPolicyStatus computed =
                AttendancePolicyComputation.compute(
                        policy,
                        presentCount,
                        lateCount,
                        absentCount,
                        excusedCount,
                        totalPlannedSessionCount,
                        maxAllowedAbsences
                );

        return new AttendancePolicyQueryDtos.AttendancePolicyStudentStatusView(
                UUID.fromString(row.getUserId()),
                row.getFullName(),
                row.getEmail(),
                toInstant(row.getJoinedAt()),
                closedSessionCount,
                computed.eligibleSessionCount(),
                computed.thresholdBaseSessionCount(),
                computed.maxAllowedAbsences(),
                presentCount,
                lateCount,
                absentCount,
                excusedCount,
                computed.earnedAttendancePoints(),
                computed.attendanceRate(),
                computed.absenceRate(),
                computed.policyStatus(),
                computed.riskLevel(),
                computed.examEligibility(),
                computed.computedWarningAbsentCount(),
                computed.computedCriticalAbsentCount(),
                computed.computedExamBanAbsentCount(),
                computed.breachReasons()
        );
    }

    private SortSpec parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return new SortSpec("fullName", "asc");
        }

        String[] parts = sort.split(",", 2);
        String sortBy = parts[0].trim();
        String sortDir = parts.length > 1 ? parts[1].trim().toLowerCase(Locale.ROOT) : "asc";

        if (!sortBy.equals("fullName") && !sortBy.equals("email") && !sortBy.equals("joinedAt")) {
            throw ApiException.badRequest(
                    "INVALID_SORT",
                    "sort must use one of: fullName,email,joinedAt"
            );
        }

        if (!sortDir.equals("asc") && !sortDir.equals("desc")) {
            throw ApiException.badRequest("INVALID_SORT", "sort direction must be asc or desc");
        }

        return new SortSpec(sortBy, sortDir);
    }

    private String normalizeKeyword(String q) {
        if (q == null || q.isBlank()) {
            return null;
        }
        return q.trim();
    }

    private BigDecimal normalizeWeight(BigDecimal value) {
        if (value == null) {
            return new BigDecimal("1.0000");
        }
        return value.setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizePercent(BigDecimal value) {
        if (value == null) {
            return new BigDecimal("80.00");
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeNullablePercent(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private long nvl(Long value) {
        return value == null ? 0L : value;
    }

    private long positiveLong(Integer value) {
        return value == null || value <= 0 ? 0L : value.longValue();
    }

    private Integer positive(Integer value) {
        return value == null || value <= 0 ? null : value;
    }

    private Instant toInstant(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Timestamp ts) {
            return ts.toInstant();
        }
        if (value instanceof java.util.Date d) {
            return d.toInstant();
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        throw new IllegalStateException("Unsupported timestamp type: " + value.getClass());
    }

    private record SortSpec(String sortBy, String sortDir) {
    }
}
