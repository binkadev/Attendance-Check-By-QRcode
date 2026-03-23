package com.attendance.backend.security.dashboard;

import com.attendance.backend.auth.repository.UserRepository;
import com.attendance.backend.common.exception.ApiException;
import com.attendance.backend.domain.entity.User;
import com.attendance.backend.domain.enums.PlatformRole;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class SecurityDashboardService {

    private final UserRepository userRepository;
    private final EntityManager entityManager;
    private final Clock clock;

    public SecurityDashboardService(UserRepository userRepository,
                                    EntityManager entityManager,
                                    Clock clock) {
        this.userRepository = userRepository;
        this.entityManager = entityManager;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public OverviewResponse getOverview(UUID actorUserId, Integer hours) {
        ensureAdmin(actorUserId);

        int effectiveHours = sanitizeHours(hours);
        Instant since = Instant.now(clock).minus(effectiveHours, ChronoUnit.HOURS);

        return new OverviewResponse(
                effectiveHours,
                count("""
                    select count(*)
                    from password_reset_attempts
                    where created_at >= :since
                      and outcome = 'ISSUED'
                """, since),
                count("""
                    select count(*)
                    from password_reset_attempts
                    where created_at >= :since
                      and outcome in ('THROTTLED_EMAIL', 'THROTTLED_IP')
                """, since),
                count("""
                    select count(*)
                    from password_reset_attempts
                    where created_at >= :since
                      and outcome = 'MAIL_DELIVERY_FAILED'
                """, since),
                count("""
                    select count(*)
                    from login_attempts
                    where created_at >= :since
                      and outcome = 'SUCCESS'
                """, since),
                count("""
                    select count(*)
                    from login_attempts
                    where created_at >= :since
                      and outcome = 'INVALID_CREDENTIALS'
                """, since),
                count("""
                    select count(*)
                    from login_attempts
                    where created_at >= :since
                      and outcome in ('THROTTLED_IP', 'THROTTLED_EMAIL_IP')
                """, since),
                count("""
                    select count(*)
                    from email_outbox
                    where status = 'PENDING'
                """, since),
                count("""
                    select count(*)
                    from email_outbox
                    where status = 'RETRY'
                """, since),
                count("""
                    select count(*)
                    from email_outbox
                    where status = 'DEAD'
                """, since),
                count("""
                    select count(*)
                    from user_sessions
                    where revoked_at >= :since
                      and revoked_reason = 'PASSWORD_RESET'
                """, since)
        );
    }

    @Transactional(readOnly = true)
    public List<AbuseItem> getPasswordResetAbuse(UUID actorUserId, Integer hours, Integer limit) {
        ensureAdmin(actorUserId);

        Instant since = Instant.now(clock).minus(sanitizeHours(hours), ChronoUnit.HOURS);
        int effectiveLimit = sanitizeLimit(limit);

        List<Object[]> rows = entityManager.createNativeQuery("""
                select
                    coalesce(requested_ip, 'UNKNOWN') as source,
                    count(*) as total_count,
                    sum(case when outcome in ('THROTTLED_EMAIL', 'THROTTLED_IP') then 1 else 0 end) as throttled_count
                from password_reset_attempts
                where created_at >= :since
                group by requested_ip
                order by throttled_count desc, total_count desc
                limit :limit
                """)
                .setParameter("since", Timestamp.from(since))
                .setParameter("limit", effectiveLimit)
                .getResultList();

        return rows.stream()
                .map(r -> new AbuseItem(
                        (String) r[0],
                        ((Number) r[1]).longValue(),
                        ((Number) r[2]).longValue()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AbuseItem> getLoginAbuse(UUID actorUserId, Integer hours, Integer limit) {
        ensureAdmin(actorUserId);

        Instant since = Instant.now(clock).minus(sanitizeHours(hours), ChronoUnit.HOURS);
        int effectiveLimit = sanitizeLimit(limit);

        List<Object[]> rows = entityManager.createNativeQuery("""
                select
                    coalesce(requested_ip, 'UNKNOWN') as source,
                    count(*) as total_count,
                    sum(case when outcome in ('THROTTLED_IP', 'THROTTLED_EMAIL_IP') then 1 else 0 end) as throttled_count
                from login_attempts
                where created_at >= :since
                group by requested_ip
                order by throttled_count desc, total_count desc
                limit :limit
                """)
                .setParameter("since", Timestamp.from(since))
                .setParameter("limit", effectiveLimit)
                .getResultList();

        return rows.stream()
                .map(r -> new AbuseItem(
                        (String) r[0],
                        ((Number) r[1]).longValue(),
                        ((Number) r[2]).longValue()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DeadOutboxItem> getDeadOutbox(UUID actorUserId, Integer limit) {
        ensureAdmin(actorUserId);

        int effectiveLimit = sanitizeLimit(limit);

        List<Object[]> rows = entityManager.createNativeQuery("""
                select
                    BIN_TO_UUID(id, 1) as id,
                    to_email,
                    status,
                    retry_count,
                    last_error_code,
                    last_error_message,
                    next_attempt_at,
                    created_at
                from email_outbox
                where status in ('RETRY', 'DEAD')
                order by updated_at desc
                limit :limit
                """)
                .setParameter("limit", effectiveLimit)
                .getResultList();

        return rows.stream()
                .map(r -> new DeadOutboxItem(
                        UUID.fromString((String) r[0]),
                        (String) r[1],
                        (String) r[2],
                        ((Number) r[3]).intValue(),
                        (String) r[4],
                        (String) r[5],
                        ((Timestamp) r[6]).toInstant(),
                        ((Timestamp) r[7]).toInstant()
                ))
                .toList();
    }

    private long count(String sql, Instant since) {
        var query = entityManager.createNativeQuery(sql);

        if (since != null) {
            Timestamp sinceTimestamp = Timestamp.from(since);

            if (sql.contains(":since")) {
                query.setParameter("since", sinceTimestamp);
            } else if (sql.contains("?1") || sql.contains("?")) {
                query.setParameter(1, sinceTimestamp);
            }
        }

        Object result = query.getSingleResult();
        if (result == null) {
            return 0L;
        }
        if (result instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(result.toString());
    }

    private void ensureAdmin(UUID actorUserId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(actorUserId)
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "User not found"));

        if (user.getPlatformRole() != PlatformRole.ADMIN) {
            throw ApiException.forbidden("FORBIDDEN", "Admin only");
        }
    }

    private int sanitizeHours(Integer hours) {
        if (hours == null) {
            return 24;
        }
        if (hours < 1 || hours > 168) {
            throw ApiException.badRequest("HOURS_INVALID", "hours must be between 1 and 168");
        }
        return hours;
    }

    private int sanitizeLimit(Integer limit) {
        if (limit == null) {
            return 20;
        }
        if (limit < 1 || limit > 100) {
            throw ApiException.badRequest("LIMIT_INVALID", "limit must be between 1 and 100");
        }
        return limit;
    }

    public record OverviewResponse(
            int hours,
            long passwordResetIssued,
            long passwordResetThrottled,
            long passwordResetMailFailed,
            long loginSuccess,
            long loginInvalidCredentials,
            long loginThrottled,
            long emailOutboxPending,
            long emailOutboxRetry,
            long emailOutboxDead,
            long sessionsRevokedByPasswordReset
    ) {
    }

    public record AbuseItem(
            String source,
            long totalCount,
            long throttledCount
    ) {
    }

    public record DeadOutboxItem(
            UUID id,
            String toEmail,
            String status,
            int retryCount,
            String lastErrorCode,
            String lastErrorMessage,
            Instant nextAttemptAt,
            Instant createdAt
    ) {
    }
}