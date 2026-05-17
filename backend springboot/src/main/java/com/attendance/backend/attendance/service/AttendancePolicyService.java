package com.attendance.backend.attendance.service;

import com.attendance.backend.attendance.config.AttendancePolicyDefaultsProperties;
import com.attendance.backend.attendance.repository.AttendancePolicyRepository;
import com.attendance.backend.common.exception.ApiException;
import com.attendance.backend.domain.entity.AttendancePolicy;
import com.attendance.backend.domain.enums.AttendancePolicyBreachReason;
import com.attendance.backend.domain.enums.AttendancePolicySource;
import com.attendance.backend.domain.enums.AttendancePolicyStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


@Service
public class AttendancePolicyService {

    private static final String EXCUSED_HANDLING = "EXCLUDE_FROM_DENOMINATOR";
    private static final String SESSION_SCOPE = "CLOSED_NON_DELETED_ONLY";
    private static final String MEMBERSHIP_SCOPE = "AFTER_MEMBERSHIP_EFFECTIVE_AT";

    private final AttendancePolicyRepository attendancePolicyRepository;
    private final AttendancePolicyDefaultsProperties defaults;

    @PersistenceContext
    private EntityManager entityManager;

    public AttendancePolicyService(
            AttendancePolicyRepository attendancePolicyRepository,
            AttendancePolicyDefaultsProperties defaults
    ) {
        this.attendancePolicyRepository = attendancePolicyRepository;
        this.defaults = defaults;
    }

    @PostConstruct
    void validateDefaults() {
        defaults.validateLogicalOrder();
    }
    /**
     * Batch-facing resolved policy contract.
     * Batch 2+ must reuse this instead of re-resolving fallback logic.
     */
    @Transactional(readOnly = true)
    public EffectivePolicy getEffectivePolicy(UUID groupId) {
        requireExistingGroup(groupId);
        return resolveEffectivePolicy(groupId);
    }

    @Transactional(readOnly = true)
    public PolicyView getGroupPolicy(UUID groupId, UUID actorUserId) {
        requireExistingGroup(groupId);
        requireGroupAdminRead(groupId, actorUserId);
        return toPolicyView(groupId, resolveEffectivePolicy(groupId));
    }

    @Transactional
    public PolicyView upsertGroupPolicy(UUID groupId, UUID actorUserId, UpsertPolicyCommand cmd) {
        GroupState groupState = requireExistingGroup(groupId);
        requireGroupOwnerWrite(groupId, actorUserId);
        if (groupState.archived()) {
            throw ApiException.conflict("GROUP_ARCHIVED", "Archived group cannot update attendance policy");
        }

        validateCommand(cmd);

        AttendancePolicy policy = attendancePolicyRepository.findByGroupIdForUpdate(groupId)
                .orElseGet(() -> {
                    AttendancePolicy created = new AttendancePolicy();
                    created.id = UUID.randomUUID();
                    created.groupId = groupId;
                    created.createdByUserId = actorUserId;
                    return created;
                });

        policy.lateWeight = normalizeWeight(cmd.lateWeight());
        policy.warningBelowRate = normalizePercent(cmd.warningBelowRate());
        policy.criticalBelowRate = normalizeNullablePercent(cmd.criticalBelowRate());
        policy.warningAbsentCount = cmd.warningAbsentCount();
        policy.criticalAbsentCount = cmd.criticalAbsentCount();
        policy.requireLocation = cmd.requireLocation();
        policy.locationLat = normalizeNullableLatitude(cmd.locationLat());
        policy.locationLng = normalizeNullableLongitude(cmd.locationLng());
        policy.allowedRadiusMeter = normalizeAllowedRadiusMeter(cmd.allowedRadiusMeter());
        policy.updatedByUserId = actorUserId;

        attendancePolicyRepository.save(policy);
        return toPolicyView(groupId, resolveEffectivePolicy(groupId));
    }

    @Transactional
    public void resetGroupPolicy(UUID groupId, UUID actorUserId) {
        GroupState groupState = requireExistingGroup(groupId);
        requireGroupOwnerWrite(groupId, actorUserId);
        if (groupState.archived()) {
            throw ApiException.conflict("GROUP_ARCHIVED", "Archived group cannot update attendance policy");
        }
        attendancePolicyRepository.findByGroupIdForUpdate(groupId)
                .ifPresent(attendancePolicyRepository::delete);
    }

    @Transactional(readOnly = true)
    public StudentPolicyStatusView getMyPolicyStatus(UUID groupId, UUID userId) {
        requireExistingGroup(groupId);
        MembershipRow membership = requireApprovedMembership(groupId, userId);

        EffectivePolicy effectivePolicy = resolveEffectivePolicy(groupId);
        AttendanceCounts counts = aggregateCountsForUser(groupId, userId, membership.effectiveAt());

        return evaluateStudent(effectivePolicy, userId, null, null, membership.effectiveAt(), counts);
    }

    @Transactional(readOnly = true)
    public StudentPolicyPageView listGroupStudentStatuses(
            UUID groupId,
            UUID actorUserId,
            String q,
            int page,
            int size,
            String sort
    ) {
        requireExistingGroup(groupId);
        requireGroupAdminRead(groupId, actorUserId);

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        SortSpec sortSpec = parseSort(sort);
        String keyword = normalizeKeyword(q);

        long total = countApprovedMembers(groupId, keyword);
        List<MemberListRow> members = fetchApprovedMembers(groupId, keyword, safePage, safeSize, sortSpec);

        EffectivePolicy effectivePolicy = resolveEffectivePolicy(groupId);
        PolicyView policyView = toPolicyView(groupId, effectivePolicy);

        Map<UUID, AttendanceCounts> countsByUserId = aggregateCountsForUsers(groupId, members);
        List<StudentPolicyStatusView> items = new ArrayList<>();

        for (MemberListRow member : members) {
            AttendanceCounts counts = countsByUserId.getOrDefault(member.userId(), AttendanceCounts.zero());
            items.add(evaluateStudent(
                    effectivePolicy,
                    member.userId(),
                    member.fullName(),
                    member.email(),
                    member.effectiveAt(),
                    counts
            ));
        }

        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / safeSize);
        return new StudentPolicyPageView(policyView, items, safePage, safeSize, total, totalPages);
    }

    private GroupState requireExistingGroup(UUID groupId) {
        List<?> rows = entityManager.createNativeQuery("""
                select status
                from class_groups
                where id = UUID_TO_BIN(:groupId, 1)
                  and deleted_at is null
                """)
                .setParameter("groupId", groupId.toString())
                .getResultList();

        if (rows.isEmpty()) {
            throw ApiException.notFound("GROUP_NOT_FOUND", "Group not found");
        }

        String status = Objects.toString(rows.get(0), null);
        return new GroupState("ARCHIVED".equals(status));
    }

    private void requireGroupAdminRead(UUID groupId, UUID actorUserId) {
        MembershipAccessRow access = loadMembershipAccess(groupId, actorUserId);
        if (access == null || !access.approved() || !(access.owner() || access.coHost())) {
            throw ApiException.forbidden("FORBIDDEN", "Only OWNER/CO_HOST can view group attendance policy");
        }
    }

    private void requireGroupOwnerWrite(UUID groupId, UUID actorUserId) {
        MembershipAccessRow access = loadMembershipAccess(groupId, actorUserId);
        if (access == null || !access.approved() || !access.owner()) {
            throw ApiException.forbidden("FORBIDDEN", "Only OWNER can update attendance policy");
        }
    }

    private MembershipRow requireApprovedMembership(UUID groupId, UUID userId) {
        List<Object[]> rows = entityManager.createNativeQuery("""
                select member_status, joined_at, created_at
                from group_members
                where group_id = UUID_TO_BIN(:groupId, 1)
                  and user_id = UUID_TO_BIN(:userId, 1)
                """)
                .setParameter("groupId", groupId.toString())
                .setParameter("userId", userId.toString())
                .getResultList();

        if (rows.isEmpty()) {
            throw ApiException.forbidden("FORBIDDEN", "You are not a member of this group");
        }

        Object[] row = rows.get(0);
        String memberStatus = Objects.toString(row[0], null);
        if (!"APPROVED".equals(memberStatus)) {
            throw ApiException.forbidden("FORBIDDEN", "Only approved members can view attendance policy status");
        }

        Instant joinedAt = toInstant(row[1]);
        Instant createdAt = toInstant(row[2]);
        Instant effectiveAt = joinedAt != null ? joinedAt : createdAt;
        return new MembershipRow(effectiveAt);
    }

    private MembershipAccessRow loadMembershipAccess(UUID groupId, UUID actorUserId) {
        List<Object[]> rows = entityManager.createNativeQuery("""
                select role, member_status
                from group_members
                where group_id = UUID_TO_BIN(:groupId, 1)
                  and user_id = UUID_TO_BIN(:userId, 1)
                """)
                .setParameter("groupId", groupId.toString())
                .setParameter("userId", actorUserId.toString())
                .getResultList();

        if (rows.isEmpty()) {
            return null;
        }

        Object[] row = rows.get(0);
        String role = Objects.toString(row[0], null);
        String memberStatus = Objects.toString(row[1], null);
        return new MembershipAccessRow(
                "APPROVED".equals(memberStatus),
                "OWNER".equals(role),
                "CO_HOST".equals(role)
        );
    }

    /**
     * Single source of truth for effective policy resolution.
     * Current precedence:
     *   1) group policy row
     *   2) system defaults
     *
     * GLOBAL_POLICY enum value is reserved for future expansion.
     */
    private EffectivePolicy resolveEffectivePolicy(UUID groupId) {
        EffectivePolicy resolved = attendancePolicyRepository.findByGroupId(groupId)
                .map(this::toGroupEffectivePolicy)
                .orElseGet(this::toSystemDefaultEffectivePolicy);

        validateResolvedPolicy(resolved);
        return resolved;
    }

    private EffectivePolicy toGroupEffectivePolicy(AttendancePolicy policy) {
        return new EffectivePolicy(
                AttendancePolicySource.GROUP_POLICY,
                canonicalizeResolvedWeight(policy.lateWeight, AttendancePolicySource.GROUP_POLICY, "lateWeight"),
                canonicalizeResolvedPercent(policy.warningBelowRate, AttendancePolicySource.GROUP_POLICY, "warningBelowRate", false),
                canonicalizeResolvedPercent(policy.criticalBelowRate, AttendancePolicySource.GROUP_POLICY, "criticalBelowRate", true),
                canonicalizeResolvedAbsentCount(policy.warningAbsentCount, AttendancePolicySource.GROUP_POLICY, "warningAbsentCount", true),
                canonicalizeResolvedAbsentCount(policy.criticalAbsentCount, AttendancePolicySource.GROUP_POLICY, "criticalAbsentCount", true),
                Boolean.TRUE.equals(policy.requireLocation),
                canonicalizeResolvedLatitude(policy.locationLat, AttendancePolicySource.GROUP_POLICY, "locationLat", true),
                canonicalizeResolvedLongitude(policy.locationLng, AttendancePolicySource.GROUP_POLICY, "locationLng", true),
                canonicalizeResolvedAllowedRadius(policy.allowedRadiusMeter, AttendancePolicySource.GROUP_POLICY, "allowedRadiusMeter"),
                policy.createdAt,
                policy.createdByUserId,
                policy.updatedAt,
                policy.updatedByUserId
        );
    }

    private EffectivePolicy toSystemDefaultEffectivePolicy() {
        return new EffectivePolicy(
                AttendancePolicySource.SYSTEM_DEFAULT,
                canonicalizeResolvedWeight(defaults.getLateWeight(), AttendancePolicySource.SYSTEM_DEFAULT, "lateWeight"),
                canonicalizeResolvedPercent(defaults.getWarningBelowRate(), AttendancePolicySource.SYSTEM_DEFAULT, "warningBelowRate", false),
                canonicalizeResolvedPercent(defaults.getCriticalBelowRate(), AttendancePolicySource.SYSTEM_DEFAULT, "criticalBelowRate", true),
                canonicalizeResolvedAbsentCount(defaults.getWarningAbsentCount(), AttendancePolicySource.SYSTEM_DEFAULT, "warningAbsentCount", true),
                canonicalizeResolvedAbsentCount(defaults.getCriticalAbsentCount(), AttendancePolicySource.SYSTEM_DEFAULT, "criticalAbsentCount", true),
                false,
                null,
                null,
                150,
                null,
                null,
                null,
                null
        );
    }

    private PolicyView toPolicyView(UUID groupId, EffectivePolicy effectivePolicy) {
        return new PolicyView(
                groupId,
                effectivePolicy.source(),
                effectivePolicy.lateWeight(),
                effectivePolicy.warningBelowRate(),
                effectivePolicy.criticalBelowRate(),
                effectivePolicy.warningAbsentCount(),
                effectivePolicy.criticalAbsentCount(),
                effectivePolicy.requireLocation(),
                effectivePolicy.locationLat(),
                effectivePolicy.locationLng(),
                effectivePolicy.allowedRadiusMeter(),
                EXCUSED_HANDLING,
                SESSION_SCOPE,
                MEMBERSHIP_SCOPE,
                effectivePolicy.createdAt(),
                effectivePolicy.createdByUserId(),
                effectivePolicy.updatedAt(),
                effectivePolicy.updatedByUserId()
        );
    }

    private void validateResolvedPolicy(EffectivePolicy policy) {
        if (policy.criticalBelowRate() != null
                && policy.criticalBelowRate().compareTo(policy.warningBelowRate()) >= 0) {
            throw new IllegalStateException(
                    "Resolved attendance policy is invalid: criticalBelowRate must be lower than warningBelowRate"
            );
        }

        if (policy.warningAbsentCount() != null && policy.warningAbsentCount() < 1) {
            throw new IllegalStateException(
                    "Resolved attendance policy is invalid: warningAbsentCount must be >= 1"
            );
        }

        if (policy.criticalAbsentCount() != null && policy.criticalAbsentCount() < 1) {
            throw new IllegalStateException(
                    "Resolved attendance policy is invalid: criticalAbsentCount must be >= 1"
            );
        }

        if (policy.warningAbsentCount() != null
                && policy.criticalAbsentCount() != null
                && policy.criticalAbsentCount() <= policy.warningAbsentCount()) {
            throw new IllegalStateException(
                    "Resolved attendance policy is invalid: criticalAbsentCount must be greater than warningAbsentCount"
            );
        }

        validateLocationFields(
                policy.requireLocation(),
                policy.locationLat(),
                policy.locationLng(),
                policy.allowedRadiusMeter(),
                "Resolved attendance policy"
        );
    }

    private AttendanceCounts aggregateCountsForUser(UUID groupId, UUID userId, Instant membershipEffectiveAt) {
        List<Object[]> rows = entityManager.createNativeQuery("""
                select
                    count(s.id) as closed_session_count,
                    sum(case when coalesce(sa.attendance_status, 'ABSENT') <> 'EXCUSED' then 1 else 0 end) as eligible_session_count,
                    sum(case when coalesce(sa.attendance_status, 'ABSENT') = 'PRESENT' then 1 else 0 end) as present_count,
                    sum(case when coalesce(sa.attendance_status, 'ABSENT') = 'LATE' then 1 else 0 end) as late_count,
                    sum(case when coalesce(sa.attendance_status, 'ABSENT') = 'ABSENT' then 1 else 0 end) as absent_count,
                    sum(case when coalesce(sa.attendance_status, 'ABSENT') = 'EXCUSED' then 1 else 0 end) as excused_count
                from attendance_sessions s
                left join session_attendance sa
                    on sa.session_id = s.id
                   and sa.user_id = UUID_TO_BIN(:userId, 1)
                where s.group_id = UUID_TO_BIN(:groupId, 1)
                  and s.status = 'CLOSED'
                  and s.deleted_at is null
                  and s.start_at >= :membershipEffectiveAt
                """)
                .setParameter("groupId", groupId.toString())
                .setParameter("userId", userId.toString())
                .setParameter("membershipEffectiveAt", Timestamp.from(membershipEffectiveAt))
                .getResultList();

        if (rows.isEmpty()) {
            return AttendanceCounts.zero();
        }
        return toCounts(rows.get(0));
    }

    private Map<UUID, AttendanceCounts> aggregateCountsForUsers(UUID groupId, List<MemberListRow> members) {
        if (members.isEmpty()) {
            return Map.of();
        }

        String inClause = IntStream.range(0, members.size())
                .mapToObj(i -> "UUID_TO_BIN(:u" + i + ", 1)")
                .collect(Collectors.joining(", "));

        String sql = """
                select
                    BIN_TO_UUID(gm.user_id, 1) as user_id,
                    count(s.id) as closed_session_count,
                    sum(case when coalesce(sa.attendance_status, 'ABSENT') <> 'EXCUSED' then 1 else 0 end) as eligible_session_count,
                    sum(case when coalesce(sa.attendance_status, 'ABSENT') = 'PRESENT' then 1 else 0 end) as present_count,
                    sum(case when coalesce(sa.attendance_status, 'ABSENT') = 'LATE' then 1 else 0 end) as late_count,
                    sum(case when coalesce(sa.attendance_status, 'ABSENT') = 'ABSENT' then 1 else 0 end) as absent_count,
                    sum(case when coalesce(sa.attendance_status, 'ABSENT') = 'EXCUSED' then 1 else 0 end) as excused_count
                from group_members gm
                join attendance_sessions s
                  on s.group_id = gm.group_id
                 and s.status = 'CLOSED'
                 and s.deleted_at is null
                 and s.start_at >= coalesce(gm.joined_at, gm.created_at)
                left join session_attendance sa
                  on sa.session_id = s.id
                 and sa.user_id = gm.user_id
                where gm.group_id = UUID_TO_BIN(:groupId, 1)
                  and gm.member_status = 'APPROVED'
                  and gm.user_id in (%s)
                group by gm.user_id
                """.formatted(inClause);

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("groupId", groupId.toString());
        for (int i = 0; i < members.size(); i++) {
            query.setParameter("u" + i, members.get(i).userId().toString());
        }

        Map<UUID, AttendanceCounts> result = new HashMap<>();
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        for (Object[] row : rows) {
            UUID userId = UUID.fromString(Objects.toString(row[0]));
            result.put(userId, toCounts(row, 1));
        }
        return result;
    }

    private long countApprovedMembers(UUID groupId, String keyword) {
        StringBuilder sql = new StringBuilder("""
                select count(*)
                from group_members gm
                join users u on u.id = gm.user_id and u.deleted_at is null
                where gm.group_id = UUID_TO_BIN(:groupId, 1)
                  and gm.member_status = 'APPROVED'
                """);

        if (keyword != null) {
            sql.append(" and (lower(u.full_name) like :keyword or lower(u.email) like :keyword) ");
        }

        Query query = entityManager.createNativeQuery(sql.toString())
                .setParameter("groupId", groupId.toString());
        if (keyword != null) {
            query.setParameter("keyword", "%" + keyword + "%");
        }
        Number n = (Number) query.getSingleResult();
        return n.longValue();
    }

    private List<MemberListRow> fetchApprovedMembers(UUID groupId, String keyword, int page, int size, SortSpec sortSpec) {
        StringBuilder sql = new StringBuilder("""
                select
                    BIN_TO_UUID(gm.user_id, 1) as user_id,
                    u.full_name,
                    u.email,
                    gm.joined_at,
                    gm.created_at
                from group_members gm
                join users u on u.id = gm.user_id and u.deleted_at is null
                where gm.group_id = UUID_TO_BIN(:groupId, 1)
                  and gm.member_status = 'APPROVED'
                """);

        if (keyword != null) {
            sql.append(" and (lower(u.full_name) like :keyword or lower(u.email) like :keyword) ");
        }

        sql.append(" order by ").append(sortSpec.orderBySql())
                .append(" limit :limit offset :offset ");

        Query query = entityManager.createNativeQuery(sql.toString())
                .setParameter("groupId", groupId.toString())
                .setParameter("limit", size)
                .setParameter("offset", page * size);

        if (keyword != null) {
            query.setParameter("keyword", "%" + keyword + "%");
        }

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        List<MemberListRow> result = new ArrayList<>();
        for (Object[] row : rows) {
            UUID userId = UUID.fromString(Objects.toString(row[0]));
            String fullName = Objects.toString(row[1], null);
            String email = Objects.toString(row[2], null);
            Instant joinedAt = toInstant(row[3]);
            Instant createdAt = toInstant(row[4]);
            Instant effectiveAt = joinedAt != null ? joinedAt : createdAt;
            result.add(new MemberListRow(userId, fullName, email, effectiveAt));
        }
        return result;
    }

    private StudentPolicyStatusView evaluateStudent(
            EffectivePolicy policy,
            UUID userId,
            String fullName,
            String email,
            Instant joinedAt,
            AttendanceCounts counts
    ) {
        BigDecimal earnedAttendancePoints = BigDecimal.valueOf(counts.presentCount())
                .add(policy.lateWeight().multiply(BigDecimal.valueOf(counts.lateCount())))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal attendanceRate = null;
        AttendancePolicyStatus status = AttendancePolicyStatus.NO_DATA;
        List<AttendancePolicyBreachReason> reasons = new ArrayList<>();

        if (counts.eligibleSessionCount() > 0) {
            attendanceRate = earnedAttendancePoints
                    .multiply(new BigDecimal("100"))
                    .divide(BigDecimal.valueOf(counts.eligibleSessionCount()), 2, RoundingMode.HALF_UP);

            boolean critical = false;
            boolean warning = false;

            if (policy.criticalBelowRate() != null && attendanceRate.compareTo(policy.criticalBelowRate()) < 0) {
                reasons.add(AttendancePolicyBreachReason.RATE_BELOW_CRITICAL);
                critical = true;
            } else if (attendanceRate.compareTo(policy.warningBelowRate()) < 0) {
                reasons.add(AttendancePolicyBreachReason.RATE_BELOW_WARNING);
                warning = true;
            }

            if (policy.criticalAbsentCount() != null && counts.absentCount() >= policy.criticalAbsentCount()) {
                reasons.add(AttendancePolicyBreachReason.ABSENT_COUNT_CRITICAL);
                critical = true;
            } else if (policy.warningAbsentCount() != null && counts.absentCount() >= policy.warningAbsentCount()) {
                reasons.add(AttendancePolicyBreachReason.ABSENT_COUNT_WARNING);
                warning = true;
            }

            if (critical) {
                status = AttendancePolicyStatus.CRITICAL;
            } else if (warning) {
                status = AttendancePolicyStatus.WARNING;
            } else {
                status = AttendancePolicyStatus.NORMAL;
            }
        }

        return new StudentPolicyStatusView(
                userId,
                fullName,
                email,
                joinedAt,
                counts.closedSessionCount(),
                counts.eligibleSessionCount(),
                counts.presentCount(),
                counts.lateCount(),
                counts.absentCount(),
                counts.excusedCount(),
                earnedAttendancePoints,
                attendanceRate,
                status,
                reasons
        );
    }

    private AttendanceCounts toCounts(Object[] row) {
        return toCounts(row, 0);
    }

    private AttendanceCounts toCounts(Object[] row, int startIndex) {
        return new AttendanceCounts(
                toLong(row[startIndex]),
                toLong(row[startIndex + 1]),
                toLong(row[startIndex + 2]),
                toLong(row[startIndex + 3]),
                toLong(row[startIndex + 4]),
                toLong(row[startIndex + 5])
        );
    }

    private long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        return ((Number) value).longValue();
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
        throw new IllegalStateException("Unsupported timestamp type: " + value.getClass());
    }

    private void validateCommand(UpsertPolicyCommand cmd) {
        if (cmd == null) {
            throw ApiException.badRequest("ATTENDANCE_POLICY_REQUIRED", "Attendance policy request body is required");
        }

        BigDecimal lateWeight = normalizeWeight(cmd.lateWeight());
        BigDecimal warningBelowRate = normalizePercent(cmd.warningBelowRate());
        BigDecimal criticalBelowRate = normalizeNullablePercent(cmd.criticalBelowRate());

        if (criticalBelowRate != null && criticalBelowRate.compareTo(warningBelowRate) >= 0) {
            throw ApiException.unprocessable(
                    "ATTENDANCE_POLICY_RATE_ORDER_INVALID",
                    "criticalBelowRate must be lower than warningBelowRate"
            );
        }

        if (cmd.warningAbsentCount() != null && cmd.warningAbsentCount() < 1) {
            throw ApiException.unprocessable(
                    "ATTENDANCE_POLICY_WARNING_ABSENT_INVALID",
                    "warningAbsentCount must be >= 1"
            );
        }

        if (cmd.criticalAbsentCount() != null && cmd.criticalAbsentCount() < 1) {
            throw ApiException.unprocessable(
                    "ATTENDANCE_POLICY_CRITICAL_ABSENT_INVALID",
                    "criticalAbsentCount must be >= 1"
            );
        }

        if (cmd.warningAbsentCount() != null
                && cmd.criticalAbsentCount() != null
                && cmd.criticalAbsentCount() <= cmd.warningAbsentCount()) {
            throw ApiException.unprocessable(
                    "ATTENDANCE_POLICY_ABSENT_ORDER_INVALID",
                    "criticalAbsentCount must be greater than warningAbsentCount"
            );
        }

        validateLocationCommand(cmd);

        if (lateWeight.compareTo(BigDecimal.ZERO) < 0 || lateWeight.compareTo(BigDecimal.ONE) > 0) {
            throw ApiException.unprocessable(
                    "ATTENDANCE_POLICY_LATE_WEIGHT_INVALID",
                    "lateWeight must be between 0 and 1"
            );
        }
    }

    private BigDecimal normalizeWeight(BigDecimal value) {
        if (value == null) {
            throw ApiException.badRequest("ATTENDANCE_POLICY_LATE_WEIGHT_REQUIRED", "lateWeight is required");
        }
        return value.setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizePercent(BigDecimal value) {
        if (value == null) {
            throw ApiException.badRequest("ATTENDANCE_POLICY_WARNING_RATE_REQUIRED", "warningBelowRate is required");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(new BigDecimal("100")) > 0) {
            throw ApiException.unprocessable(
                    "ATTENDANCE_POLICY_PERCENT_INVALID",
                    "percentage must be between 0 and 100"
            );
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeNullablePercent(BigDecimal value) {
        if (value == null) {
            return null;
        }
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(new BigDecimal("100")) > 0) {
            throw ApiException.unprocessable(
                    "ATTENDANCE_POLICY_PERCENT_INVALID",
                    "percentage must be between 0 and 100"
            );
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private void validateLocationCommand(UpsertPolicyCommand cmd) {
        boolean requireLocation = Boolean.TRUE.equals(cmd.requireLocation());
        BigDecimal locationLat = normalizeNullableLatitude(cmd.locationLat());
        BigDecimal locationLng = normalizeNullableLongitude(cmd.locationLng());
        Integer allowedRadiusMeter = normalizeAllowedRadiusMeter(cmd.allowedRadiusMeter());

        validateLocationFields(
                requireLocation,
                locationLat,
                locationLng,
                allowedRadiusMeter,
                "Attendance policy"
        );
    }

    private void validateLocationFields(
            boolean requireLocation,
            BigDecimal locationLat,
            BigDecimal locationLng,
            Integer allowedRadiusMeter,
            String subject
    ) {
        boolean hasLat = locationLat != null;
        boolean hasLng = locationLng != null;

        if (hasLat != hasLng) {
            throw ApiException.unprocessable(
                    "ATTENDANCE_POLICY_LOCATION_PAIR_INVALID",
                    subject + " locationLat and locationLng must be provided together"
            );
        }

        if (requireLocation && (!hasLat || !hasLng)) {
            throw ApiException.unprocessable(
                    "ATTENDANCE_POLICY_LOCATION_REQUIRED_TARGET_INVALID",
                    subject + " requires locationLat and locationLng when requireLocation is true"
            );
        }

        if (allowedRadiusMeter == null || allowedRadiusMeter < 10 || allowedRadiusMeter > 10000) {
            throw ApiException.unprocessable(
                    "ATTENDANCE_POLICY_ALLOWED_RADIUS_INVALID",
                    subject + " allowedRadiusMeter must be between 10 and 10000"
            );
        }
    }

    private BigDecimal normalizeNullableLatitude(BigDecimal value) {
        if (value == null) {
            return null;
        }

        if (value.compareTo(BigDecimal.valueOf(-90)) < 0 || value.compareTo(BigDecimal.valueOf(90)) > 0) {
            throw ApiException.unprocessable(
                    "ATTENDANCE_POLICY_LOCATION_LAT_INVALID",
                    "locationLat must be between -90 and 90"
            );
        }

        return value.setScale(7, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeNullableLongitude(BigDecimal value) {
        if (value == null) {
            return null;
        }

        if (value.compareTo(BigDecimal.valueOf(-180)) < 0 || value.compareTo(BigDecimal.valueOf(180)) > 0) {
            throw ApiException.unprocessable(
                    "ATTENDANCE_POLICY_LOCATION_LNG_INVALID",
                    "locationLng must be between -180 and 180"
            );
        }

        return value.setScale(7, RoundingMode.HALF_UP);
    }

    private Integer normalizeAllowedRadiusMeter(Integer value) {
        return value == null ? 150 : value;
    }

    private BigDecimal canonicalizeResolvedWeight(
            BigDecimal value,
            AttendancePolicySource source,
            String fieldName
    ) {
        if (value == null) {
            throw new IllegalStateException("Resolved attendance policy " + source + " is missing " + fieldName);
        }
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalStateException("Resolved attendance policy " + source + " has invalid " + fieldName);
        }
        return value.setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal canonicalizeResolvedPercent(
            BigDecimal value,
            AttendancePolicySource source,
            String fieldName,
            boolean nullable
    ) {
        if (value == null) {
            if (nullable) {
                return null;
            }
            throw new IllegalStateException("Resolved attendance policy " + source + " is missing " + fieldName);
        }
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalStateException("Resolved attendance policy " + source + " has invalid " + fieldName);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private Integer canonicalizeResolvedAbsentCount(
            Integer value,
            AttendancePolicySource source,
            String fieldName,
            boolean nullable
    ) {
        if (value == null) {
            if (nullable) {
                return null;
            }
            throw new IllegalStateException("Resolved attendance policy " + source + " is missing " + fieldName);
        }
        if (value < 1) {
            throw new IllegalStateException("Resolved attendance policy " + source + " has invalid " + fieldName);
        }
        return value;
    }

    private BigDecimal canonicalizeResolvedLatitude(
            BigDecimal value,
            AttendancePolicySource source,
            String fieldName,
            boolean nullable
    ) {
        if (value == null) {
            if (nullable) {
                return null;
            }
            throw new IllegalStateException("Resolved attendance policy " + source + " is missing " + fieldName);
        }
        if (value.compareTo(BigDecimal.valueOf(-90)) < 0 || value.compareTo(BigDecimal.valueOf(90)) > 0) {
            throw new IllegalStateException("Resolved attendance policy " + source + " has invalid " + fieldName);
        }
        return value.setScale(7, RoundingMode.HALF_UP);
    }

    private BigDecimal canonicalizeResolvedLongitude(
            BigDecimal value,
            AttendancePolicySource source,
            String fieldName,
            boolean nullable
    ) {
        if (value == null) {
            if (nullable) {
                return null;
            }
            throw new IllegalStateException("Resolved attendance policy " + source + " is missing " + fieldName);
        }
        if (value.compareTo(BigDecimal.valueOf(-180)) < 0 || value.compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new IllegalStateException("Resolved attendance policy " + source + " has invalid " + fieldName);
        }
        return value.setScale(7, RoundingMode.HALF_UP);
    }

    private Integer canonicalizeResolvedAllowedRadius(
            Integer value,
            AttendancePolicySource source,
            String fieldName
    ) {
        Integer normalized = value == null ? 150 : value;
        if (normalized < 10 || normalized > 10000) {
            throw new IllegalStateException("Resolved attendance policy " + source + " has invalid " + fieldName);
        }
        return normalized;
    }

    private String normalizeKeyword(String q) {
        if (q == null || q.isBlank()) {
            return null;
        }
        return q.trim().toLowerCase(Locale.ROOT);
    }

    private SortSpec parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return new SortSpec("u.full_name asc, BIN_TO_UUID(gm.user_id, 1) asc");
        }

        String[] parts = sort.split(",", 2);
        String field = parts[0].trim();
        String dir = parts.length > 1 ? parts[1].trim().toLowerCase(Locale.ROOT) : "asc";
        String direction = "desc".equals(dir) ? "desc" : "asc";

        return switch (field) {
            case "email" -> new SortSpec("u.email " + direction + ", BIN_TO_UUID(gm.user_id, 1) asc");
            case "joinedAt" -> new SortSpec("coalesce(gm.joined_at, gm.created_at) " + direction + ", BIN_TO_UUID(gm.user_id, 1) asc");
            case "fullName" -> new SortSpec("u.full_name " + direction + ", BIN_TO_UUID(gm.user_id, 1) asc");
            default -> throw ApiException.badRequest("INVALID_SORT", "Supported sort fields: fullName,email,joinedAt");
        };
    }

    private record GroupState(boolean archived) {
    }

    private record MembershipAccessRow(boolean approved, boolean owner, boolean coHost) {
    }

    private record MembershipRow(Instant effectiveAt) {
    }

    private record MemberListRow(UUID userId, String fullName, String email, Instant effectiveAt) {
    }

    private record SortSpec(String orderBySql) {
    }

    private record AttendanceCounts(
            long closedSessionCount,
            long eligibleSessionCount,
            long presentCount,
            long lateCount,
            long absentCount,
            long excusedCount
    ) {
        static AttendanceCounts zero() {
            return new AttendanceCounts(0, 0, 0, 0, 0, 0);
        }
    }

    public record UpsertPolicyCommand(
            BigDecimal lateWeight,
            BigDecimal warningBelowRate,
            BigDecimal criticalBelowRate,
            Integer warningAbsentCount,
            Integer criticalAbsentCount,
            Boolean requireLocation,
            BigDecimal locationLat,
            BigDecimal locationLng,
            Integer allowedRadiusMeter
    ) {
    }

    public record EffectivePolicy(
            AttendancePolicySource source,
            BigDecimal lateWeight,
            BigDecimal warningBelowRate,
            BigDecimal criticalBelowRate,
            Integer warningAbsentCount,
            Integer criticalAbsentCount,
            boolean requireLocation,
            BigDecimal locationLat,
            BigDecimal locationLng,
            Integer allowedRadiusMeter,
            Instant createdAt,
            UUID createdByUserId,
            Instant updatedAt,
            UUID updatedByUserId
    ) {
    }

    public record PolicyView(
            UUID groupId,
            AttendancePolicySource source,
            BigDecimal lateWeight,
            BigDecimal warningBelowRate,
            BigDecimal criticalBelowRate,
            Integer warningAbsentCount,
            Integer criticalAbsentCount,
            boolean requireLocation,
            BigDecimal locationLat,
            BigDecimal locationLng,
            Integer allowedRadiusMeter,
            String excusedHandling,
            String sessionScope,
            String membershipScope,
            Instant createdAt,
            UUID createdByUserId,
            Instant updatedAt,
            UUID updatedByUserId
    ) {
    }

    public record StudentPolicyStatusView(
            UUID userId,
            String fullName,
            String email,
            Instant joinedAt,
            long closedSessionCount,
            long eligibleSessionCount,
            long presentCount,
            long lateCount,
            long absentCount,
            long excusedCount,
            BigDecimal earnedAttendancePoints,
            BigDecimal attendanceRate,
            AttendancePolicyStatus policyStatus,
            List<AttendancePolicyBreachReason> breachReasons
    ) {
    }

    public record StudentPolicyPageView(
            PolicyView policy,
            List<StudentPolicyStatusView> items,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
    }
}